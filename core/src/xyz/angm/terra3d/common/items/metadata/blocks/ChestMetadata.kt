/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/16/20, 5:54 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata.blocks

import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.metadata.InventoryMetadata

/** Metadata for a chest.
 * @property inventory The chest's inventory. */
class ChestMetadata : InventoryMetadata {

    var inventory = Inventory(54)

    override val pull get() = inventory
    override val push get() = inventory

    override fun toString() = inventory.toString()
    override fun equals(other: Any?) = other is ChestMetadata && other.inventory == inventory
}