package xyz.angm.terra3d.client.ecs.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Disposable
import xyz.angm.rox.EntitySystem
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.ecs.playerRender
import xyz.angm.terra3d.common.items.ItemType

/** A system used to render the player hand, which is rendered
 * into it's own framebuffer that is then rendered on top of the back/display buffer. */
class PlayerRenderSystem(private val screen: GameScreen) : EntitySystem(), Disposable {

    private val batch = ModelBatch()
    private val camera = PerspectiveCamera(40f, WORLD_WIDTH, WORLD_HEIGHT)
    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, WORLD_WIDTH.toInt(), WORLD_HEIGHT.toInt(), true)
    private val environment = Environment()
    private val pRender get() = screen.player[playerRender]

    private var modelBacking: ModelInstance? = null // Model inst of held item, if any
    private var modelType: ItemType = 0 // Item type of modelBacking

    private val handModel: ModelInstance
        get() {
            val heldItem = screen.playerInventory.heldItem
            heldItem ?: return pRender.hand
            if (modelType == heldItem.type) return modelBacking!!

            val model = ResourceManager.models.getItemModel(heldItem.type)
            modelBacking = ModelInstance(model)
            modelType = heldItem.type
            return modelBacking!!
        }

    init {
        camera.far = 10f
        camera.lookAt(0f, -1f, 0f)
        camera.update()

        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, 0.8f, -0.2f))
    }

    /** Regenerates the hand image to account for player actions. */
    override fun update(deltaTime: Float) {
        fbo.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        val model = handModel
        model.transform.setToTranslation(0.593f, -1.8f, -0.678f)
        model.transform.rotate(-0.982f, 0.08f, 0.368f, 222f)
        batch.begin(camera)
        batch.render(model, environment)
        batch.end()

        fbo.end()
    }

    /** The actor to be used for rendering the player hand into the
     * back buffer using Scene2D. */
    fun getActor() = Image(fbo.colorBufferTexture)

    override fun dispose() {
        batch.dispose()
        fbo.dispose()
    }
}
