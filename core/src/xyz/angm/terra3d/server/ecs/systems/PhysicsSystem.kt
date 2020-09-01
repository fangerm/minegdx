package xyz.angm.terra3d.server.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.*
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import ktx.collections.*
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.WORLD_HEIGHT_IN_CHUNKS
import xyz.angm.terra3d.common.world.Block

/** The system for handling all player physics using Bullet! Do not touch, or else all hell will break loose.
 *
 * Note regarding class properties: All objects still needed need to be referenced somewhere. If they aren't,
 * the GC will destroy them along with the corresponding C++ Bullet object, leading to a segfault in the JNI (SIGSEGV).
 *
 * Regarding blocks: All blocks directly adjacent to the player are stored in the blocks array, which is (3 | 4 | 3) (x | y | z) big.
 * The collision objects in this array are set to the blocks around the player instead of creating new ones for every block.
 *
 * @param blockExists A function returning if a block exists at the given position */
class PhysicsSystem(private val blockExists: (IntVector3) -> Block?) : EntitySystem(), Disposable {

    private val tmpV = Vector3()
    private val tmpV2 = Vector3()
    private val tmpIV = IntVector3()
    private val tmpIV2 = IntVector3()
    private val chunkColliders = ObjectMap<IntVector3, GdxArray<btRigidBody>>(400)

    private val collisionConfig = btDefaultCollisionConfiguration()
    private val dispatcher = btCollisionDispatcher(collisionConfig)
    private val sweep = btAxisSweep3(Vector3(), Vector3(2000f, WORLD_HEIGHT_IN_CHUNKS * CHUNK_SIZE.toFloat(), 2000f))
    private val ghostPairCallback = btGhostPairCallback()
    private val constraintSolver = btSequentialImpulseConstraintSolver()
    private val world = btDiscreteDynamicsWorld(dispatcher, sweep, constraintSolver, collisionConfig)
    // private val debugDrawer = DebugDrawer()

    init {
        sweep.overlappingPairCache.setInternalGhostPairCallback(ghostPairCallback)
        world.gravity = Vector3(0f, -9.78f, 0f)
        // world.debugDrawer = debugDrawer
        // debugDrawer.debugMode = DBG_MAX_DEBUG_DRAW_MODE
    }

    fun render(batch: ModelBatch) {
        batch.flush()
        // debugDrawer.begin(batch.camera)
        world.debugDrawWorld()
        // debugDrawer.end()
    }

    /** Will update the player's position using the engine.
     * @param delta Time since last call. */
    override fun update(delta: Float) {
        world.stepSimulation(delta, 2, 1f / 60f)
    }

    /** Free all Bullet objects */
    override fun dispose() {
        world.dispose()
        chunkColliders.values().forEach { it.forEach { it.dispose() } }
        collisionConfig.dispose()
        dispatcher.dispose()
        constraintSolver.dispose()
        ghostPairCallback.dispose()
        sweep.dispose()
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
            0f, null, btBoxShape(Vector3(0.5f, 0.5f, 0.5f)), Vector3.Zero
        )

        fun createBlock(): btRigidBody {
            val body = btRigidBody(blockConstructionInfo)
            body.collisionFlags = (body.collisionFlags or btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK)
            return body
        }
    }
}