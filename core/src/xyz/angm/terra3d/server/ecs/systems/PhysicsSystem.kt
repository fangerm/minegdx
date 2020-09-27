/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/20/20, 8:02 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server.ecs.systems

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import xyz.angm.rox.Entity
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.rox.IteratingSystem
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.*
import xyz.angm.terra3d.common.ecs.components.NoPhysicsFlag
import xyz.angm.terra3d.common.ecs.components.PositionComponent
import xyz.angm.terra3d.common.ecs.components.VelocityComponent

/** Used for movement, collision and gravity.
 *
 * Called on all entities with Position, Direction and Velocity components.
 *
 * @param colliderAt Returns the collider of the block at the given position. */
class PhysicsSystem(
    private val colliderAt: (Vector3) -> BlockCollider
) : IteratingSystem(
    allOf(
        PositionComponent::class,
        VelocityComponent::class
    ).exclude(NoPhysicsFlag::class)
) {

    private val tmpIV = IntVector3()
    private val tmpIV2 = IntVector3()
    private val tmpV = Vector3()
    private val tmpV2 = Vector3()
    private val blockBelow = Vector3()
    private val blockAbove = Vector3()

    /** Update the entities position based on a very inaccurate physics simulation. */
    override fun process(entity: Entity, delta: Float) {
        val position = entity[position]
        val velocity = entity[velocity]
        val network = entity.c(network)

        velocity.scl(velocity.accelerationRate)
        checkFailsafes(entity)
        applyGravity(velocity, delta)
        position.set(getNextPosition(entity, delta))

        blockBelow.set(position)
        blockAbove.set(position).add(0f, entity.size.y, 0f)

        if (colliderAt(blockBelow) != BlockCollider.NONE) {
            applyFloorCollision(position, velocity)
            network?.needsSync = true
        }
        if (colliderAt(blockAbove) != BlockCollider.NONE) {
            applyCeilingCollision(velocity)
            network?.needsSync = true
        }

        if (velocity.x != 0f || velocity.z != 0f) {
            for (x in -1..1)
                for (z in -1..1)
                    checkSideCollision(entity, x, z)

            network?.needsSync = true
        }
    }

    private fun applyCeilingCollision(velocity: VelocityComponent) {
        if (velocity.y > 0f) velocity.y = 0f
    }

    private fun applyFloorCollision(position: PositionComponent, velocity: VelocityComponent) {
        position.y = MathUtils.floor(blockBelow.y) + 1f
        velocity.y = 0f
    }

    private fun applyGravity(velocity: VelocityComponent, delta: Float) {
        if (velocity.gravity) velocity.y -= GRAVITY * delta
    }

    private fun checkSideCollision(entity: Entity, x: Int, z: Int) {
        val position = entity[position]
        val size = entity.size

        for (i in 0..size.y.toInt()) {
            val collider = colliderAt(tmpV2.set(position).sub(x.toFloat() * size.x, i.toFloat(), z.toFloat() * size.z))
            if (collider != BlockCollider.NONE) {
                val blockPos = tmpIV.set(tmpV2)
                val diff = tmpIV2.set(position).minus(blockPos)
                when {
                    diff.x != 0 && diff.z != 0 -> {
                    }
                    diff.x < 0 -> position.x = blockPos.x - size.x
                    diff.x > 0 -> position.x = (blockPos.x + 1) + size.x
                    diff.z < 0 -> position.z = blockPos.z - size.z
                    diff.z > 0 -> position.z = (blockPos.z + 1) + size.z
                }
            }
        }
    }

    private fun checkFailsafes(entity: Entity) {
        // Failsafe if the entity somehow ended up below the world; kill it then
        if (entity[position].y < -32f) {
            entity.c(health)?.health = 0
            entity.c(network)?.needsSync = true
        }
    }

    /** Gets next position, taking the direction into account. Uses tmpV as return vector.
     * @param entity The entity to apply to
     * @param delta Time since last call */
    private fun getNextPosition(entity: Entity, delta: Float): Vector3 {
        val direction = entity.c(direction) ?: return tmpV.set(entity[position]).add(tmpV2.set(entity[velocity]).scl(delta))

        tmpV.set(entity[position])
        tmpV.y += entity[velocity].y * delta * (GRAVITY / 2)

        // Following code is abridged from libGDXs built-in FirstPersonCameraController
        // (https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/graphics/g3d/utils/FirstPersonCameraController.java)
        if (entity[velocity].x != 0f) {
            tmpV2.set(direction)
            tmpV2.y = 0f
            tmpV2.nor().scl(entity[velocity].x * delta * entity[velocity].speedModifier)
            tmpV.add(tmpV2)
        }
        if (entity[velocity].z != 0f) {
            tmpV2.set(direction).crs(0f, 1f, 0f).nor().scl(entity[velocity].z * delta * entity[velocity].speedModifier)
            tmpV.add(tmpV2)
        }

        return tmpV
    }

    /** The various types of block colliders in the game. */
    enum class BlockCollider {
        /** A full block taking the entire 1x1x1 space.*/
        FULL,

        /** A half block taking 1x0.5x1 space, on the lower half. */
        HALF_LOWER,

        /** A half block taking 1x0.5x1 space, on the upper half. */
        HALF_UPPER,

        /** Either air or a block that isn't solid, like a fluid. */
        NONE;
    }

    companion object {

        /** The gravity multiplier for all entities. */
        const val GRAVITY = 8f

        private val itemSize = Vector3(0.2f, 0.2f, 0.2f)
        private val humanoidSize = Vector3(0.4f, 1.85f, 0.4f)

        private val Entity.size
            get() =
                when {
                    this has item -> itemSize
                    else -> humanoidSize
                }
    }
}
