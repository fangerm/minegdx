/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/16/20, 6:10 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata.blocks

import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.metadata.EnergyStorageMeta
import xyz.angm.terra3d.common.items.metadata.InventoryMetadata

/** Metadata for ore miners. */
class MinerMetadata : EnergyStorageMeta, InventoryMetadata {

    var energy = 0
    var inventory = Inventory(27)

    override val pull get() = inventory
    override val push = Inventory(0)

    override fun receive(amount: Int) {
        energy += amount
    }

    override fun isFull() = energy > 1000

    override fun toString() = "${I18N["meta.stored"]}: $energy"
}