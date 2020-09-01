package xyz.angm.terra3d.client.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import ktx.collections.*
import xyz.angm.terra3d.client.networking.Client
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.components.VectoredComponent
import xyz.angm.terra3d.common.ecs.components.set
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.networking.BlockUpdate
import xyz.angm.terra3d.common.networking.ChunkRequest
import xyz.angm.terra3d.common.networking.ChunksLine
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.Chunk
import xyz.angm.terra3d.common.world.WorldInterface
import xyz.angm.terra3d.common.world.generation.TerrainGenerator

const val RENDER_DIST_CHUNKS = 2

/** How far a block raycast goes - The max distance a player can change blocks from. */
private const val RAYCAST_REACH = 5f

/** How far to step forward each raycast check iteration. Smaller values are more precise but take longer. */
private const val RAYCAST_STEP = 0.02f

/** The amount of time to spend rendering/meshing chunks per frame. */
private const val RENDER_TIME = 4

/** The amount of time to spend rendering/meshing chunks per frame during initialization, see [xyz.angm.terra3d.client.Terra3D]. */
const val RENDER_TIME_LOAD = 10

/** The maximum distance a chunk can have to the player before being discarded. */
private const val MAX_CHUNK_DIST = 150f

/** Client-side representation of the world, which contains all blocks.
 * @param client A connected network client. */
class World(private val client: Client, override val seed: String) : Disposable, WorldInterface {

    private val tmpIV1 = IntVector3()
    private val tmpIV2 = IntVector3()
    private val tmpIV3 = IntVector3()
    private val tmpV1 = Vector3()
    private val tmpV2 = Vector3()

    private val chunks = OrderedMap<IntVector3, RenderableChunk>()
    private val chunksWaitingForRender = Queue<RenderableChunk>(400)
    private val generator = TerrainGenerator(this)

    val chunksLoaded: Int get() = chunks.size
    val waitingForRender: Int get() = chunksWaitingForRender.size

    init {
        client.addListener { packet ->
            when (packet) {
                is BlockUpdate -> {
                    val chunk = getChunk(packet.position) ?: return@addListener
                    chunk.setBlock(packet.position.minus(chunk.position), packet)
                    queueForRender(chunk)
                }
                is ChunksLine -> addChunks(packet.chunks)
            }
        }
    }

    /** Requests any chunks that are near the specified location from the server if they're not already loaded,
     * and unloads far away chunks.
     * @param position The position to check. Should be the player's position. */
    fun updateLoadedChunks(position: IntVector3) {
        for (x in 0..RENDER_DIST_CHUNKS) {
            for (z in 0..RENDER_DIST_CHUNKS) {
                val chunkPosition = tmpIV2.set(position).add(x * CHUNK_SIZE, 0, z * CHUNK_SIZE)
                if (getChunk(chunkPosition) == null) loadChunkLine(chunkPosition)
                chunkPosition.set(position).add(x * -CHUNK_SIZE, 0, z * CHUNK_SIZE)
                if (getChunk(chunkPosition) == null) loadChunkLine(chunkPosition)
                chunkPosition.set(position).add(x * CHUNK_SIZE, 0, z * -CHUNK_SIZE)
                if (getChunk(chunkPosition) == null) loadChunkLine(chunkPosition)
                chunkPosition.set(position).add(x * -CHUNK_SIZE, 0, z * -CHUNK_SIZE)
                if (getChunk(chunkPosition) == null) loadChunkLine(chunkPosition)
            }
        }
        generator.finalizeGen()

        // Post a runnable since disposing gl data needs to happen on the main thread
        Gdx.app.postRunnable {
            // Iterate chunks and remove all that are too far
            var i = 0
            while (i < chunks.size) {
                val pos = chunks.orderedKeys()[i]
                if (pos.distXZ(position) > MAX_CHUNK_DIST) {
                    chunks[pos]!!.dispose()
                    chunks.removeIndex(i)
                } else i++
            }
        }
    }

    /** Continues loading any chunks still waiting for render. Should be called once per frame.
     * @param renderTime The time to spend rendering per call */
    fun update(renderTime: Int = RENDER_TIME) {
        val startTime = System.currentTimeMillis()
        // Mesh until there's nothing left or we run out of time
        while (!chunksWaitingForRender.isEmpty && (System.currentTimeMillis() - startTime) < renderTime) {
            val next = chunksWaitingForRender.removeLast()
            next.mesh(this)

            if (next != chunks[next.position]) {
                chunks[next.position]?.dispose()
                chunks[next.position] = next
            }
        }
    }

    /** Renders itself.
     * @param modelBatch A ModelBatch. begin() should already be called.
     * @param cam The camera used for frustum culling.
     * @param environment The environment to render with. */
    fun render(modelBatch: ModelBatch, cam: PerspectiveCamera, environment: Environment) {
        chunks.values().forEach { if (it.shouldRender(cam)) it.render(modelBatch, environment) }
    }

    private val last = IntVector3()
    private val raycast = IntVector3()

    /** Gets the position of the block being looked at.
     * @param position Position of the one looking
     * @param direction Direction of the one looking
     * @param prev true returns block before the one being looked at (used for placing blocks, etc.)
     * @return Position of the block being looked at, or null if there is none */
    fun getBlockRaycast(position: VectoredComponent, direction: VectoredComponent, prev: Boolean): IntVector3? {
        for (i in 1 until (RAYCAST_REACH / RAYCAST_STEP).toInt()) {
            val dist = i * RAYCAST_STEP
            tmpV1.set(direction).nor().scl(dist)
            raycast.set(tmpV2.set(position).add(tmpV1))

            if (blockExists(raycast)) return if (prev) last else raycast
            last.set(raycast)
        }
        return null
    }

    /** Places a new block at the side of a block being looked at.
     * If the block is null, it will instead remove the block looked at.
     * @param position Position of the one looking
     * @param direction Direction of the one looking
     * @param newBlock Block to be placed. Null will destroy the block instead
     * @return If there was a block to be placed/removed and the operation was successful */
    fun updateBlockRaycast(position: VectoredComponent, direction: VectoredComponent, newBlock: Item?): Boolean {
        val blockPosition = getBlockRaycast(position, direction, newBlock != null) ?: return false
        setBlock(blockPosition, newBlock)
        return true
    }

    /** Gets block.
     * @param position Position of the block in world coordinates
     * @return Block at specified location; can be null */
    fun getBlock(position: IntVector3): Block? {
        val chunk = getChunk(position)
        return chunk?.getBlock(tmpIV1.set(position).minus(chunk.position))
    }

    /** @return If there's a block at the given position. */
    fun blockExists(position: IntVector3, default: Boolean = false): Boolean {
        val chunk = getChunk(position)
        return chunk?.blockExists(tmpIV3.set(position).minus(chunk.position)) ?: default
    }

    private fun queueForRender(chunk: RenderableChunk) {
        if (chunk.isQueued) return
        chunk.isQueued = true
        chunksWaitingForRender.addFirst(chunk)

        // TODO: don't do this
        queueRerender(getChunk(tmpIV3.set(chunk.position).minus(CHUNK_SIZE, 0, 0)))
        queueRerender(getChunk(tmpIV3.set(chunk.position).minus(0, CHUNK_SIZE, 0)))
        queueRerender(getChunk(tmpIV3.set(chunk.position).minus(0, 0, CHUNK_SIZE)))
        queueRerender(getChunk(tmpIV3.set(chunk.position).add(CHUNK_SIZE, 0, 0)))
        queueRerender(getChunk(tmpIV3.set(chunk.position).add(0, CHUNK_SIZE, 0)))
        queueRerender(getChunk(tmpIV3.set(chunk.position).add(0, 0, CHUNK_SIZE)))
    }

    private fun queueRerender(chunk: RenderableChunk?) {
        chunk ?: return
        if (!chunk.isQueued) {
            chunksWaitingForRender.addFirst(chunk)
            chunk.isQueued = true
        }
    }

    private fun setBlock(position: IntVector3, item: Item?) {
        val block = if (item != null) Block(item, position) else Block(0, position)
        setBlock(block)
    }

    /** Sets the given position to the block. Note that block type of 0 removes the block.
     * This works by having the server echo the block change to all clients, making
     * the change occur when this client also receives the echo and applies it. */
    fun setBlock(block: Block) = client.send(block)

    override fun setBlockRaw(position: IntVector3, type: ItemType): Boolean {
        val chunk = getChunk(position) ?: return false
        chunk.setBlock(tmpIV1.set(position).minus(chunk.position), type)
        queueForRender(chunk)
        return true
    }

    private fun getChunk(position: IntVector3): RenderableChunk? = chunks[tmpIV1.set(position).norm(CHUNK_SIZE)]

    override fun getLoadedChunk(position: IntVector3): Chunk? = getChunk(position)

    /** Will load all chunks in the given XZ coordinates.
     * Generates them first using [generator], and also requests them from the server
     * (which will only return chunks that were changed by players). */
    private fun loadChunkLine(position: IntVector3) {
        client.send(ChunkRequest(position))
        generator.generateChunks(position)
    }

    override fun addChunk(chunk: Chunk) {
        val renderableChunk = RenderableChunk(serverChunk = chunk)
        queueForRender(renderableChunk)
        if (!chunks.containsKey(renderableChunk.position)) chunks[renderableChunk.position] = renderableChunk
    }

    /** Adds given chunks to the world and queues them for render. */
    fun addChunks(chunks: Array<Chunk>) {
        chunks.forEach { addChunk(it) }
    }

    override fun dispose() {
        chunks.values().forEach { it.dispose() }
    }
}
