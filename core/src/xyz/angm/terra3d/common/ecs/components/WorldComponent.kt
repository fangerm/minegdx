package xyz.angm.terra3d.common.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

/** A component that is 'just' a matrix. Used for all components that are at least one of the following:
 * - Processed by the Bullet physics system
 * - Rendered in 3D space
 * Usually entities with this will not have a PositionComponent/VelocityComponent/DirectionComponent. */
class WorldComponent : Matrix4(), Component {

    /** Use this with constants in Matrix4 to allow things like world[M13]*/
    operator fun get(i: Int) = `val`[i]

    /** Returns the translation/position of this component.
     * WARNING: This is a temporary value! The value should not
     * be kept; it should only be used for temporary calculations.
     * It is however thread-safe. */
    fun pos() = getTranslation(tmpV.get())

    companion object {
        private val tmpV = ThreadLocal.withInitial { Vector3() }
    }
}