package xyz.angm.terra3d.client.ecs.components.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import xyz.angm.terra3d.client.resources.ResourceManager

/** Renders everything related to the local player, currently only the block selector.
 * @property blockSelector The block selector model. */
class PlayerRenderComponent : RenderableComponent {

    val blockSelector: ModelInstance
    val hand: ModelInstance

    init {
        val attributes = VertexAttributes.Usage.Position.toLong() or
                VertexAttributes.Usage.Normal.toLong() or VertexAttributes.Usage.TextureCoordinates.toLong()
        val material = Material(TextureAttribute.createDiffuse(ResourceManager.get<Texture>("textures/gui/block_highlighted.png")))
        material.set(BlendingAttribute())
        val model = ModelBuilder().createBox(1.003f, 1.003f, 1.003f, material, attributes)
        blockSelector = ModelInstance(model)
        hand = ModelInstance(
            ModelBuilder().createBox(
                0.2f, 0.6f, 0.2f,
                Material(
                    ColorAttribute.createDiffuse(
                        Color(232f / 255f, 177f / 255f, 112f / 255f, 1f)
                    )
                ), attributes
            )
        )
    }

    override fun render(batch: ModelBatch, environment: Environment?) {
        batch.render(ResourceManager.models.activeDamageModel)
        batch.render(blockSelector)
    }
}