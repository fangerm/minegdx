/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.ecs.systems

import xyz.angm.rox.EntitySystem
import xyz.angm.terra3d.common.world.BfsFluid

/** A very simple system that ticks fluids. */
class FluidSystem(private val alg: BfsFluid) : EntitySystem() {

    var delta = 0f

    override fun update(deltaT: Float) {
        delta += deltaT
        if (delta > 0.4f) {
            alg.tick()
            delta = 0f
        }
    }
}