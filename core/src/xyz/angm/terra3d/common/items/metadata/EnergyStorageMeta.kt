/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 5:16 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata

import kotlin.math.min

/** An interface for all metadata that contains an energy buffer
 * and can receive or send energy. */
interface EnergyStorageMeta : IMetadata {

    /** Put [amount] of energy into the buffer.
     * @return The amount accepted. */
    fun receive(amount: Int): Int
}

/** Convenience adapter for implementing a simple buffer with max capacity. */
interface EnergyStorageAdapter : EnergyStorageMeta {

    /** The amount stored in the buffer */
    var energy: Int

    /** The maximum amount the buffer can store */
    val max: Int

    override fun receive(amount: Int): Int {
        val diff = max - energy
        energy = min(energy + amount, max)
        return min(diff, amount)
    }
}