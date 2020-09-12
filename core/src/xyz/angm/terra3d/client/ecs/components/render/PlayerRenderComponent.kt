package xyz.angm.terra3d.client.ecs.components.render

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import xyz.angm.terra3d.client.resources.ResourceManager

/** Renders everything related to the local player, like the skybox or the block selector.
 * @property skybox The skybox model.
 * @property blockSelector The block selector model. */
class PlayerRenderComponent : RenderableComponent {

    val blockSelector: ModelInstance

    init {
        val attributes = VertexAttributes.Usage.Position.toLong() or
                VertexAttributes.Usage.Normal.toLong() or VertexAttributes.Usage.TextureCoordinates.toLong()
        val material = Material(TextureAttribute.createDiffuse(ResourceManager.get<Texture>("textures/gui/block_highlighted.png")))
        material.set(BlendingAttribute())
        val model = ModelBuilder().createBox(1.003f, 1.003f, 1.003f, material, attributes)
        blockSelector = ModelInstance(model)
    }

    override fun render(batch: ModelBatch, environment: Environment?) {
        batch.render(ResourceManager.models.activeDamageModel)
        batch.render(blockSelector)
    }
}