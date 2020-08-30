package xyz.angm.terra3d.server.world

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.*
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.WORLD_BUFFER_DIST
import xyz.angm.terra3d.common.fst
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.Chunk
import xyz.angm.terra3d.common.world.generation.TerrainGenerator
import xyz.angm.terra3d.server.Server
import java.sql.Connection
import javax.sql.rowset.serial.SerialBlob

internal class WorldDatabase(private val server: Server) {

    private val db: Database
    private val tmpIV = IntVector3()
    private val tmpV2 = IntVector3()

    // These chunks have been created/modified/accessed since the last flushToDB call.
    private val newChunks = OrderedMap<IntVector3, Chunk>()
    private val changedChunks = OrderedMap<IntVector3, Chunk>()
    private val unchangedChunks = OrderedMap<IntVector3, Chunk>()

    init {
        db = dbs[server.save.location] ?: {
            dbs[server.save.location] = Database.connect("jdbc:sqlite:${server.save.location}/world.sqlite3", "org.sqlite.JDBC")
            dbs[server.save.location]!!
        }()

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(db) {
            SchemaUtils.create(Chunks)
            (exposedLogger as Logger).level = Level.ERROR
        }
    }

    /** Generates chunks in an area, if they're not already loaded.
     * @param position The position to check. Should be the player's position. */
    internal fun generateChunks(position: IntVector3, generator: TerrainGenerator) {
        transaction(db) {
            for (x in -WORLD_BUFFER_DIST..WORLD_BUFFER_DIST) {
                for (z in -WORLD_BUFFER_DIST..WORLD_BUFFER_DIST) {
                    tmpV2.set(position).norm(CHUNK_SIZE).add(x * CHUNK_SIZE, 0, z * CHUNK_SIZE).y = 0
                    if (newChunks[tmpV2] == null && Chunks.select { (Chunks.x eq tmpV2.x) and (Chunks.z eq tmpV2.z) }.toList().isEmpty()) {
                        generator.generateChunks(tmpV2)
                    }
                }
            }
        }
    }

    /** Adds the specified chunk to the world. */
    internal fun addChunk(newChunk: Chunk) {
        newChunks[newChunk.position] = newChunk
    }

    /** Returns chunk or null if it is not in the DB */
    internal fun getChunk(position: IntVector3): Chunk? {
        tmpIV.set(position).norm(CHUNK_SIZE)
        val cacheChunk = getCachedChunk(tmpIV)
        if (cacheChunk != null) return cacheChunk
        val dbChunk = getDBChunk(position) ?: return null
        val chunk = fst.asObject(dbChunk[Chunks.data].binaryStream.readBytes()) as Chunk
        unchangedChunks[chunk.position] = chunk
        return chunk
    }

    /** Gets all chunks with matching x and z axes, adding them to out.
     * @param position The chunks position, y axis is ignored. */
    internal fun getChunkLine(position: IntVector3, out: GdxArray<Chunk>) {
        val dbChunks = transaction(db) { Chunks.select { (Chunks.x eq position.x) and (Chunks.z eq position.z) }.toList() }
        for (chunk in dbChunks) {
            // Already got this one in cache if true
            if (out.any { it.position.x == chunk[Chunks.x] && it.position.y == chunk[Chunks.y] && it.position.z == chunk[Chunks.z] }) continue
            val ch = fst.asObject(chunk[Chunks.data].binaryStream.readBytes()) as Chunk
            out.add(ch)
            unchangedChunks[ch.position] = ch
        }
    }

    /** Get a chunk from one of the caches, if they have it. */
    internal fun getCachedChunk(pos: IntVector3): Chunk? = newChunks[pos] ?: changedChunks[pos] ?: unchangedChunks[pos]

    /** Sets the block. Does not do other needed things like firing events or updating block entities.
     * @param position The position to place it at
     * @param block The block to place
     * @return The old block */
    internal fun setBlock(position: IntVector3, block: Block): Block? {
        val chunk = getChunk(position) ?: return null
        tmpIV.set(position).minus(chunk.position)
        val oldBlock = chunk.getBlock(tmpIV)

        chunk.setBlock(tmpIV, block)
        changedChunks[chunk.position] = chunk
        return oldBlock
    }

    /** Same as above, but takes type instead of block. Also returns chunk instead of old block.
     * Better performance than the method above; mainly used for batching block operations ([World.setBlockRaw]).
     * Does not consider a chunk that the block was placed in to be changed. */
    internal fun setBlockRaw(position: IntVector3, type: ItemType): Boolean {
        val chunk = getChunk(position) ?: return false
        tmpIV.set(position).minus(chunk.position)

        chunk.setBlock(tmpIV, type)
        unchangedChunks[chunk.position] = chunk

        return true
    }

    /** Saves all chunks to DB. Called at regular intervals; as well as on shutdown. */
    internal fun flushChunks() {
        transaction(db) {
            newChunks.filter { changedChunks.keys().contains(it.key) }.forEach { chunk ->
                Chunks.insert {
                    it[x] = chunk.key.x
                    it[y] = chunk.key.y
                    it[z] = chunk.key.z
                    it[data] = SerialBlob(fst.asByteArray(chunk.value))
                }
            }
            changedChunks.filter { !newChunks.keys().contains(it.key) }.forEach { chunk ->
                Chunks.update({ (Chunks.x eq chunk.key.x) and (Chunks.y eq chunk.key.y) and (Chunks.z eq chunk.key.z) }) {
                    it[data] = SerialBlob(fst.asByteArray(chunk.value))
                }
            }
        }
        unchangedChunks.clear()
        newChunks.clear()
        changedChunks.clear()
    }

    private fun getDBChunk(position: IntVector3): ResultRow? {
        tmpIV.set(position).norm(CHUNK_SIZE)
        return transaction(db) { Chunks.select { (Chunks.x eq tmpIV.x) and (Chunks.y eq tmpIV.y) and (Chunks.z eq tmpIV.z) }.firstOrNull() }
    }

    private companion object {
        // https://github.com/JetBrains/Exposed/wiki/Transactions#working-with-a-multiple-databases
        // Connecting to the same DB more than once causes leaks
        private val dbs = ObjectMap<String, Database>()
    }
}

/** Chunk DB table. All axes are primary keys for SELECT performance */
private object Chunks : IntIdTable() {
    /** Position X axis */
    val x = integer("x").primaryKey()

    /** Position Y axis */
    val y = integer("y").primaryKey()

    /** Position Z axis */
    val z = integer("z").primaryKey()

    /** The chunk object, serialized with FST */
    val data = blob("data")
}
