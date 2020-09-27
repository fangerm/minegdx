/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/27/20, 2:15 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

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