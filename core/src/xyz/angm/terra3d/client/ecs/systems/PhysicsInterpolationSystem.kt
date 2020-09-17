package xyz.angm.terra3d.client.ecs.systems

import com.badlogic.gdx.math.Vector3
import xyz.angm.rox.Entity
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.rox.IteratingSystem
import xyz.angm.terra3d.common.ecs.components.NoPhysicsFlag
import xyz.angm.terra3d.common.ecs.components.PositionComponent
import xyz.angm.terra3d.common.ecs.components.VelocityComponent
import xyz.angm.terra3d.common.ecs.direction
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.ecs.velocity
import xyz.angm.terra3d.server.ecs.systems.PhysicsSystem

/** A heavily simplified version of the server-side physics system.
 * Only used to smooth the frames between server physics updates, which
 * are choppy due to the server only updating 20x a second (but the client rendering at 60fps). */
class PhysicsInterpolationSystem : IteratingSystem(
    allOf(
        PositionComponent::class,
        VelocityComponent::class
    ).exclude(NoPhysicsFlag::class)
) {

    private val tmpV = Vector3()

    override fun process(entity: Entity, delta: Float) {
        val direction = entity.c(direction)
        if (direction == null) {
            entity[position].add(this.tmpV.set(entity[velocity]).scl(delta))
            return
        }

        entity[position].y += entity[velocity].y * delta * (PhysicsSystem.GRAVITY / 2)

        // Following code is abridged from libGDXs built-in FirstPersonCameraController
        // (https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/graphics/g3d/utils/FirstPersonCameraController.java)
        if (entity[velocity].x != 0f) {
            tmpV.set(direction)
            tmpV.y = 0f
            tmpV.nor().scl(entity[velocity].x * delta * entity[velocity].speedModifier)
            entity[position].add(tmpV)
        }
        if (entity[velocity].z != 0f) {
            tmpV.set(direction).crs(0f, 1f, 0f).nor().scl(entity[velocity].z * delta * entity[velocity].speedModifier)
            entity[position].add(tmpV)
        }
    }
}