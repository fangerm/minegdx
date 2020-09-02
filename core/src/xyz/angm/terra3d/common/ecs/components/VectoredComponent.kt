package xyz.angm.terra3d.common.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector3

/** A simple class for components containing a float vector.
 * @property x The first/X axis.
 * @property y The second/Y axis.
 * @property z The third/Z axis. */
abstract class VectoredComponent : Component {

    var x = 0f
    var y = 0f
    var z = 0f

    /** Sets itself from a libGDX vector.
     * @param v The vector to set itself to.
     * @return Itself for chaining. */
    fun set(v: Vector3): VectoredComponent {
        x = v.x
        y = v.y
        z = v.z
        return this
    }

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
 * @property speedModifier The modifier used when applying X/Z axis speed. */
class VelocityComponent : VectoredComponent() {

    var gravity = true
    var speedModifier = 4.5f
}


/** Component for all entities with a size. Each axis represents the width on that axis.
 * Mainly used by physics systems.
 * Entities without this will often be represented by a (0|0|0) size. */
class SizeComponent : VectoredComponent()


/** Simple helper to apply a vectored component to a libGDX vector. */
fun Vector3.set(v: VectoredComponent): Vector3 {
    x = v.x
    y = v.y
    z = v.z
    return this
}
