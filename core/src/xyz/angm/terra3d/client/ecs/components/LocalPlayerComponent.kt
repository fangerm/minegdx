package xyz.angm.terra3d.client.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Matrix4
import xyz.angm.terra3d.common.IntVector3

const val FOV = 75f
const val SPRINT_FOV = 85f

/** A component containing all local player state that is temporary and purely client-side.
 * @property transform The transform used with the physics engine Bullet.
 * @property The FOV of the player camera. Stored here to allow smoothly transitioning the FOV in PlayerSystem.
 * @property blockHitTime Time the player has been hitting the block they are looking at, in seconds.
 * @property blockHitPercent Block brokenness in percent.
 * @property blockLookingAt Position of the block the player is looking at. */
class LocalPlayerComponent : Component {

    val transform = Matrix4()
    var fov = FOV

    var blockHitTime = 0f
    var blockHitPercent = 0f
    var blockLookingAt = IntVector3()
        set(value) {
            if (field == value) return
            field.set(value)
            blockHitTime = 0f
            blockHitPercent = 0f
        }
}