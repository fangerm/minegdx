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
import xyz.angm.terra3d.common.ecs.components.WorldComponent
import xyz.angm.terra3d.common.ecs.modelRender
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.ecs.world

/** Listener that automatically attaches a renderable component to all entities
 * added to the engine. */
class ModelAttachListener : EntityListener {

    override fun entityAdded(entity: Entity) {
        Gdx.app.postRunnable {
            val component = ModelRenderComponent()
            component.model = ResourceManager.models.getEntityModelInstance(entity)
            entity.add(component)
        }
    }

    override fun entityRemoved(entity: Entity) {}
}

/** Automatically updates all render positions of entities using the "simple physics" system. */
class SimpleRenderSystem : IteratingSystem(allOf(ModelRenderComponent::class, PositionComponent::class).get()) {

    private val tmpV = Vector3()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[modelRender]!!.model.transform.setToTranslation(tmpV.set(entity[position]!!))
    }
}

/** Updates render positions of entities using proper Bullet physics.
 * TODO: Setting this transform is only needed whenever the entity
 * gets received over network. This could be optimized
 * (Probably not enough performance impact to justify it though.) */
class BulletRenderSystem : IteratingSystem(allOf(ModelRenderComponent::class, WorldComponent::class).get()) {

    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[modelRender]!!.model.transform = entity[world]!!
    }
}