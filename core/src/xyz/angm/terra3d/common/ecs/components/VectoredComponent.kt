package xyz.angm.terra3d.common.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector3

/** A simple class for components containing a float vector.
 * @property x The first/X axis.
 * @property y The second/Y axis.
 * @property z The third/Z axis. */
abstract class VectoredComponent : Vector3(), Component {
    override fun toString() = "($x | $y | $z)"
}


/** Component for all entities with an in-world position.
 * This is not used for entities using Bullet - they use WorldComponent. */
class PositionComponent : VectoredComponent()


/** Component for all entities with a direction. Usually also requires a position component.
 * This is not used for entities using Bullet - they use WorldComponent. */
class DirectionComponent : VectoredComponent()


/** Component for all entities with a velocity. Also requires a position component.
 * Entities that use Bullet *do* usually have this component, with the velocity
 * of the last Bullet world step applied. This is to allow for easy interpolation
 * with WorldComponent on the side without the proper physics system. */
class VelocityComponent : VectoredComponent()
