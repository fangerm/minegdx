package xyz.angm.terra3d.client.ecs.components.render

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch

/** An interface for a component that can render itself into the 3D game world.
 * Note that all components that implement this interface should NOT be transferred over network when serializing an entity containing it.
 * To ensure this, add them to [xyz.angm.terra3d.common.ecs.EntityData.ignore]. */
interface RenderableComponent : Component {

    /** Called when the component should render itself.
     * @param batch A model batch with begin() already called.
     * @param environment The environment that should be used. */
    fun render(batch: ModelBatch, environment: Environment?)
}