package xyz.angm.terra3d.common.ecs.systems

import xyz.angm.rox.Entity
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.rox.IteratingSystem
import xyz.angm.terra3d.common.ecs.components.RemoveFlag

/** A system that removes all entities with a [RemoveFlag].
 * ALWAYS ADD LAST TO ENSURE IT GETS EXECUTED AT THE END OF A CYCLE. */
class RemoveSystem : IteratingSystem(allOf(RemoveFlag::class)) {
    /** Removes all entities with [RemoveFlag]. */
    override fun process(entity: Entity, delta: Float) = engine.remove(entity)
}