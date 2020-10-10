/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/10/20, 9:47 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.world.generation

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import ktx.assets.file
import xyz.angm.terra3d.common.yaml
import java.util.*
import kotlin.math.min

/** A biome represents a type of terrain like a forest or a desert.
 * @property surfaceBlock The block at the surface
 * @property surfaceLayers The amount of layers of surface blocks
 * @property minimumSurfaceHeight Minimum height of the surface
 * @property surfaceHeightDeviation Deviation of the height, the higher, the steeper the terrain
 * @property belowSurfaceBlock The block right below the surface
 * @property belowSurfaceLayers The amount of layers of below-surface blocks
 * @property structuresGenerating The structures generating in the biome, see [Structure] */
@Serializable
class Biome {

    /** Used for storing biomes. */
    companion object {
        private val biomes = yaml.decodeFromString(MapSerializer(String.serializer(), serializer()), file("biomes.yaml").readString())

        /** Gets the biome to apply by the noise output. See [NoiseGenerator]. */
        fun getBiomeByNoise(noise: Float): Biome {
            val adjNoise = (noise + 1f) / 2f // adjNoise is now 0 < noise < 1
            return biomes.values.elementAt(min((biomes.values.size * adjNoise).toInt(), biomes.values.size - 1))
        }
    }

    val surfaceBlock = "grass_block"
    val surfaceLayers = 1
    val minimumSurfaceHeight = 40
    val surfaceHeightDeviation = 10
    val belowSurfaceBlock = "dirt"
    val belowSurfaceLayers = 3
    val structuresGenerating = HashMap<String, Double>()
}
