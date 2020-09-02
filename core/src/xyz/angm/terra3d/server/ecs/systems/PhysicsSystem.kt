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
import xyz.angm.terra3d.common.ecs.components.NoPhysicsFlag
import xyz.angm.terra3d.common.ecs.components.PositionComponent
import xyz.angm.terra3d.common.ecs.components.VelocityComponent
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
        VelocityComponent::class
    ).exclude(NoPhysicsFlag::class).get()
) {

    private val tmpIV = IntVector3()
    private val tmpV = Vector3()
    private val tmpV2 = Vector3()
    private val blockBelow = Vector3()
    private val blockAbove = Vector3()

    /** Update the entities position based on a very inaccurate physics simulation. */
    override fun processEntity(entity: Entity, delta: Float) {
        val position = entity[position]!!
        val velocity = entity[velocity]!!
        val network = entity[network]

        velocity.scl(velocity.accelerationRate)
        checkFailsafes(entity)
        applyGravity(velocity, delta)
        position.set(getNextPosition(entity, delta))

        blockBelow.set(position)
        blockAbove.set(position).add(0f, entity.size.y, 0f)

        if (getBlock(blockBelow) != null) {
            applyFloorCollision(position, velocity)
            network?.needsSync = true
        }
        if (getBlock(blockAbove) != null) {
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
        val position = entity[position]!!
        val size = entity.size

        for (i in 0..size.y.toInt()) {
            val block = getBlock(tmpV2.set(position).sub(x.toFloat() * size.x, i.toFloat(), z.toFloat() * size.z))
            if (block != null) {
                val diff = tmpIV.set(position).minus(block.position)
                when {
                    diff.x != 0 && diff.z != 0 -> {
                    }
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
            entity[health]?.health = 0
            entity[network]?.needsSync = true
        }
    }

    /** Gets next position, taking the direction into account. Uses tmpV as return vector.
     * @param entity The entity to apply to
     * @param delta Time since last call */
    private fun getNextPosition(entity: Entity, delta: Float): Vector3 {
        val direction = entity[direction] ?: return tmpV.set(entity[position]!!).add(tmpV2.set(entity[velocity]!!).scl(delta))

        tmpV.set(entity[position]!!)
        tmpV.y += entity[velocity]!!.y * delta * (GRAVITY / 2)

        // Following code is abridged from libGDXs built-in FirstPersonCameraController
        // (https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/graphics/g3d/utils/FirstPersonCameraController.java)
        if (entity[velocity]!!.x != 0f) {
            tmpV2.set(direction)
            tmpV2.y = 0f
            tmpV2.nor().scl(entity[velocity]!!.x * delta * entity[velocity]!!.speedModifier)
            tmpV.add(tmpV2)
        }
        if (entity[velocity]!!.z != 0f) {
            tmpV2.set(direction).crs(0f, 1f, 0f).nor().scl(entity[velocity]!!.z * delta * entity[velocity]!!.speedModifier)
            tmpV.add(tmpV2)
        }

        return tmpV
    }

    enum class BlockCollider {
        FULL, HALF_LOWER, HALF_UPPER, NONE;
    }

    private companion object {

        /** The gravity multiplier for all entities. */
        private const val GRAVITY = 8f

        private val itemSize = Vector3(0.2f, 0.2f, 0.2f)
        private val humanoidSize = Vector3(0.4f, 1.85f, 0.4f)

        private val Entity.size
            get() =
                when {
                    this[item] != null -> itemSize
                    else -> humanoidSize
                }
    }
}
