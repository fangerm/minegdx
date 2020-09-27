package xyz.angm.terra3d.common.items.metadata

import xyz.angm.terra3d.common.items.Inventory

/** Metadata for a chest.
 * @property inventory The chest's inventory. */
class ChestMetadata : InventoryMetadata {

    var inventory = Inventory(54)

    override val pull get() = inventory
    override val push get() = inventory

    override fun toString() = inventory.toString()
    override fun equals(other: Any?) = other is ChestMetadata && other.inventory == inventory
}