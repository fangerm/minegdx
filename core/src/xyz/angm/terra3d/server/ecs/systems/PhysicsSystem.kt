package xyz.angm.terra3d.server.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.*
import xyz.angm.terra3d.common.ecs.components.*
import xyz.angm.terra3d.common.world.Block

/** Used for movement, collision and gravity.
 *
 * Called on all entities with Position, Direction and Velocity components.
 *
 * @param getBlock Function for getting blocks from the world */
class PhysicsSystem(
    private val getBlock: (Vector3) -> Block?
) : IteratingSystem(
    allOf(
        PositionComponent::class,
        DirectionComponent::class,
        VelocityComponent::class
    ).exclude(NoPhysicsFlag::class).get()
) {

    private val tmpIV = IntVector3()
    private val tmpV = Vector3()
    private val tmpV2 = Vector3()
    private val blockBelow = Vector3()
    private val blockAbove = Vector3()

    /** Update the entities position based on a very inaccurate (TODO?) physics simulation. */
    override fun processEntity(entity: Entity, delta: Float) {
        val position = entity[position]!!
        val velocity = entity[velocity]!!
        val network = entity[network]

        checkFailsafes(entity)
        applyGravity(velocity, delta)
        position.set(getNextPosition(entity, delta))

        blockBelow.set(position).sub(0f, (entity[size]?.y ?: 0f) + 0.001f, 0f)
        blockAbove.set(position).add(0f, 0.1f, 0f)

        if (getBlock(blockBelow) != null) {
            applyFloorCollision(entity, position, velocity)
            network?.needsSync = true
        }
        if (getBlock(blockAbove) != null) {
            applyCeilingCollision(velocity)
            network?.needsSync = true
        }

        if (velocity.x != 0f || velocity.z != 0f) {
            for (x in -1..1)
                for (z in -1..1)
                    checkSideCollision(entity, delta, x, z)

            network?.needsSync = true
        }
    }

    private fun applyCeilingCollision(velocity: VelocityComponent) {
        if (velocity.y > 0f) velocity.y = 0f
    }

    private fun applyFloorCollision(entity: Entity, position: PositionComponent, velocity: VelocityComponent) {
        if (velocity.y < fallDamageMinBound) applyFallDamage(entity)
        position.y = MathUtils.floor(blockBelow.y).toFloat() + (entity[size]?.y ?: 0f) + 1f
        velocity.y = 0f
    }

    private fun applyGravity(velocity: VelocityComponent, delta: Float) {
        if (velocity.gravity) velocity.y -= gravity * delta
    }

    private val fallDamageMinBound = -5f
    private val fallDamageMaxBound = -9.5f

    private fun applyFallDamage(entity: Entity) {
        val health = entity[health] ?: return
        var velocity = entity[velocity]!!.y
        velocity -= fallDamageMinBound
        health.health -= (velocity * (health.maxHealth / (fallDamageMaxBound - fallDamageMinBound))).toInt()
    }

    private fun checkSideCollision(entity: Entity, delta: Float, x: Int, z: Int) {
        val nextPos = getNextPosition(entity, delta)
        val position = entity[position]!!
        val size = entity[size] ?: defaultSize

        for (i in 0..size.y.toInt()) {
            val block = getBlock(nextPos.sub(x.toFloat() * size.x, i.toFloat() + 0.5f, z.toFloat() * size.z))
            if (block != null) {
                val diff = tmpIV.set(position).minus(block.position)
                when {
                    diff.x < 0 -> position.x = block.position.x - size.x
                    diff.x > 0 -> position.x = (block.position.x + 1) + size.x
                    diff.z < 0 -> position.z = block.position.z - size.z
                    diff.z > 0 -> position.z = (block.position.z + 1) + size.z
                }
            }
        }
    }

    private fun checkFailsafes(entity: Entity) {
        // Failsafe if the entity somehow ended up below the world; kill it then
        if (entity[position]!!.y < -32f) {
            entity[health]?.health?.unaryMinus()
            entity[network]?.needsSync = true
        }
    }

    /** Gets next position, taking the direction into account. Uses tmpV as return vector.
     * @param entity The entity to apply to
     * @param delta Time since last call */
    private fun getNextPosition(entity: Entity, delta: Float): Vector3 {
        tmpV.set(entity[position]!!)
        tmpV.y += entity[velocity]!!.y * delta * (gravity / 2)

        // Following code is abridged from libGDXs built-in FirstPersonCameraController
        // (https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/graphics/g3d/utils/FirstPersonCameraController.java)
        if (entity[velocity]!!.x != 0f) {
            tmpV2.set(entity[direction]!!)
            tmpV2.y = 0f
            tmpV2.nor().scl(entity[velocity]!!.x * delta * entity[velocity]!!.speedModifier)
            tmpV.add(tmpV2)
        }
        if (entity[velocity]!!.z != 0f) {
            tmpV2.set(entity[direction]!!).crs(0f, 1f, 0f).nor().scl(entity[velocity]!!.z * delta * entity[velocity]!!.speedModifier)
            tmpV.add(tmpV2)
        }

        return tmpV
    }

    private companion object {
        /** Used as a substitute for entities with no size. */
        private val defaultSize = SizeComponent()

        /** The gravity multiplier for all entities. */
        private const val gravity = 6f
    }
}
