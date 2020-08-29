package xyz.angm.terra3d.common.world

import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.ItemType

/** An interface implemented by both serverside and clientside worlds, to be used
 * to interface with the world generator. */
interface WorldInterface {

    /** The world seed */
    val seed: String

    /** Will add the given chunk to the world. In the case of the server, the chunk
     * is considered unchanged and will not spool to disk. */
    fun addChunk(chunk: Chunk)

    /** Returns the chunk at the given position *if* it is already loaded and available.
     * For the client: It must be locally available.
     * For the server: It must be loaded in RAM, it will not check on-disk DB. */
    fun getLoadedChunk(position: IntVector3): Chunk?

    /** Will set the given block at the given position, ignoring any other
     * previous blocks or other things. Used by Structure to generate itself.
     * Does not cause a sync between client and server.
     * Returns success (false if no chunk generated at given position yet) */
    fun setBlockRaw(position: IntVector3, type: ItemType): Boolean
}