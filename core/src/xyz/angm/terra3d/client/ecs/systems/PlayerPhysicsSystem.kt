package xyz.angm.terra3d.client.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.*
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btKinematicCharacterController
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState
import com.badlogic.gdx.utils.Disposable
import ktx.ashley.get
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.WORLD_HEIGHT_IN_CHUNKS
import xyz.angm.terra3d.common.ecs.components.set
import xyz.angm.terra3d.common.ecs.direction
import xyz.angm.terra3d.common.ecs.localPlayer
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.ecs.velocity

/** The height of the player. Multiply by 2 to get full height. */
const val PLAYER_HEIGHT = (1.85f / 2f)

/** The amount the player shrinks when sneaking. */
const val SNEAK_SIZE_MODIFIER = 0.05f

/** How much the player slows down when sneaking. */
const val SNEAK_SPEED_MODIFIER = 0.7f

/** The fall speed while sneaking. */
const val SNEAK_FALL_SPEED = 0.05f

/** Gliding start speed. */
const val GLIDE_START_SPEED = 2f

/** Maximum glide speed. */
const val GLIDE_MAX_SPEED = 10f

/** The system for handling all player physics using Bullet! Do not touch, or else all hell will break loose.
 *
 * Note regarding class properties: All objects still needed need to be referenced somewhere. If they aren't,
 * the GC will destroy them along with the corresponding C++ Bullet object, leading to a segfault in the JNI (SIGSEGV).
 *
 * Regarding blocks: All blocks directly adjacent to the player are stored in the blocks array, which is (3 | 4 | 3) (x | y | z) big.
 * The collision objects in this array are set to the blocks around the player instead of creating new ones for every block.
 *
 * @param blockExists A function returning if a block exists at the given position
 * @param player The player on the client to apply physics to.
 * @property sneaking If the player is currently sneaking. */
class PlayerPhysicsSystem(
    private val blockExists: (IntVector3) -> Boolean,
    private val player: Entity
) : EntitySystem(), Disposable {

    private val tmpV = Vector3()
    private val tmpV2 = Vector3()
    private val tmpIV = IntVector3()
    private val tmpIV2 = IntVector3()
    private val blocks = Array(3) {
        Array(4) {
            Array(3) {
                createBlock()
            }
        }
    }

    private val playerTransform = player[localPlayer]!!.transform

    private val collisionConfig = btDefaultCollisionConfiguration()
    private val dispatcher = btCollisionDispatcher(collisionConfig)
    private val sweep = btAxisSweep3(Vector3(), Vector3(2000f, WORLD_HEIGHT_IN_CHUNKS * CHUNK_SIZE.toFloat(), 2000f))
    private val ghostPairCallback = btGhostPairCallback()
    private val constraintSolver = btSequentialImpulseConstraintSolver()
    private val world = btDiscreteDynamicsWorld(dispatcher, sweep, constraintSolver, collisionConfig)

    private val playerShape = btBoxShape(Vector3(0.2f, 0.2f, PLAYER_HEIGHT))
    private val playerSneakShape = btBoxShape(Vector3(0.2f, 0.2f, PLAYER_HEIGHT - SNEAK_SIZE_MODIFIER))
    private val playerBody: btRigidBody
    private val playerGhostObj = btPairCachingGhostObject()
    private val playerController: btKinematicCharacterController
    private val jumpV = Vector3(0f, 10f, 0f)
    private val doubleJumpV = Vector3(0f, 15f, 0f)

    init {
        playerBody = createPlayerBody()
        playerController = createPlayerController()
        sweep.overlappingPairCache.setInternalGhostPairCallback(ghostPairCallback)

        world.gravity = Vector3(0f, -9.78f, 0f)

        blocks.forEach { x -> x.forEach { y -> y.forEach { world.addRigidBody(it) } } }

        playerTransform.setToTranslation(tmpV2.set(player[position]!!))
    }

    private fun createPlayerBody(): btRigidBody {
        val mass = 10f
        val inertia = Vector3()
        playerShape.calculateLocalInertia(mass, inertia)
        val playerConstructionInfo =
            btRigidBody.btRigidBodyConstructionInfo(mass, MotionState(playerTransform), playerShape, inertia)
        val playerBody = btRigidBody(playerConstructionInfo)
        world.addRigidBody(playerBody)
        playerConstructionInfo.dispose()
        return playerBody
    }

    private fun createPlayerController(): btKinematicCharacterController {
        playerGhostObj.collisionShape = playerShape
        playerGhostObj.worldTransform = playerTransform.setTranslation(tmpV.set(player[position]!!))
        playerGhostObj.collisionFlags = btCollisionObject.CollisionFlags.CF_CHARACTER_OBJECT
        val playerController = btKinematicCharacterController(playerGhostObj, playerShape, 0.55f, Vector3.Y)
        playerController.setMaxJumpHeight(1.5f)
        playerController.jumpSpeed = 1f

        world.addCollisionObject(
            playerGhostObj,
            btBroadphaseProxy.CollisionFilterGroups.CharacterFilter.toShort().toInt(),
            (btBroadphaseProxy.CollisionFilterGroups.StaticFilter or btBroadphaseProxy.CollisionFilterGroups.DefaultFilter).toShort().toInt()
        )
        world.addAction(playerController)

        return playerController
    }

    /** Will update the player's position using the engine.
     * @param delta Time since last call. */
    override fun update(delta: Float) {
        updateBlockCollisionEntities()
        playerController.setWalkDirection(getWalkDirection(delta))
        world.stepSimulation(delta, 2, 1f / 60f)
        playerGhostObj.getWorldTransform(playerTransform)
        player[position]!!.set(playerTransform.getTranslation(tmpV))
        player[position]!!.y += PLAYER_HEIGHT
    }

    private fun updateBlockCollisionEntities() {
        tmpV.set(player[position]!!).sub(0f, PLAYER_HEIGHT, 0f)
        tmpIV.set(tmpV).minus(1, 1, 1)

        blocks.forEachIndexed { x, arrayX ->
            arrayX.forEachIndexed { y, arrayY ->
                arrayY.forEachIndexed { z, block ->
                    if (!blockExists(tmpIV2.set(tmpIV).add(x, y, z))) tmpIV2.set(0, -10000, 0)
                    block.worldTransform = block.worldTransform.setToTranslation(tmpIV2.toV3(tmpV).add(0.5f, 0.5f, 0.5f))
                }
            }
        }
    }

    private var hasJumped = false

    /** Have the player jump. */
    fun jump() {
        if (playerController.canJump() && !sneaking) {
            playerController.jump(jumpV)
            hasJumped = false
        } else if (!hasJumped) {
            playerController.jump(doubleJumpV)
            hasJumped = true
        }
    }

    private var sneaking = false
    private var glideSpeed = GLIDE_START_SPEED

    /** Toggle the player sneaking. Reduces player's height and makes their fall speed very low, allowing for a 'gliding' effect */
    fun sneak(force: Boolean = !sneaking) {
        sneaking = force
        glideSpeed = GLIDE_START_SPEED
        if (sneaking) {
            player[velocity]!!.speedModifier *= SNEAK_SPEED_MODIFIER
            playerBody.collisionShape = playerSneakShape
            playerGhostObj.collisionShape = playerSneakShape
            playerController.fallSpeed = SNEAK_FALL_SPEED
        } else {
            player[velocity]!!.speedModifier /= SNEAK_SPEED_MODIFIER
            playerBody.collisionShape = playerShape
            playerGhostObj.collisionShape = playerShape
            playerController.fallSpeed = 55f // Bullet default
        }
    }

    /** Free all Bullet objects */
    override fun dispose() {
        collisionConfig.dispose()
        dispatcher.dispose()
        sweep.dispose()
        ghostPairCallback.dispose()
        constraintSolver.dispose()
        world.dispose()
        playerController.dispose()
        playerBody.dispose()
        playerGhostObj.dispose()
        playerShape.dispose()
        blocks.forEach { x -> x.forEach { y -> y.forEach { it.dispose() } } }
    }

    /** Following code is abridged from libGDXs built-in FirstPersonCameraController
     * [com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController] */
    private fun getWalkDirection(delta: Float): Vector3 {
        val velocity = player[velocity]!!
        val direction = player[direction]!!
        tmpV.set(0f, 0f, 0f)

        // Multiply speed if the player is currently sneaking in the air (gliding)
        val modifier = if (!playerController.canJump() && sneaking) {
            glideSpeed *= 1.02f
            glideSpeed = glideSpeed.coerceAtMost(GLIDE_MAX_SPEED)
            delta * glideSpeed
        } else delta

        if (velocity.x != 0f) {
            tmpV2.set(direction)
            tmpV2.y = 0f
            tmpV2.nor().scl(velocity.x * modifier * velocity.speedModifier)
            tmpV.add(tmpV2)
        }
        if (velocity.z != 0f) {
            tmpV2.set(direction).crs(0f, 1f, 0f).nor().scl(velocity.z * modifier * velocity.speedModifier)
            tmpV.add(tmpV2)
        }

        return tmpV
    }

    private class MotionState(private val transform: Matrix4) : btMotionState() {

        override fun getWorldTransform(worldTrans: Matrix4) {
            worldTrans.set(transform)
        }

        override fun setWorldTransform(worldTrans: Matrix4) {
            transform.set(worldTrans)
        }
    }

    private companion object {
        private val blockConstructionInfo = btRigidBody.btRigidBodyConstructionInfo(
            0f, null, btBoxShape(Vector3(0.52f, 0.52f, 0.52f)), Vector3.Zero
        )

        fun createBlock(): btRigidBody {
            val body = btRigidBody(blockConstructionInfo)
            body.collisionFlags = (body.collisionFlags or btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK)
            return body
        }
    }
}