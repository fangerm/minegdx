package xyz.angm.terra3d.client.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import ktx.collections.*
import xyz.angm.terra3d.client.networking.Client
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.components.VectoredComponent
import xyz.angm.terra3d.common.ecs.components.set
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.networking.BlockUpdate
import xyz.angm.terra3d.common.networking.ChunkRequest
import xyz.angm.terra3d.common.networking.ChunksUpdate
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.Chunk

private const val RENDER_DIST_CHUNKS = 3
private const val RAYCAST_REACH = 5f
private const val RAYCAST_STEP = 0.02f

/** Client-side representation of the world, which contains all blocks.
 * @param client A connected network client. */
class World(private val client: Client) : Disposable {

    private val tmpIV1 = IntVector3()
    private val tmpIV2 = IntVector3()
    private val tmpIV3 = IntVector3()
    private val tmpV1 = Vector3()
    private val tmpV2 = Vector3()

    private val chunks = ObjectMap<IntVector3, RenderableChunk>()
    private val chunksWaitingForRender = com.badlogic.gdx.utils.Array<RenderableChunk>()

    init {
        client.addListener { packet ->
            when (packet) {
                is BlockUpdate -> {
                    val chunk = getChunk(packet.position) ?: return@addListener
                    chunk.setBlock(packet.position.minus(chunk.position), packet)
                    queueForRender(chunk)
                }
                is ChunksUpdate -> Gdx.app.postRunnable { addChunks(packet.chunks) }
            }
        }
    }

    /** Requests any chunks that are near the specified location from the server if they're not already loaded.
     * @param position The position to check. Should be the player's position. */
    fun updateLoadedChunks(position: IntVector3) {
        for (x in 0..RENDER_DIST_CHUNKS) {
            for (z in 0..RENDER_DIST_CHUNKS) {
                val chunkPosition = tmpIV2.set(position).add(x * CHUNK_SIZE, 0, z * CHUNK_SIZE)
                if (getChunk(chunkPosition) == null) requestChunk(chunkPosition)
                chunkPosition.set(position).add(x * -CHUNK_SIZE, 0, z * CHUNK_SIZE)
                if (getChunk(chunkPosition) == null) requestChunk(chunkPosition)
                chunkPosition.set(position).add(x * CHUNK_SIZE, 0, z * -CHUNK_SIZE)
                if (getChunk(chunkPosition) == null) requestChunk(chunkPosition)
                chunkPosition.set(position).add(x * -CHUNK_SIZE, 0, z * -CHUNK_SIZE)
                if (getChunk(chunkPosition) == null) requestChunk(chunkPosition)
            }
        }
    }

    /** Continues loading any chunks still waiting for render. Should be called once per frame. */
    fun update() {
        if (!chunksWaitingForRender.isEmpty) {
            val next = chunksWaitingForRender.pop()
            next.mesh()

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
        chunks.values().forEach { if (it.isVisible(cam)) it.render(modelBatch, environment) }
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

    /** @return If there's a block at the given position */
    fun blockExists(position: IntVector3): Boolean {
        val chunk = getChunk(position)
        return chunk?.blockExists(tmpIV3.set(position).minus(chunk.position)) ?: false
    }

    private fun queueForRender(chunk: RenderableChunk) = chunksWaitingForRender.add(chunk)

    private fun setBlock(position: IntVector3, item: Item?) {
        val block = if (item != null) Block(item, position) else Block(0, position)
        setBlock(block)
    }

    /** Sets the given position to the block. Note that block type of 0 removes the block.
     * This works by having the server echo the block change to all clients, making
     * the change occur when this client also receives the echo and applies it. */
    fun setBlock(block: Block) = client.send(block)

    private fun getChunk(position: IntVector3) = chunks[tmpIV1.set(position).norm(CHUNK_SIZE)]

    private fun requestChunk(position: IntVector3) = client.send(ChunkRequest(position))

    private fun addChunk(chunk: Chunk) {
        val renderableChunk = RenderableChunk(serverChunk = chunk)
        if (requiresRender(chunk)) queueForRender(renderableChunk)
        else chunks[chunk.position] = renderableChunk
    }

    private fun addChunks(chunks: Array<Chunk>) {
        chunks.forEach { addChunk(it) }
    }

    private fun requiresRender(chunk: Chunk) = chunk.position.y in 40..90

    /** @see com.badlogic.gdx.utils.Disposable */
    override fun dispose() {
        chunks.values().forEach { it.dispose() }
    }
}
