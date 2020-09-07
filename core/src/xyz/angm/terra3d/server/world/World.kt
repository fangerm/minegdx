package xyz.angm.terra3d.server.world

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import ktx.ashley.get
import ktx.collections.*
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.WORLD_HEIGHT_IN_CHUNKS
import xyz.angm.terra3d.common.ecs.components.VectoredComponent
import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.world.BfsLight
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.Chunk
import xyz.angm.terra3d.common.world.WorldInterface
import xyz.angm.terra3d.common.world.generation.TerrainGenerator
import xyz.angm.terra3d.server.Server
import xyz.angm.terra3d.server.ecs.systems.BlockEntitySystem
import xyz.angm.terra3d.server.ecs.systems.PhysicsSystem
import java.util.concurrent.TimeUnit

/** The render distance to use when sending a newly connecting client init data. */
const val INIT_DIST_CHUNKS = 2

/** Server-side representation of a World containing all blocks.
 * @param server The server this world is running under.
 * @property seed The world seed used for generating the terrain. */
class World(private val server: Server) : WorldInterface {

    private val tmpIVLocal = ThreadLocal.withInitial { IntVector3() }
    private val tmpIV get() = tmpIVLocal.get()

    override val seed = server.save.seed
    private val database = WorldDatabase(server)
    internal val generator = TerrainGenerator(this)
    private val blockEntitySystem = BlockEntitySystem(this)
    private val physics = PhysicsSystem(this::getBlock)
    private val lighting = BfsLight(this)

    init {
        server.executor.scheduleAtFixedRate(database::flushChunks, 60, 60, TimeUnit.SECONDS)
        database.generateChunks(IntVector3(1000, 0, 1000), generator)
        server.engine.addSystem(blockEntitySystem)
        server.engine.addSystem(physics)
    }

    /** Updates pre-generated chunks around the player.
     * @param players A list of all players */
    fun updateLoadedChunksByPlayers(players: Iterable<Entity>) = players.forEach { database.generateChunks(tmpIV.set(it[position]!!), generator) }

    /** Adds the specified chunk to the world; does not save it to disk. */
    override fun addChunk(chunk: Chunk) = database.addChunk(chunk)

    /** Returns an array of chunks, returning only those that were changed since world generation.
     * @param position The chunk's position, y axis is ignored.
     * @return All chunks with matching x and z axis that had player modification */
    fun getChunkLine(position: IntVector3): Array<Chunk> {
        tmpIV.set(position).norm(CHUNK_SIZE).y = 0
        val out = GdxArray<Chunk>(false, 6, Chunk::class.java)
        for (chunk in 0..WORLD_HEIGHT_IN_CHUNKS) {
            val loaded = getLoadedChunk(tmpIV.add(0, CHUNK_SIZE, 0))
            if (loaded != null) out.add(loaded)
        }
        database.getChunkLine(tmpIV, out)
        return out.toArray()
    }

    /** Returns a chunk in cache if it exists. Does not norm the position given!
     * Used by the [TerrainGenerator] when filling in missing chunks in a line
     * to catch chunks that are in cache but weren't spooled to disk yet. */
    override fun getLoadedChunk(position: IntVector3) = database.getCachedChunk(position)

    /** Returns all chunks in render distance for the given position.
     * Used for initial world sync with clients. */
    fun getInitData(position: VectoredComponent): Array<Chunk> {
        val across = (INIT_DIST_CHUNKS * 2) + 1
        val out = GdxArray<Chunk>(false, across * across * WORLD_HEIGHT_IN_CHUNKS, Chunk::class.java)
        val dist = INIT_DIST_CHUNKS * CHUNK_SIZE * 2
        tmpIV.set(position).norm(CHUNK_SIZE).minus(dist, 0, dist)
        val tmpI = IntVector3()

        for (x in tmpIV.x until (position.x + dist + CHUNK_SIZE).toInt() step CHUNK_SIZE)
            for (z in tmpIV.z until (position.z + dist + CHUNK_SIZE).toInt() step CHUNK_SIZE) {
                database.getChunkLine(tmpI.set(x, 0, z), out)
                generator.generateMissing(out, tmpI)
            }

        generator.finalizeGen()
        return out.toArray()
    }

    /** Returns a block at the specified position, or null if there is none. */
    fun getBlock(position: IntVector3): Block? {
        val chunk = database.getChunk(position)
        return chunk?.getBlock(tmpIV.set(position).minus(chunk.position))
    }

    private fun getBlock(position: Vector3) = getBlock(tmpIV.set(position))

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
            ItemComponent.create(server.engine, item, position.toV3().add(0.5f, 0f, 0.5f))

        } else if (oldBlock?.type != block.type) { // A new block got placed; the block was just updated if this is false
            BlockEvents.getListener(block, Event.BLOCK_PLACED)?.invoke(this, block)
            val blockEntity = BlockEvents.getBlockEntity(block)
            if (blockEntity != null) blockEntitySystem.createBlockEntity(server.engine, blockEntity)
        }

        lighting.blockSet(block, oldBlock)
        server.sendToAll(block)

        return true
    }

    /** Call when a block's metadata changed. Will mark
     * the chunk changed to ensure saving to disk and sync clients. */
    fun metadataChanged(block: Block) {
        database.markBlockChanged(block)
        server.sendToAll(block)
    }

    override fun setBlockRaw(position: IntVector3, type: ItemType) = database.setBlockRaw(position, type)

    /** @return Local light at the given block.
     * THE VECTOR RETURNED IS REUSED FOR EVERY CALL. Copy it if you need it to persist. */
    override fun getLocalLight(position: IntVector3): IntVector3? {
        val chunk = database.getChunk(position)
        tmpIV.set(position).minus(chunk?.position ?: return null)
        return chunk.getLocalLight(tmpIV.x, tmpIV.y, tmpIV.z)
    }

    /** Sets local light at the given block. */
    override fun setLocalLight(position: IntVector3, light: IntVector3) {
        val chunk = database.getChunk(position)
        tmpIV.set(position).minus(chunk?.position ?: return)
        database.markChunkChanged(chunk)
        return chunk.setLocalLight(tmpIV.x, tmpIV.y, tmpIV.z, light)
    }

    /** Called on server close; saves to disk */
    fun close() = database.flushChunks()
}