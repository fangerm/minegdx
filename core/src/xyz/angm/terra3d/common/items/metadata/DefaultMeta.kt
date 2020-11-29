/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/29/20, 4:23 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata

import com.badlogic.gdx.utils.IntMap
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.items.metadata.blocks.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/** This object holds metadata for all item types.
 * When an item is first created, it's metadata is
 * retrieved from here and copied. */
object DefaultMeta {

    private val metadata = IntMap<KClass<out IMetadata>>()

    init {
        fun add(name: String, t: KClass<out IMetadata>) = metadata.put(Item.Properties.fromIdentifier(name).type, t)

        // Add custom metadata here
        add("furnace", FurnaceMetadata::class)
        add("electric_furnace", ElectricFurnaceMetadata::class)
        add("chest", ChestMetadata::class)
        add("generator", GeneratorMetadata::class)
        add("energy_translocator", EnergyTranslocatorMetadata::class)
        add("item_translocator", TranslocatorMetadata::class)
        add("configurator", ConfiguratorMetadata::class)
        add("energy_cell", EnergyCellMetadata::class)
        for (miner in listOf("stone_miner", "iron_miner", "gold_miner", "diamond_miner")) {
            add(miner, MinerMetadata::class)
        }
    }

    infix fun of(type: ItemType): IMetadata? = metadata[type]?.createInstance()
}