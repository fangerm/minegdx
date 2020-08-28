package xyz.angm.terra3d.client.graphics.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import xyz.angm.terra3d.client.Terra3D
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.panels.PanelStack
import xyz.angm.terra3d.client.graphics.panels.menu.LoadingPanel
import xyz.angm.terra3d.client.graphics.panels.menu.MainMenuPanel
import xyz.angm.terra3d.client.graphics.panels.menu.MessagePanel
import xyz.angm.terra3d.client.graphics.panels.menu.ServerSyncPanel
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.world.WorldSaveManager
import java.io.IOException

/** The menu screen. It manages the current menu panel stack and draws it on top of a nice background.
 * @param game The game instance. */
class MenuScreen(private val game: Terra3D) : ScreenAdapter(), Screen {

    private val panoramaRotationSpeed = 4f

    private val stage = Stage(FitViewport(WORLD_WIDTH, WORLD_HEIGHT))
    private var panelStack = PanelStack()

    private var skybox = ModelInstance(Model())
    private var modelBatch = ModelBatch()
    private val cam = PerspectiveCamera(75f, WORLD_WIDTH, WORLD_HEIGHT)

    override fun show() {
        stage.addActor(panelStack)

        ResourceManager.init()
        panelStack.pushPanel(LoadingPanel(this))
        Gdx.input.inputProcessor = stage

        skybox = ModelInstance(ResourceManager.get<Model>("models/skybox.obj"))
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        cam.rotate(Vector3.Y, panoramaRotationSpeed * delta)
        cam.update()

        modelBatch.begin(cam)
        modelBatch.render(skybox)
        modelBatch.end()

        stage.act()
        stage.draw()
    }

    /** Connect to server or display error, see method in [Terra3D] */
    fun connectToServer(ip: String) {
        pushPanel(ServerSyncPanel(this))
        try {
            game.connectToServer(ip)
        } catch (e: IOException) {
            pushPanel(MessagePanel(this, I18N["multi.connect-failed"], callback = this::popPanel))
        }
    }

    /** @see [Terra3D.localServer] */
    fun localServer(save: WorldSaveManager.Save) {
        pushPanel(ServerSyncPanel(this))
        game.localServer(save)
    }

    /** Called when [ResourceManager] has finished loading. Will remove the loading screen
     * and show the main menu. */
    fun doneLoading() {
        panelStack.popPanel(-1)
        panelStack.pushPanel(MainMenuPanel(this))
    }

    override fun pushPanel(panel: Panel) = panelStack.pushPanel(panel)

    override fun popPanel() {
        if (panelStack.panelsInStack > 1) panelStack.popPanel()
    }

    override fun resize(width: Int, height: Int) = stage.viewport.update(width, height, true)

    override fun dispose() {
        stage.dispose()
        modelBatch.dispose()
        panelStack.dispose()
    }

    /** Recreates this screen. Used when resource pack changed, which requires all assets to be recreated. */
    fun reload() {
        game.screen = MenuScreen(game)
    }
}
