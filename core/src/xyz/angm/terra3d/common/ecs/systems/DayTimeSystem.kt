package xyz.angm.terra3d.common.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.MathUtils
import ktx.ashley.allOf
import ktx.ashley.get
import xyz.angm.terra3d.common.ecs.components.specific.DayTimeComponent
import xyz.angm.terra3d.common.ecs.dayTime

/** The movement speed of the sun. */
private const val SUN_SPEED = 0.01f

/** This system is only used for the DayTime entity, which is responsible for
 * advancing and syncing day time. */
class DayTimeSystem : IteratingSystem(allOf(DayTimeComponent::class).get()) {

    override fun processEntity(entity: Entity, delta: Float) {
        val c = entity[dayTime]!!
        c.time += delta * SUN_SPEED
        if (c.time > MathUtils.PI2) c.time = 0f
    }
}
