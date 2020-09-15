package xyz.angm.terra3d.client.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
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
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.networking.BlockUpdate
import xyz.angm.terra3d.common.networking.ChunkRequest
import xyz.angm.terra3d.common.networking.ChunksLine
import xyz.angm.terra3d.common.world.BfsLight
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.Chunk
import xyz.angm.terra3d.common.world.WorldInterface
import xyz.angm.terra3d.common.world.generation.TerrainGenerator

const val RENDER_DIST_CHUNKS = 3

/** How far a block raycast goes - The max distance a player can change blocks from. */
private const val RAYCAST_REACH = 5f

/** How far to step forward each raycast check iteration. Smaller values are more precise but take longer. */
private const val RAYCAST_STEP = 0.02f

/** The amount of time to spend rendering/meshing chunks per frame. */
private const val RENDER_TIME = 4

/** The amount of time to spend rendering/meshing chunks per frame during initialization, see [xyz.angm.terra3d.client.Terra3D]. */
const val RENDER_TIME_LOAD = 10

/** The maximum distance a chunk can have to the player before being discarded. */
private const val MAX_CHUNK_DIST = (RENDER_DIST_CHUNKS + 1) * CHUNK_SIZE

/** Client-side representation of the world, which contains all blocks.
 * @param client A connected network client. */
class World(private val client: Client, override val seed: String) : Disposable, WorldInterface {

    private val tmpIV1 = IntVector3()
    private val tmpIV2 = IntVector3()

    private val chunks = OrderedMap<IntVector3, RenderableChunk>()
    private val chunksWaitingForRender = Queue<RenderableChunk>(400)
    private val generator = TerrainGenerator(this)
    private val lighting = BfsLight(this)

    val chunksLoaded: Int get() = chunks.size
    val waitingForRender: Int get() = chunksWaitingForRender.size

    init {
        client.addListener { packet ->
            when (packet) {
                is BlockUpdate -> {
                    val chunk = getChunk(packet.position) ?: return@addListener
                    packet.position.minus(chunk.position)

                    val oldBlock = chunk.getBlock(packet.position)
                    chunk.setBlock(packet.position, packet)
                    packet.position.add(chunk.position) // Restore state, listeners should not modify
                    lighting.blockSet(packet, oldBlock)

                    // Do this immediately instead of queueing to prevent other
                    // chunks holding up the queue and the user noticing a delay
                    Gdx.app.postRunnable { chunk.mesh(this) }
                    queueNeighbors(chunk)
                }
                is ChunksLine -> addChunks(packet.chunks)
            }
        }
    }

    /** Requests any chunks that are near the specified location from the server if they're not already loaded,
     * and unloads far away chunks.
     * @param position The position to check. Should be the player's position. */
    fun updateLoadedChunks(position: IntVector3) {
        val chunkPos = IntVector3()
        for (x in -RENDER_DIST_CHUNKS..RENDER_DIST_CHUNKS) {
            for (z in -RENDER_DIST_CHUNKS..RENDER_DIST_CHUNKS) {
                val chunkPosition = chunkPos.set(position).add(x * CHUNK_SIZE, 0, z * CHUNK_SIZE)
                if (getChunk(chunkPosition) == null) loadChunkLine(chunkPosition)
            }
        }
        generator.finalizeGen()

        // Iterate chunks and remove all that are too far
        var i = 0
        while (i < chunks.size) {
            val pos = chunks.orderedKeys()[i]
            if (!pos.within(position, MAX_CHUNK_DIST)) {
                chunks[pos]!!.dispose()
                chunks.removeIndex(i)
            } else i++
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
    fun render(modelBatch: ModelBatch, cam: Camera, environment: Environment?) {
        chunks.values().forEach { if (it.shouldRender(cam)) it.render(modelBatch, environment) }
    }

    private val last = IntVector3()
    private val raycast = IntVector3()
    private val tmpV1 = Vector3()
    private val tmpV2 = Vector3()

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
    fun updateBlockRaycast(position: VectoredComponent, direction: VectoredComponent, newBlock: Item): Boolean {
        if (!newBlock.properties.isBlock) return false
        val blockPosition = getBlockRaycast(position, direction, true) ?: return false
        setBlock(blockPosition, newBlock, getOrientationFromRaycast())
        return true
    }

    private fun getOrientationFromRaycast(): Block.Orientation {
        val d = raycast.minus(last)
        return when {
            d.y == -1 -> Block.Orientation.UP
            d.y == 1 -> Block.Orientation.DOWN
            d.x == -1 -> Block.Orientation.NORTH
            d.x == 1 -> Block.Orientation.SOUTH
            d.z == -1 -> Block.Orientation.EAST
            d.z == 1 -> Block.Orientation.WEST
            else -> throw RuntimeException("Block orientation is invalid")
        }
    }

    /** Gets block.
     * @param position Position of the block in world coordinates
     * @return Block at specified location; can be null */
    fun getBlock(position: IntVector3): Block? {
        val chunk = getChunk(position)
        return chunk?.getBlock(tmpIV1.set(position).minus(chunk.position))
    }

    /** @return Local light at the given block.
     * THE VECTOR RETURNED IS REUSED FOR EVERY CALL. Copy it if you need it to persist. */
    override fun getLocalLight(position: IntVector3): IntVector3? {
        val chunk = getChunk(position)
        tmpIV2.set(position).minus(chunk?.position ?: return null)
        return chunk.getLocalLight(tmpIV2.x, tmpIV2.y, tmpIV2.z)
    }

    /** Sets local light at the given block. */
    override fun setLocalLight(position: IntVector3, light: IntVector3) {
        val chunk = getChunk(position)
        tmpIV2.set(position).minus(chunk?.position ?: return)
        return chunk.setLocalLight(tmpIV2.x, tmpIV2.y, tmpIV2.z, light)
    }

    /** @return If there's a block at the given position. */
    fun blockExists(position: IntVector3, default: Boolean = false): Boolean {
        val chunk = getChunk(position)
        return chunk?.blockExists(tmpIV1.set(position).minus(chunk.position)) ?: default
    }

    /** @return If there's a block at the given position AND the block is solid/not blended.
     * Used by RenderableChunk to determine if an adjacent block face is visible */
    fun isBlended(position: IntVector3): Boolean {
        val chunk = getChunk(position)
        return chunk?.isBlended(tmpIV1.set(position).minus(chunk.position)) ?: false
    }

    /** Queue a chunk and all adjacent chunks for rendering. */
    private fun queueForRender(chunk: RenderableChunk, queueNeighbors: Boolean = true) {
        if (chunk.isQueued) return
        chunk.isQueued = true
        chunksWaitingForRender.addFirst(chunk)
        if (queueNeighbors) queueNeighbors(chunk)
    }

    /** Queues all neighboring chunks for rerender. */
    private fun queueNeighbors(chunk: Chunk) {
        // TODO: don't do this
        queueRerender(getChunk(tmpIV2.set(chunk.position).minus(CHUNK_SIZE, 0, 0)))
        queueRerender(getChunk(tmpIV2.set(chunk.position).minus(0, CHUNK_SIZE, 0)))
        queueRerender(getChunk(tmpIV2.set(chunk.position).minus(0, 0, CHUNK_SIZE)))
        queueRerender(getChunk(tmpIV2.set(chunk.position).add(CHUNK_SIZE, 0, 0)))
        queueRerender(getChunk(tmpIV2.set(chunk.position).add(0, CHUNK_SIZE, 0)))
        queueRerender(getChunk(tmpIV2.set(chunk.position).add(0, 0, CHUNK_SIZE)))
    }

    private fun queueRerender(chunk: RenderableChunk?) {
        chunk ?: return
        if (!chunk.isQueued) {
            chunksWaitingForRender.addFirst(chunk)
            chunk.isQueued = true
        }
    }

    private fun setBlock(position: IntVector3, item: Item?, orientation: Block.Orientation) {
        val block = if (item != null) Block(item, position, orientation) else Block(0, position, orientation = orientation)
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

    private fun getChunk(position: IntVector3): RenderableChunk? = chunks[tmpIV1.set(position).chunk()]

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
        if (!chunks.containsKey(renderableChunk.position)) {
            chunks[renderableChunk.position] = renderableChunk
        }
    }

    /** Adds given chunks to the world and queues them for render. */
    fun addChunks(chunks: Array<Chunk>, queueNeighbors: Boolean = true) {
        chunks.forEach {
            val renderableChunk = RenderableChunk(serverChunk = it)
            queueForRender(renderableChunk, queueNeighbors)
            if (!this@World.chunks.containsKey(renderableChunk.position)) {
                this@World.chunks[renderableChunk.position] = renderableChunk
            }
        }
    }

    override fun dispose() {
        chunks.values().forEach { it.dispose() }
    }
}
