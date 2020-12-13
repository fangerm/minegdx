/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 12/13/20, 9:00 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

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
import xyz.angm.terra3d.client.Terra3D
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.panels.PanelStack
import xyz.angm.terra3d.client.graphics.panels.menu.LoadingPanel
import xyz.angm.terra3d.client.graphics.panels.menu.MainMenuPanel
import xyz.angm.terra3d.client.graphics.panels.menu.MessagePanel
import xyz.angm.terra3d.client.graphics.panels.menu.ServerSyncPanel
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.client.world.World
import xyz.angm.terra3d.common.world.WorldSaveManager
import java.io.IOException

private const val PANORAMA_SPEED = 2f

/** The menu screen. It manages the current menu panel stack and draws it on top of a nice background.
 * @param game The game instance. */
class MenuScreen(private val game: Terra3D) : ScreenAdapter(), Screen {


    private val stage = Stage(viewport)
    private var panelStack = PanelStack()

    private var skybox = ModelInstance(Model())
    private var modelBatch = ModelBatch()
    private val cam = PerspectiveCamera(75f, worldWidth, worldHeight)

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

        cam.rotate(Vector3.Y, PANORAMA_SPEED * delta)
        cam.update()

        modelBatch.begin(cam)
        modelBatch.render(skybox)
        modelBatch.end()

        stage.act()
        stage.draw()
    }

    /** Connect to server or display error, see method in [Terra3D] */
    fun connectToServer(ip: String) {
        try {
            game.connectToServer(ip)
            pushPanel(ServerSyncPanel(this))
        } catch (e: IOException) {
            pushPanel(MessagePanel(this, I18N["multi.connect-failed"], callback = this::popPanel))
        }
    }

    /** @see [Terra3D.localServer] */
    fun localServer(save: WorldSaveManager.Save) {
        pushPanel(ServerSyncPanel(this))
        game.localServer(save)
    }

    /** See [Terra3D], used after the user has selected a world
     * and called after the server sent init data (which is when world meshing starts).
     * This is used to display the remaining chunks to mesh to the user. */
    fun setWorldLoading(world: World) {
        (panelStack.current as ServerSyncPanel).secondPhase(world)
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
        dispose()
        game.screen = MenuScreen(game)
    }
}
