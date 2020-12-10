/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/30/20, 12:19 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.koise

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

const val RADIUS = 3

/** Noise generator for caves. */
class CaveGenerator(private val size: Int, private val seed: Long) {

    fun carve(data: Array<Array<BooleanArray>>, xpos: Int, ypos: Int, zpos: Int) {
        val random = Random((xpos * ypos + zpos).toLong() + seed)
        val xStart = random.nextInt(size)
        val yStart = random.nextInt(size)
        val zStart = random.nextInt(size)
        val xStep = random.nextDouble(4.0) - 2
        val yStep = -random.nextDouble(2.0)
        val zStep = random.nextDouble(4.0) - 2
        val length = random.nextInt(10, 20)

        var xF = xStart.toDouble()
        var yF = yStart.toDouble()
        var zF = zStart.toDouble()

        repeat(length) {
            xF += xStep
            yF += yStep
            zF += zStep
            val x = xF.toInt()
            val y = yF.toInt()
            val z = zF.toInt()
            if (x < 0 || y < 0 || z < 0 || x >= size || y >= size || z >= size) return
            carvePoint(data, x, y, z)
        }
    }

    private fun carvePoint(data: Array<Array<BooleanArray>>, xP: Int, yP: Int, zP: Int) {
        for (x in (max(0, xP - RADIUS)..(min(xP + RADIUS, size - 1))))
            for (y in (max(0, yP - RADIUS)..(min(yP + RADIUS, size - 1))))
                for (z in (max(0, zP - RADIUS)..(min(zP + RADIUS, size - 1)))) {
                    data[x][z][y] = false
                }
    }
}