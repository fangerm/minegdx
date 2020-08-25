package xyz.angm.terra3d.client.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector3
import ktx.ashley.allOf
import ktx.ashley.get
import xyz.angm.terra3d.client.ecs.components.render.ModelRenderComponent
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.ecs.components.PositionComponent
import xyz.angm.terra3d.common.ecs.components.set
import xyz.angm.terra3d.common.ecs.modelRender
import xyz.angm.terra3d.common.ecs.position

/** A system that automatically updates the positions of all renderable components that need it.
 * Also an entity listener for adding the rendering component to new entities. */
class RenderSystem : IteratingSystem(allOf(ModelRenderComponent::class, PositionComponent::class).get()), EntityListener {

    private val tmpV = Vector3()

    /** Set the correct position of the rendering component of the entity. */
    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[modelRender]!!.model.transform.setToTranslation(tmpV.set(entity[position]!!))
    }

    /** Add the entities model. */
    override fun entityAdded(entity: Entity) {
        Gdx.app.postRunnable {
            val component = ModelRenderComponent()
            component.model = ResourceManager.models.getEntityModelInstance(entity)
            entity.add(component)
        }
    }

    override fun entityRemoved(entity: Entity) {}
}