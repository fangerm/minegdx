/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/16/20, 5:59 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata

/** An interface for all metadata that contains an energy buffer
 * and can receive energy. */
interface EnergyStorageMeta : IMetadata {

    /** Put [amount] of energy into the buffer. */
    fun receive(amount: Int)

    /** Returns if this buffer is full and cannot accept more energy. */
    fun isFull(): Boolean
}