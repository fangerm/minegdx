package xyz.angm.terra3d.common.items.metadata

import xyz.angm.terra3d.common.items.Inventory

/** Metadata for a chest.
 * @property inventory The chest's inventory. */
class ChestMetadata : IMetadata {
    var inventory = Inventory(54)

    /** Returns string representation of the inventory. */
    override fun toString() = inventory.toString()
}