/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/29/20, 10:32 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.ecs.systems

import com.badlogic.gdx.math.Vector3
import xyz.angm.rox.Entity
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.rox.systems.IteratingSystem
import xyz.angm.terra3d.common.ecs.components.NoPhysicsFlag
import xyz.angm.terra3d.common.ecs.components.PositionComponent
import xyz.angm.terra3d.common.ecs.components.VelocityComponent
import xyz.angm.terra3d.common.ecs.direction
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.ecs.velocity
import xyz.angm.terra3d.server.ecs.systems.PhysicsSystem.Companion.MAX_FALL
import kotlin.math.max

/** A heavily simplified version of the server-side physics system.
 * Only used to smooth the frames between server physics updates, which
 * are choppy due to the server only updating 20x a second (but the client rendering at vsync). */
class PhysicsInterpolationSystem : IteratingSystem(
    allOf(
        PositionComponent::class,
        VelocityComponent::class
    ).exclude(NoPhysicsFlag::class)
) {

    private val tmpV = Vector3()

    override fun process(entity: Entity, delta: Float) {
        val velocity = entity[velocity]
        velocity.scl(velocity.accelerationRate)
        velocity.y -= velocity.gravity * delta
        velocity.y = max(velocity.y, MAX_FALL)

        val direction = entity.c(direction)
        if (direction == null) {
            entity[position].add(tmpV.set(velocity).scl(delta))
            return
        }
        entity[position].y += velocity.y * delta

        // Following code is abridged from libGDXs built-in FirstPersonCameraController
        // (https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/graphics/g3d/utils/FirstPersonCameraController.java)
        if (velocity.x != 0f) {
            tmpV.set(direction)
            tmpV.y = 0f
            tmpV.nor().scl(velocity.x * delta * velocity.speedModifier)
            entity[position].add(tmpV)
        }
        if (velocity.z != 0f) {
            tmpV.set(direction).crs(0f, 1f, 0f).nor().scl(velocity.z * delta * velocity.speedModifier)
            entity[position].add(tmpV)
        }
    }
}