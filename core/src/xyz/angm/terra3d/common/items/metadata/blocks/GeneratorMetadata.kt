/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/29/20, 4:13 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata.blocks

import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.metadata.IMetadata
import xyz.angm.terra3d.common.items.metadata.InventoryMetadata

/** Metadata on a energy generator. */
class GeneratorMetadata : IMetadata, InventoryMetadata {

    var fuel = Inventory(1)
    var energyStored = 0

    override val pull get() = fuel
    override val push get() = fuel

    override fun toString() = """
        ${I18N["meta.stored"]}: $energyStored
        ${I18N["generator.fuel"]}: $fuel
    """.trimIndent()
}