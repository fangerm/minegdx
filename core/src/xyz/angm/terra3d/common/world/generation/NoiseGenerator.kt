/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/2/20, 6:32 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.world.generation

import xyz.angm.koise.Noise
import xyz.angm.terra3d.common.CHUNK_SIZE

/** Radius of the rectangle of blocks checked around a block to
 * smooth the block height, used to prevent cliffs at biome edges. */
private const val HEIGHT_SMOOTH_R = 3

/** Generates noise for the terrain.
 * @param seed The world seed */
class NoiseGenerator(seed: Long) {

    // These values heavily influence heightmap generation.
    private val height = Noise(seed, octaves = 3, roughness = 0.6f, scale = 0.01)
    private val biome = Noise(seed, octaves = 3, roughness = 0.3f, scale = 0.005)

    /** Returns the noise data for a chunk.
     * @param posX Chunk X position
     * @param posX Chunk Y position
     * @param posZ Chunk Z position
     * @param out A ChunkData instance to fill with the data.
     * @return [out] */
    fun generate(posX: Int, posY: Int, posZ: Int, out: ChunkData): ChunkData {
        for (x in 0 until CHUNK_SIZE) {
            for (z in 0 until CHUNK_SIZE) {
                var heightTotal = 0f
                for (xD in -HEIGHT_SMOOTH_R..HEIGHT_SMOOTH_R) {
                    for (zD in -HEIGHT_SMOOTH_R..HEIGHT_SMOOTH_R) {
                        val biome = Biome.getBiomeByNoise(biome.point(posX + x + xD, posZ + z + zD))
                        var value = height.point(posX + x + xD, posZ + z + zD)
                        value++
                        value /= 2 // value is now 0.0 to 1.0
                        heightTotal += biome.minimumSurfaceHeight + (biome.surfaceHeightDeviation * value)
                    }
                }

                out.height[x][z] = (heightTotal / (((HEIGHT_SMOOTH_R * 2) + 1) * ((HEIGHT_SMOOTH_R * 2) + 1))).toInt()
                out.biome[x][z] = Biome.getBiomeByNoise(biome.point(posX + x, posZ + z))
            }
        }
        return out
    }

    /** Data class that contains all chunk noise data
     * @property height The height map of the chunk; surface block height of each point
     * @property biome Biome map of the chunk; biome of each point
     * @property caves Caves map, true means the block should be kept air. Order in XZY!! */
    class ChunkData {
        val height = Array(CHUNK_SIZE) { IntArray(CHUNK_SIZE) }
        val biome = Array(CHUNK_SIZE) { Array(CHUNK_SIZE) { Biome.getBiomeByNoise(0f) } }
        val caves = Array(CHUNK_SIZE) { Array(CHUNK_SIZE) { BooleanArray(CHUNK_SIZE) } }
    }
}