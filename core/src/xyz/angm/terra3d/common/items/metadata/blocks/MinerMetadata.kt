/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 5:16 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata.blocks

import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.metadata.EnergyStorageAdapter
import xyz.angm.terra3d.common.items.metadata.InventoryMetadata

/** Metadata for ore miners. */
class MinerMetadata : EnergyStorageAdapter, InventoryMetadata {

    override var energy = 0
    override val max = 1000
    var inventory = Inventory(27)

    override val pull get() = inventory
    override val push = Inventory(0)

    override fun toString() = "${I18N["meta.stored"]}: $energy"
}