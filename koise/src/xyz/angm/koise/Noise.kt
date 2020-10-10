/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/4/20, 8:44 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.koise

import kotlin.math.sqrt
import kotlin.random.Random

/** Generates 2D or 3D noise.
 * @param seed The seed to use for perm generation
 * @param octaves The amount of noise points to sample, more = smoother
 * @param roughness Postmultiplier for each octave, higher results in rougher results
 * @param scale The scale of the noise, smaller = bigger / more stretched */
class Noise(
    private val seed: Long,
    private val octaves: Int,
    private val roughness: Float,
    private val scale: Double
) {

    // A lot of this class is abridged from (http://www.java-gaming.org/index.php?topic=31637.0) and
    // Stefan Gustavsons paper on simplex noise: (http://staffwww.itn.liu.se/~stegu/simplexnoise/simplexnoise.pdf)
    // It was converted from Java to Kotlin using IntelliJ IDEAs automatic converter.

    private val perm = IntArray(512)

    init {
        val random = Random(seed)
        val p = IntArray(256) { random.nextInt(256) }
        for (i in 0 until 512) {
            perm[i] = p[i and 255]
        }
    }

    /** Generate a 2D point using simplex noise. */
    fun point(x: Int, z: Int) = pointInternal { noise(x * it, z * it) }

    /** Generate a 3D point using cellular (!!) noise. */
    fun point(x: Int, y: Int, z: Int) = CellularNoise.cellular(seed.toInt(), (scale * x).toFloat(), (scale * y).toFloat(), (scale * z).toFloat())

    private inline fun pointInternal(noise: (Double) -> Double): Float {
        var layerFrequency = scale
        var layerWeight = 1f
        var weightSum = 0f
        var out = 0f

        for (octave in 0 until octaves) {
            out += noise(layerFrequency).toFloat() * layerWeight
            // Increase variables with each incrementing octave
            layerFrequency *= 2f
            weightSum += layerWeight
            layerWeight *= roughness
        }
        return out / weightSum
    }

    // -v- Taken from Stefan Gustavsons paper on simplex noise -v- \\
    // See his paper for a commented version: (http://staffwww.itn.liu.se/~stegu/simplexnoise/simplexnoise.pdf)
    // Auto-converted using IntelliJ IDEA, code isn't pretty due to this.
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

    private fun noise(xin: Double, yin: Double): Double {
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

    fun noise(xin: Double, yin: Double, zin: Double): Double {
        val n0: Double
        val n1: Double
        val n2: Double
        val n3: Double

        val F3 = 1.0 / 3.0
        val s = (xin + yin + zin) * F3
        val i = fastFloor(xin + s)
        val j = fastFloor(yin + s)
        val k = fastFloor(zin + s)
        val G3 = 1.0 / 6.0
        val t = (i + j + k) * G3
        val X0 = i - t
        val Y0 = j - t
        val Z0 = k - t
        val x0 = xin - X0
        val y0 = yin - Y0
        val z0 = zin - Z0
        val i1: Int
        val j1: Int
        val k1: Int
        val i2: Int
        val j2: Int
        val k2: Int
        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1
                j1 = 0
                k1 = 0
                i2 = 1
                j2 = 1
                k2 = 0
            } else if (x0 >= z0) {
                i1 = 1
                j1 = 0
                k1 = 0
                i2 = 1
                j2 = 0
                k2 = 1
            } else {
                i1 = 0
                j1 = 0
                k1 = 1
                i2 = 1
                j2 = 0
                k2 = 1
            }
        } else {
            if (y0 < z0) {
                i1 = 0
                j1 = 0
                k1 = 1
                i2 = 0
                j2 = 1
                k2 = 1
            } else if (x0 < z0) {
                i1 = 0
                j1 = 1
                k1 = 0
                i2 = 0
                j2 = 1
                k2 = 1
            } else {
                i1 = 0
                j1 = 1
                k1 = 0
                i2 = 1
                j2 = 1
                k2 = 0
            }
        }
        val x1 = x0 - i1 + G3
        val y1 = y0 - j1 + G3
        val z1 = z0 - k1 + G3
        val x2 = x0 - i2 + 2.0 * G3
        val y2 = y0 - j2 + 2.0 * G3
        val z2 = z0 - k2 + 2.0 * G3
        val x3 = x0 - 1.0 + 3.0 * G3
        val y3 = y0 - 1.0 + 3.0 * G3
        val z3 = z0 - 1.0 + 3.0 * G3

        val ii = i and 255
        val jj = j and 255
        val kk = k and 255
        val gi0 = perm[ii + perm[jj + perm[kk]]] % 12
        val gi1 = perm[ii + i1 + perm[jj + j1 + perm[kk + k1]]] % 12
        val gi2 = perm[ii + i2 + perm[jj + j2 + perm[kk + k2]]] % 12
        val gi3 = perm[ii + 1 + perm[jj + 1 + perm[kk + 1]]] % 12

        var t0 = 0.5 - x0 * x0 - y0 * y0 - z0 * z0
        if (t0 < 0) n0 = 0.0 else {
            t0 *= t0
            n0 = t0 * t0 * dot(grad3[gi0], x0, y0, z0)
        }
        var t1 = 0.5 - x1 * x1 - y1 * y1 - z1 * z1
        if (t1 < 0) n1 = 0.0 else {
            t1 *= t1
            n1 = t1 * t1 * dot(grad3[gi1], x1, y1, z1)
        }
        var t2 = 0.5 - x2 * x2 - y2 * y2 - z2 * z2
        if (t2 < 0) n2 = 0.0 else {
            t2 *= t2
            n2 = t2 * t2 * dot(grad3[gi2], x2, y2, z2)
        }
        var t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3
        if (t3 < 0) n3 = 0.0 else {
            t3 *= t3
            n3 = t3 * t3 * dot(grad3[gi3], x3, y3, z3)
        }

        return 32.0 * (n0 + n1 + n2 + n3)
    }

    private fun dot(g: IntArray, x: Double, y: Double) = g[0] * x + g[1] * y
    private fun dot(g: IntArray, x: Double, y: Double, z: Double) = g[0] * x + g[1] * y + g[2] * z

    private fun fastFloor(x: Double) = if (x > 0) x.toInt() else x.toInt() - 1
}