package xyz.angm.terra3d.client.ecs.components.render

import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance

/** A render component that uses a model to render itself.
 * @property model The model instance used in render calls. */
class ModelRenderComponent : RenderableComponent {
    lateinit var model: ModelInstance
    override fun render(batch: ModelBatch, environment: Environment) = batch.render(model, environment)
}