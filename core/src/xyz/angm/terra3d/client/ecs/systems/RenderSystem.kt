package xyz.angm.terra3d.client.ecs.systems

import xyz.angm.rox.Entity
import xyz.angm.rox.EntityListener
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.rox.IteratingSystem
import xyz.angm.terra3d.client.Terra3D
import xyz.angm.terra3d.client.ecs.components.render.ModelRenderComponent
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.ecs.components.PositionComponent
import xyz.angm.terra3d.common.ecs.dayTime
import xyz.angm.terra3d.common.ecs.modelRender
import xyz.angm.terra3d.common.ecs.position

/** A system that automatically updates the positions of all renderable components that need it.
 * Also an entity listener for adding the rendering component to new entities. */
class RenderSystem : IteratingSystem(allOf(ModelRenderComponent::class, PositionComponent::class)), EntityListener {

    /** Set the correct position of the rendering component of the entity. */
    override fun process(entity: Entity, delta: Float) {
        entity[modelRender].model.transform.setToTranslation(entity[position])
    }

    /** Add the entities model. */
    override fun entityAdded(entity: Entity) {
        if (hasModel(entity)) {
            Terra3D.postRunnable {
                val component = ModelRenderComponent()
                component.model = ResourceManager.models.getEntityModelInstance(entity)
                entity.add(engine, component)
            }
        }
    }

    override fun entityRemoved(entity: Entity) {}

    companion object {
        /** If this entity has a model that needs rendering. */
        private fun hasModel(e: Entity) = !e.has(dayTime) // DayTime entity currently only one without model
    }
}