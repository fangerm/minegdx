package xyz.angm.terra3d.server.world

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import ktx.ashley.get
import ktx.collections.*
import xyz.angm.terra3d.client.world.RENDER_DIST_CHUNKS
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.WORLD_HEIGHT_IN_CHUNKS
import xyz.angm.terra3d.common.ecs.components.VectoredComponent
import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.networking.ChunksUpdate
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.Chunk
import xyz.angm.terra3d.server.Server
import xyz.angm.terra3d.server.ecs.systems.BlockEntitySystem
import xyz.angm.terra3d.server.world.generation.Structure
import xyz.angm.terra3d.server.world.generation.TerrainGenerator
import java.util.concurrent.TimeUnit

/** Server-side representation of a World containing all blocks.
 * @param server The server this world is running under.
 * @property seed The world seed used for generating the terrain. */
class World(private val server: Server) {

    val seed = server.save.seed
    private val database = WorldDatabase(server)
    private val generator = TerrainGenerator(this)
    private val chunksWithQueuedChanges = GdxArray<Chunk>(false, 8, Chunk::class.java)
    private val blockEntitySystem = BlockEntitySystem(this)
    private val tmpV = Vector3()
    private val tmpIV = IntVector3()
    private val tmpIV2 = IntVector3()

    init {
        server.executor.scheduleAtFixedRate(database::flushChunks, 60, 60, TimeUnit.SECONDS)
        database.generateChunks(IntVector3(1000, 0, 1000), generator)
        server.engine.addSystem(blockEntitySystem)
    }

    /** Updates pre-generated chunks around the player.
     * @param players A list of all players */
    fun updateLoadedChunksByPlayers(players: Iterable<Entity>) = players.forEach { database.generateChunks(tmpIV.set(it[position]!!), generator) }

    /** Adds the specified chunk to the world. */
    internal fun addChunk(newChunk: Chunk) = database.addChunk(newChunk)

    /** Returns an array of chunks.
     * @param position The chunk's position, y axis is ignored.
     * @return All chunks with matching x and z axis */
    fun getChunkLine(position: IntVector3): Array<Chunk> {
        tmpIV.set(position).norm(CHUNK_SIZE).y = 0
        val out = GdxArray<Chunk>(false, 16, Chunk::class.java)
        database.getChunkLine(tmpIV, out)
        generator.generateMissing(out, tmpIV)
        Structure.update(this)
        return out.toArray()
    }

    /** Returns a chunk in cache if it exists. Does not norm the position given!
     * Used by the [TerrainGenerator] when filling in missing chunks in a line
     * to catch chunks that are in cache but weren't spooled to disk yet. */
    internal fun getCachedChunk(position: IntVector3) = database.getCachedChunk(position)

    /** Returns all chunks in render distance for the given position.
     * Used for initial world sync with clients. */
    fun getInitData(position: VectoredComponent): Array<Chunk> {
        val across = (RENDER_DIST_CHUNKS * 2) + 1
        val out = GdxArray<Chunk>(false, across * across * WORLD_HEIGHT_IN_CHUNKS, Chunk::class.java)
        val dist = RENDER_DIST_CHUNKS * CHUNK_SIZE * 2
        tmpIV.set(position).norm(CHUNK_SIZE).minus(dist, 0, dist)

        for (x in tmpIV.x until (position.x + dist + CHUNK_SIZE).toInt() step CHUNK_SIZE)
            for (z in tmpIV.z until (position.z + dist + CHUNK_SIZE).toInt() step CHUNK_SIZE) {
                database.getChunkLine(tmpIV2.set(x, 0, z), out)
                generator.generateMissing(out, tmpIV2)
            }

        Structure.update(this)
        return out.toArray()
    }

    /** Returns a block at the specified position, or null if there is none. */
    fun getBlock(position: IntVector3): Block? {
        val chunk = database.getChunk(position)
        return chunk?.getBlock(tmpIV.set(position).minus(chunk.position))
    }

    /** @see getBlock */
    fun getBlock(position: Vector3) = getBlock(tmpIV.set(position))

    /** Sets the block. Will automatically sync to clients and dispatch any other work required.
     * @param position The position to place it at
     * @param block The block to place
     * @return If the block was successfully placed / could be placed */
    fun setBlock(position: IntVector3, block: Block): Boolean {
        val oldBlock = database.setBlock(position, block)

        if (block.type == 0) {
            if (oldBlock == null || oldBlock.type == 0) return false
            blockEntitySystem.removeBlockEntity(server.engine, oldBlock.position)
            BlockEvents.getListener(oldBlock, Event.BLOCK_DESTROYED)?.invoke(this, oldBlock)

            val item = Item(oldBlock)
            item.type = Item.Properties.fromIdentifier(item.properties.block!!.drop ?: item.properties.ident).type
            ItemComponent.create(server.engine, item, position.toV3(tmpV).add(0.5f, 0f, 0.5f))

        } else if (oldBlock?.type != block.type) { // A new block got placed; the block was just updated if this is false
            BlockEvents.getListener(block, Event.BLOCK_PLACED)?.invoke(this, block)
            val blockEntity = BlockEvents.getBlockEntity(block)
            if (blockEntity != null) blockEntitySystem.createBlockEntity(server.engine, blockEntity)
        }

        server.sendToAll(block)

        return true
    }

    /** Queues a block to be set. Way faster than setBlock, but assumes block was null before and does not consider block entities.
     * This is useful when a lot of simple blocks have to be set at once,
     * since it prevents the network packet spam that batch setting blocks would cause
     * since full chunks are sent anew on flush.
     * A call to [flushBlockQueue] will set all blocks queued and sync state.
     * Chunks with blocks changed by batching are not considered to be changed; they will not be spooled to DB.
     * The main use of all this is world generation *after* initial chunk generation - this is the case
     * when generating structures that cross chunk boundaries.
     * @return If the block was successfully queued. No means the world at that location is not yet generated. */
    fun queueBlock(position: IntVector3, type: ItemType): Boolean {
        val chunk = database.setBlock(position, type) ?: return false
        if (chunk !in chunksWithQueuedChanges) chunksWithQueuedChanges.add(chunk)
        return true
    }

    /** @see queueBlock; flushes all blocks queued and syncs network and database. */
    fun flushBlockQueue() {
        if (chunksWithQueuedChanges.isEmpty) return
        server.sendToAll(ChunksUpdate(chunksWithQueuedChanges.toArray()))
        chunksWithQueuedChanges.clear()
    }

    /** Called on server close; saves to disk */
    fun close() = database.flushChunks()
}