/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/1/20, 9:50 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.ecs.systems

import com.badlogic.gdx.math.MathUtils
import xyz.angm.rox.Entity
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.rox.systems.IteratingSystem
import xyz.angm.terra3d.common.ecs.components.specific.DayTimeComponent
import xyz.angm.terra3d.common.ecs.dayTime

/** The movement speed of the sun. */
private const val SUN_SPEED = 0.01f

/** This system is only used for the DayTime entity, which is responsible for
 * advancing and syncing day time. */
class DayTimeSystem : IteratingSystem(allOf(DayTimeComponent::class)) {

    override fun process(entity: Entity, delta: Float) {
        val c = entity[dayTime]
        c.time += delta * SUN_SPEED
        if (c.time > MathUtils.PI2) c.time = 0f
    }
}
