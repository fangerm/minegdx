package xyz.angm.terra3d.common.items.metadata

import xyz.angm.terra3d.common.items.Inventory

/** A simple metadata interface for all metadata that
 * contains an inventory and can interact with translocators
 * and other item movement mechanisms. */
interface InventoryMetadata : IMetadata {

    /** The inventory to pull items from. */
    val pull: Inventory

    /** The inventory to push items into. */
    val push: Inventory
}