package xyz.angm.terra3d.common.world.generation

import xyz.angm.terra3d.common.CHUNK_SIZE
import kotlin.math.sqrt
import kotlin.random.Random

/** Radius of the rectangle of blocks checked around a block to
 * smooth the block height, used to prevent cliffs at biome edges. */
private const val HEIGHT_SMOOTH_R = 3

/** Generates noise for the terrain.
 * @param seed The world seed */
class NoiseGenerator(seed: Long) {

    // A lot of this class is abridged from (http://www.java-gaming.org/index.php?topic=31637.0) and
    // Stefan Gustavsons paper on simplex noise: (http://staffwww.itn.liu.se/~stegu/simplexnoise/simplexnoise.pdf)
    // It was converted from Java to Kotlin using IntelliJ IDEAs automatic converter.

    // These values heavily influence heightmap generation.
    private val octaves = 3
    private val roughnessHeight = 0.6f
    private val roughnessBiome = 0.3f
    private val heightScale = 0.01
    private val biomeScale = 0.005

    private val heightMapPerm = IntArray(512)
    private val biomeMapPerm = IntArray(512)

    init {
        val random = Random(seed)
        val heightMapP = IntArray(256) { random.nextInt(256) }
        val biomeMapP = IntArray(256) { random.nextInt(256) }
        for (i in 0 until 512) {
            heightMapPerm[i] = heightMapP[i and 255]
            biomeMapPerm[i] = biomeMapP[i and 255]
        }
    }

    /** Returns the terrain height for a chunk.
     * @param posX Chunk X position
     * @param posZ Chunk Y position
     * @param biomeMap Biome map for the chunk
     * @return Height map for chunk*/
    fun generateChunkHeightMap(posX: Int, posZ: Int): Array<IntArray> {
        val result = Array(CHUNK_SIZE) {
            IntArray(CHUNK_SIZE)
        }

        for (x in 0 until CHUNK_SIZE) {
            for (z in 0 until CHUNK_SIZE) {
                var heightTotal = 0f
                for (xD in -HEIGHT_SMOOTH_R..HEIGHT_SMOOTH_R) {
                    for (zD in -HEIGHT_SMOOTH_R..HEIGHT_SMOOTH_R) {
                        val bNoise = generateNoisePoint(posX + x + xD, posZ + z + zD, biomeScale, roughnessBiome, biomeMapPerm)
                        val biome = Biome.getBiomeByNoise(bNoise)
                        var value = generateNoisePoint(posX + x + xD, posZ + z + zD, heightScale, roughnessHeight, heightMapPerm)
                        value++
                        value /= 2 // value is now 0.0 to 1.0
                        heightTotal += biome.minimumSurfaceHeight + (biome.surfaceHeightDeviation * value)
                    }
                }

                result[x][z] = (heightTotal / (((HEIGHT_SMOOTH_R * 2) + 1) * ((HEIGHT_SMOOTH_R * 2) + 1))).toInt()
            }
        }
        return result
    }

    /** Returns the biome map for a chunk.
     * @param posX Chunk X position
     * @param posZ Chunk Y position
     * @return Biome map for chunk*/
    fun generateChunkBiomeMap(posX: Int, posZ: Int): Array<Array<Biome>> {
        val noise = generateChunkBiomeNoise(posX, posZ)
        return Array(CHUNK_SIZE) { x ->
            Array(CHUNK_SIZE) {
                Biome.getBiomeByNoise(noise[x][it])
            }
        }
    }

    // Generates a biome 2D noise map for a chunk. Values are float; -1 < x < 1
    private fun generateChunkBiomeNoise(startX: Int, startZ: Int): Array<FloatArray> {
        val totalNoise = Array(CHUNK_SIZE) {
            FloatArray(CHUNK_SIZE)
        }
        var layerFrequency = biomeScale
        var layerWeight = 1f
        var weightSum = 0f

        for (octave in 0 until octaves) {
            // Calculate single layer/octave of simplex noise, then add it to total noise
            for (x in 0 until CHUNK_SIZE) {
                for (z in 0 until CHUNK_SIZE) {
                    totalNoise[x][z] += noise((startX + x) * layerFrequency, (startZ + z) * layerFrequency, biomeMapPerm).toFloat() * layerWeight
                }
            }
            // Increase variables with each incrementing octave
            layerFrequency *= 2f
            weightSum += layerWeight
            layerWeight *= roughnessBiome
        }
        for (x in 0 until CHUNK_SIZE) {
            for (z in 0 until CHUNK_SIZE) {
                totalNoise[x][z] /= weightSum
            }
        }
        return totalNoise
    }

    // Generates a generic 2D noise map for a chunk. Values are float; -1 < x < 1
    private fun generateNoisePoint(x: Int, z: Int, scale: Double, roughness: Float, perm: IntArray): Float {
        var layerFrequency = scale
        var layerWeight = 1f
        var weightSum = 0f
        var out = 0f

        for (octave in 0 until octaves) {
            out += noise(x * layerFrequency, z * layerFrequency, perm).toFloat() * layerWeight
            // Increase variables with each incrementing octave
            layerFrequency *= 2f
            weightSum += layerWeight
            layerWeight *= roughness
        }
        return out / weightSum
    }

    // -v- Taken from Stefan Gustavsons paper on simplex noise -v- \\
    // See his paper for a commented version: (http://staffwww.itn.liu.se/~stegu/simplexnoise/simplexnoise.pdf)
    private val grad3 = arrayOf(
        intArrayOf(1, 1, 0),
        intArrayOf(-1, 1, 0),
        intArrayOf(1, -1, 0),
        intArrayOf(-1, -1, 0),
        intArrayOf(1, 0, 1),
        intArrayOf(-1, 0, 1),
        intArrayOf(1, 0, -1),
        intArrayOf(-1, 0, -1),
        intArrayOf(0, 1, 1),
        intArrayOf(0, -1, 1),
        intArrayOf(0, 1, -1),
        intArrayOf(0, -1, -1)
    )

    private fun noise(xin: Double, yin: Double, perm: IntArray): Double {
        val n0: Double
        val n1: Double
        val n2: Double
        val f2 = 0.5 * (sqrt(3.0) - 1.0)
        val s = (xin + yin) * f2
        val i = fastFloor(xin + s)
        val j = fastFloor(yin + s)
        val g2 = (3.0 - sqrt(3.0)) / 6.0
        val t = (i + j) * g2
        val x00 = i - t
        val y00 = j - t
        val x0 = xin - x00
        val y0 = yin - y00
        val i1: Int
        val j1: Int
        if (x0 > y0) {
            i1 = 1
            j1 = 0
        } else {
            i1 = 0
            j1 = 1
        }
        val x1 = x0 - i1 + g2
        val y1 = y0 - j1 + g2
        val x2 = x0 - 1.0 + 2.0 * g2
        val y2 = y0 - 1.0 + 2.0 * g2
        val ii = i and 255
        val jj = j and 255
        val gi0 = perm[ii + perm[jj]] % 12
        val gi1 = perm[ii + i1 + perm[jj + j1]] % 12
        val gi2 = perm[ii + 1 + perm[jj + 1]] % 12
        var t0 = 0.5 - x0 * x0 - y0 * y0
        if (t0 < 0) n0 = 0.0
        else {
            t0 *= t0
            n0 = t0 * t0 * dot(grad3[gi0], x0, y0)
        }
        var t1 = 0.5 - x1 * x1 - y1 * y1
        if (t1 < 0) n1 = 0.0
        else {
            t1 *= t1
            n1 = t1 * t1 * dot(grad3[gi1], x1, y1)
        }
        var t2 = 0.5 - x2 * x2 - y2 * y2
        if (t2 < 0) n2 = 0.0
        else {
            t2 *= t2
            n2 = t2 * t2 * dot(grad3[gi2], x2, y2)
        }
        return 70.0 * (n0 + n1 + n2)
    }

    private fun fastFloor(x: Double) = if (x > 0) x.toInt() else x.toInt() - 1

    private fun dot(g: IntArray, x: Double, y: Double) = g[0] * x + g[1] * y
}