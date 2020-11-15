/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 8:50 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.ecs.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Disposable
import xyz.angm.rox.systems.EntitySystem
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.worldHeight
import xyz.angm.terra3d.client.graphics.screens.worldWidth
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.ecs.playerRender
import xyz.angm.terra3d.common.items.ItemType

/** A system used to render the player hand, which is rendered
 * into it's own framebuffer that is then rendered on top of the back/display buffer. */
class PlayerHandRenderSystem(private val screen: GameScreen) : EntitySystem(), Disposable {

    private val batch = ModelBatch()
    private val camera = PerspectiveCamera(40f, worldWidth, worldHeight)
    private var fbo = FrameBuffer(Pixmap.Format.RGBA8888, worldWidth.toInt(), worldHeight.toInt(), true)
    private val environment = Environment()
    private val pRender get() = screen.player[playerRender]
    val actor = Image(fbo.colorBufferTexture)

    private var modelBacking: ModelInstance? = null // Model inst of held item, if any
    private var modelType: ItemType = 0 // Item type of modelBacking

    private val handModel: ModelInstance
        get() {
            val heldItem = screen.playerInventory.heldItem
            heldItem ?: return pRender.hand
            if (modelType == heldItem.type) return modelBacking!!

            val model = ResourceManager.models.getItemModel(heldItem.type)
            modelBacking = ModelInstance(model)
            if (heldItem.properties.isBlock) {
                modelBacking!!.transform.setToTranslation(0.593f, -1.8f, -0.678f)
                modelBacking!!.transform.rotate(-0.982f, 0.08f, 0.368f, 222f)
            } else {
                modelBacking!!.transform.setToTranslation(0.623f, -2.2f, -0.648f)
                modelBacking!!.transform.rotate(-0.982f, 0.08f, 0.368f, -5f)
            }
            modelType = heldItem.type
            return modelBacking!!
        }

    init {
        camera.far = 10f
        camera.lookAt(0f, -1f, 0f)
        camera.update()

        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, 0.8f, -0.2f))

        pRender.hand.transform.setToTranslation(0.593f, -1.8f, -0.678f)
        pRender.hand.transform.rotate(-0.982f, 0.08f, 0.368f, 222f)
    }

    /** Regenerates the hand image to account for player actions. */
    override fun update(delta: Float) {
        fbo.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        batch.begin(camera)
        batch.render(handModel, environment)
        batch.end()
        fbo.end()
    }

    fun resize() {
        camera.viewportHeight = worldHeight
        camera.viewportWidth = worldWidth
        camera.update()
        fbo.dispose()
        fbo = FrameBuffer(Pixmap.Format.RGBA8888, worldWidth.toInt(), worldHeight.toInt(), true)
        actor.drawable = TextureRegionDrawable(TextureRegion(fbo.colorBufferTexture))
        actor.setSize(worldWidth, worldHeight)
    }

    override fun dispose() {
        batch.dispose()
        fbo.dispose()
    }
}
