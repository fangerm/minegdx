/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 10:02 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.world

import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.server.ecs.systems.PhysicsSystem

/** An interface implemented by both serverside and clientside worlds, to be used
 * to interface with the world generator.
 * @param seed The world seed */
abstract class IWorld(val seed: String) {

    private val tmpIVLocal = ThreadLocal.withInitial { IntVector3() }
    protected val tmpIV get() = tmpIVLocal.get()

    /** Will add the given chunk to the world. In the case of the server, the chunk
     * is considered unchanged and will not spool to disk. */
    abstract fun addChunk(chunk: Chunk)

    /** Returns the chunk at the given position.
     * Can be null on client if the chunk is not loaded locally.
     * NOTE: This function should set [tmpIV] to be chunk-local coordinates. */
    protected abstract fun getChunk(position: IntVector3): Chunk?

    /** Returns the chunk at the given position *if* it is already loaded and available.
     * For the client: It must be locally available.
     * For the server: It must be loaded in RAM, it will not check on-disk DB. */
    abstract fun getLoadedChunk(position: IntVector3): Chunk?

    /** Will set the given block at the given position, ignoring any other
     * previous blocks or other things. Used by Structure to generate itself.
     * Does not cause a sync between client and server.
     * Returns success (false if no chunk generated at given position yet) */
    abstract fun setBlockRaw(position: IntVector3, type: ItemType): Boolean

    /** Returns block at the given position, if it is loaded. */
    fun getBlock(position: IntVector3) = getChunk(position)?.getBlock(tmpIV)

    /** @return Local light at the given block.
     * THE VECTOR RETURNED IS REUSED FOR EVERY CALL. Copy it if you need it to persist. */
    fun getLocalLight(position: IntVector3) = getChunk(position)?.getLocalLight(tmpIV.x, tmpIV.y, tmpIV.z)

    /** Sets local light at the given block. */
    abstract fun setLocalLight(position: IntVector3, light: IntVector3)

    /** Returns the collider at the given position. */
    fun getCollider(position: IntVector3) = getChunk(position)?.getCollider(tmpIV) ?: PhysicsSystem.BlockCollider.NONE
}