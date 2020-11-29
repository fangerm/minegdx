/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/29/20, 4:28 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata.blocks

import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.metadata.EnergyStorageAdapter
import xyz.angm.terra3d.common.items.metadata.InventoryMetadata
import xyz.angm.terra3d.common.recipes.FurnaceRecipes
import xyz.angm.terra3d.common.recipes.OneToOneRecipes

/** Metadata for a machine with 1:1 processing that uses energy,
 * like an electric furnace.
 * @property progress Progress of the current operation. Range 0-100.
 * @property energy The energy stored.
 * @property processing The slot that is being processed.
 * @property result The result slot.
 * @property recipes The recipes available to the machine */
abstract class GenericProcessingMachineMetadata : EnergyStorageAdapter, InventoryMetadata {

    var progress = 0
    var processing = Inventory(1)
    var result = Inventory(1)

    abstract val recipes: OneToOneRecipes

    override val pull get() = result
    override val push get() = processing
    override var energy = 0
    override val max = 2000

    override fun toString() = """

        Progress:       $progress
        Energy:         $energy
        Processing:     $processing
        Result:         $result
    """.replace("null", "None").trimIndent()

    override fun equals(other: Any?) =
        other is GenericProcessingMachineMetadata && other.energy == energy && other.processing == processing && other.result == result
}

class ElectricFurnaceMetadata : GenericProcessingMachineMetadata() {
    override val recipes get() = FurnaceRecipes
}