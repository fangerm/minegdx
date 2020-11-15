/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 5:21 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata

import com.badlogic.gdx.utils.IntMap
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.items.metadata.blocks.ConfiguratorMetadata
import xyz.angm.terra3d.common.items.metadata.blocks.EnergyCellMetadata
import xyz.angm.terra3d.common.items.metadata.blocks.EnergyTranslocatorMetadata
import xyz.angm.terra3d.common.items.metadata.blocks.TranslocatorMetadata
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
        add("energy_translocator", EnergyTranslocatorMetadata::class)
        add("item_translocator", TranslocatorMetadata::class)
        add("configurator", ConfiguratorMetadata::class)
        add("energy_cell", EnergyCellMetadata::class)
    }

    infix fun of(type: ItemType): IMetadata? = metadata[type]?.createInstance()
}