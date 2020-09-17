package xyz.angm.terra3d.common.ecs.components

import com.badlogic.gdx.math.Vector3
import xyz.angm.rox.Component

/** A simple class for components containing a float vector.
 * @property x The first/X axis.
 * @property y The second/Y axis.
 * @property z The third/Z axis. */
abstract class VectoredComponent : Vector3(), Component {
    override fun toString() = "($x | $y | $z)"
    fun toStringFloor() = "(${x.toInt()} | ${y.toInt()} | ${z.toInt()})"
}


/** Component for all entities with an in-world position. */
class PositionComponent : VectoredComponent()


/** Component for all entities with a direction. Usually also requires a position component. */
class DirectionComponent : VectoredComponent()


/** Component for all entities with a velocity. Also requires a position component.
 * Usually, the velocity is set by some non-physics system. The physics system does not modify the velocity unless gravity is on,
 * but will not apply it when another entity would be in the way of the entity.
 * @property gravity If the entity will fall when not on solid ground. This modifies the y-coord.
 * @property speedModifier The modifier used when applying X/Z axis speed.
 * @property accelerationRate Velocity will be multiplied with this value every tick. (0-1 will result in deceleration) */
class VelocityComponent : VectoredComponent() {

    var gravity = true
    var accelerationRate = 1f
    var speedModifier = 4.5f
}
