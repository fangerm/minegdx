/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 5:56 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server.world

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.badlogic.gdx.utils.ObjectMap
import ktx.collections.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.TransactionManager
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
import java.util.concurrent.ConcurrentHashMap

internal class WorldDatabase(private val server: Server) {

    private val db: Database
    private val tmpIVLocal = ThreadLocal.withInitial { IntVector3() }
    private val tmpIV get() = tmpIVLocal.get()

    // These chunks have been created/modified/accessed since the last flushToDB call.
    private val newChunks = ConcurrentHashMap<IntVector3, Chunk>()
    private val changedChunks = ConcurrentHashMap<IntVector3, Chunk>()
    private val unchangedChunks = ConcurrentHashMap<IntVector3, Chunk>()

    init {
        db = dbs[server.save.location] ?: {
            dbs[server.save.location] = Database.connect("jdbc:sqlite:${server.save.location}/world.sqlite3", "org.sqlite.JDBC")
            dbs[server.save.location]!!
        }()

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction {
            SchemaUtils.create(Chunks)
            (exposedLogger as Logger).level = Level.ERROR
        }
    }

    /** Generates chunks in an area, if they're not already loaded.
     * @param position The position to check. Should be the player's position. */
    internal fun generateChunks(position: IntVector3, generator: TerrainGenerator) {
        transaction {
            for (x in -WORLD_BUFFER_DIST..WORLD_BUFFER_DIST) {
                for (z in -WORLD_BUFFER_DIST..WORLD_BUFFER_DIST) {
                    tmpIV.set(position).chunk().add(x * CHUNK_SIZE, 0, z * CHUNK_SIZE).y = 0
                    if (newChunks[tmpIV] == null && Chunks.select { (Chunks.x eq tmpIV.x) and (Chunks.z eq tmpIV.z) }.toList().isEmpty()) {
                        generator.generateChunks(tmpIV)
                    }
                }
            }
        }
    }

    /** Adds the specified chunk to the world. */
    internal fun addChunk(newChunk: Chunk) {
        newChunks[newChunk.position] = newChunk
    }

    /** Returns chunk or null if it is not in the DB
     * @param generate If the chunk should be generated if missing. Does not return null if true. */
    internal fun getChunk(position: IntVector3, generate: Boolean = true): Chunk? {
        if (position.y < 0) return null
        val pos = tmpIV.set(position).chunk()
        val cacheChunk = getCachedChunk(pos)
        if (cacheChunk != null) return cacheChunk

        val dbChunk = getDBChunk(pos)
        val chunk = if (dbChunk != null) fst.asObject(dbChunk[Chunks.data].bytes) as Chunk
        else if (!generate) return null
        else {
            server.world.generator.generateChunks(pos)
            server.world.generator.finalizeGen()
            // Recurse as direct getCachedChunk causes a NPE in about 1/1000 times...
            // TODO: Mayyybe fix this? probably not worth the effort
            getChunk(position, true)!!
        }

        unchangedChunks[chunk.position] = chunk
        return chunk
    }

    /** Gets all chunks with matching x and z axes, adding them to out.
     * @param position The chunks position, y axis is ignored. */
    internal fun getChunkLine(position: IntVector3, out: GdxArray<Chunk>) {
        val dbChunks = transaction { Chunks.select { (Chunks.x eq position.x) and (Chunks.z eq position.z) }.toList() }
        for (chunk in dbChunks) {
            // Already got this one in cache if true
            if (out.any { it.position.x == chunk[Chunks.x] && it.position.y == chunk[Chunks.y] && it.position.z == chunk[Chunks.z] }) continue
            val ch = fst.asObject(chunk[Chunks.data].bytes) as Chunk
            out.add(ch)
            unchangedChunks[ch.position] = ch
        }
    }

    /** Get a chunk from one of the caches, if they have it. */
    internal fun getCachedChunk(pos: IntVector3): Chunk? = changedChunks[pos] ?: unchangedChunks[pos] ?: newChunks[pos]

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
        val chunk = getChunk(position, false) ?: return false
        tmpIV.set(position).minus(chunk.position)
        chunk.setBlock(tmpIV, type)

        unchangedChunks[chunk.position] = chunk
        return true
    }

    /** Sets and marks the chunk the given block is in as changed. */
    internal fun markBlockChanged(block: Block) {
        val chunk = getChunk(block.position) ?: return
        tmpIV.set(block.position).minus(chunk.position)
        chunk.setBlock(tmpIV, block)
        changedChunks[chunk.position] = chunk
    }

    /** Marks the given chunk as changed. */
    internal fun markChunkChanged(chunk: Chunk) {
        changedChunks[chunk.position] = chunk
    }

    /** Saves all chunks to DB. Called at regular intervals; as well as on shutdown. */
    internal fun flushChunks() {
        transaction {
            newChunks.filter { changedChunks.containsKey(it.key) }.forEach { chunk ->
                Chunks.insert {
                    it[x] = chunk.key.x
                    it[y] = chunk.key.y
                    it[z] = chunk.key.z
                    it[data] = ExposedBlob(fst.asByteArray(chunk.value))
                }
            }
            changedChunks.filter { !newChunks.containsKey(it.key) }.forEach { chunk ->
                Chunks.update({ (Chunks.x eq chunk.key.x) and (Chunks.y eq chunk.key.y) and (Chunks.z eq chunk.key.z) }) {
                    it[data] = ExposedBlob(fst.asByteArray(chunk.value))
                }
            }
        }
        unchangedChunks.clear()
        newChunks.clear()
        changedChunks.clear()
    }

    private fun getDBChunk(p: IntVector3) =
        transaction { Chunks.select { (Chunks.x eq p.x) and (Chunks.y eq p.y) and (Chunks.z eq p.z) }.firstOrNull() }

    private fun <T> transaction(statement: Transaction.() -> T) =
        org.jetbrains.exposed.sql.transactions.transaction(db, statement)

    private companion object {
        // https://github.com/JetBrains/Exposed/wiki/Transactions#working-with-a-multiple-databases
        // Connecting to the same DB more than once causes leaks
        private val dbs = ObjectMap<String, Database>()
    }
}

/** Chunk DB table. */
private object Chunks : Table() {
    val id = integer("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)

    /** Position X axis */
    val x = integer("x").index()
    /** Position Y axis */
    val y = integer("y").index()
    /** Position Z axis */
    val z = integer("z").index()

    /** The chunk object, serialized with FST */
    val data = blob("data")
}
