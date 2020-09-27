/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

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

    /** Returns block at the given position, if it is loaded. */
    fun getBlock(position: IntVector3): Block?

    /** @return Local light at the given block.
     * THE VECTOR RETURNED MIGHT BE REUSED FOR EVERY CALL. Copy it if you need it to persist. */
    fun getLocalLight(position: IntVector3): IntVector3?

    /** Sets local light at the given block. */
    fun setLocalLight(position: IntVector3, light: IntVector3)
}