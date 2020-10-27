/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/27/20, 5:35 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.world.generation

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import ktx.assets.file
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.yaml

/** An ore that generates in the world, usually in the ground.
 * @property maxHeight The maximum y height the ore spawns at
 * @property minHeight The minimum y height the ore spawns at
 * @property freq The frequency of spawning; amount of blocks of ore per chunk.
 * @property block The block placed as ore. */
@Serializable
class Ore {

    /** Used for storing ores. */
    companion object {
        val ores = yaml.decodeFromString(MapSerializer(String.serializer(), serializer()), file("ores.yaml").readString())

        init {
            for (ore in ores.values) {
                ore.blockId = Item.Properties.fromIdentifier(ore.block).type
            }
        }
    }

    val maxHeight = 0
    val minHeight = 0
    val freq = 0
    val block = ""
    var blockId = 0
}
