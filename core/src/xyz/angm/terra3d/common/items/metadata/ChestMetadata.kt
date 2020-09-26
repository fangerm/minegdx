package xyz.angm.terra3d.common.items.metadata

import xyz.angm.terra3d.common.items.Inventory

/** Metadata for a chest.
 * @property inventory The chest's inventory. */
class ChestMetadata : IMetadata {
    var inventory = Inventory(54)

    override fun toString() = inventory.toString()
    override fun equals(other: Any?) = other is ChestMetadata && other.inventory == inventory
}