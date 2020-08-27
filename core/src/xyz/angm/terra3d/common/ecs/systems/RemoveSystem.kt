package xyz.angm.terra3d.common.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.allOf
import xyz.angm.terra3d.common.ecs.components.RemoveFlag

/** A system that removes all entities with a [RemoveFlag]. */
class RemoveSystem : IteratingSystem(allOf(RemoveFlag::class).get(), Int.MAX_VALUE) {
    /** Removes all entities with [RemoveFlag]. */
    override fun processEntity(entity: Entity, deltaTime: Float) = engine.removeEntity(entity)
}