package xyz.angm.terra3d.server.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.allOf
import ktx.ashley.get
import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent
import xyz.angm.terra3d.common.ecs.item
import xyz.angm.terra3d.common.ecs.network

/** System that updates the pickup timer of all item entities. */
class ItemSystem : IteratingSystem(allOf(ItemComponent::class).get()) {

    /** Update the pickup timer of the item entity. */
    override fun processEntity(entity: Entity, delta: Float) {
        val item = entity[item]!!
        if (item.pickupTimeout > 0) {
            item.pickupTimeout -= delta
            entity[network]!!.needsSync = true
        }
    }
}
