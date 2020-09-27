/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/19/20, 2:22 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.physics.bullet.Bullet
import com.kotcrab.vis.ui.VisUI
import ktx.collections.*
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.networking.Client
import xyz.angm.terra3d.client.networking.LocalServer
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.client.resources.configuration
import xyz.angm.terra3d.client.world.RENDER_TIME_LOAD
import xyz.angm.terra3d.client.world.World
import xyz.angm.terra3d.common.networking.InitPacket
import xyz.angm.terra3d.common.networking.JoinPacket
import xyz.angm.terra3d.common.world.WorldSaveManager
import kotlin.system.exitProcess

/** If the game should immediately load the first world in singleplayer as
 * fast as possible.
 * Used for debugging with the --quicklaunch argument. */
var quickLaunch = false

/** The game itself. Only sets the screen, everything else is handled per-screen. */
class Terra3D : Game() {

    /** Called when libGDX environment is ready. */
    override fun create() {
        Bullet.init()
        VisUI.load()
        if (quickLaunch) quickLaunch()
        else setScreen(MenuScreen(this))
    }

    /** See [quickLaunch] */
    private fun quickLaunch() {
        ResourceManager.init()
        ResourceManager.finishLoading()
        localServer(WorldSaveManager.getWorlds().first())
    }

    /** Connects to a server and switches to the game screen.
     * @param ip IP of the server to connect to. */
    fun connectToServer(ip: String) = collectWorld(Client(ip))

    /** Creates a local server and switches to the game screen.
     * @param world The world to initialize the server with. */
    fun localServer(world: WorldSaveManager.Save) {
        // Run in a second thread - booting the server takes 1-2s and would
        // cause the main thread to hang
        Thread {
            LocalServer.start(world)
            collectWorld(Client())
        }.start()
    }

    private fun collectWorld(client: Client) {
        client.addListener { initWorld(client, it as? InitPacket ?: return@addListener) }
        client.send(JoinPacket(configuration.playerName, configuration.clientUUID))
    }

    private fun initWorld(client: Client, data: InitPacket) {
        client.clearListeners() // Remove the collectWorld listener
        val world = World(client, data.seed)
        world.addChunks(data.world, false)

        if (quickLaunch) Gdx.app.postRunnable { startGame(client, world, data) }
        else {
            (screen as MenuScreen).setWorldLoading(world)
            Gdx.app.postRunnable { updateWorld(client, world, data) }
        }
    }

    private fun updateWorld(client: Client, world: World, data: InitPacket) {
        Gdx.app.postRunnable {
            world.update(RENDER_TIME_LOAD)
            // Start the game if there is nothing left to mesh
            if (world.waitingForRender == 0) startGame(client, world, data)
            // Make it call itself to cause it to run every frame
            else updateWorld(client, world, data)
        }
    }

    private fun startGame(client: Client, world: World, data: InitPacket) =
        setScreen(GameScreen(this, client, world, data.player!!, data.entities))

    override fun dispose() = exitProcess(0)

    companion object {

        private val runnables = GdxArray<() -> Unit>(10)

        /** Post a runnable to be run on the main thread on the next frame.
         * This is a replacement for `Gdx.app.postRunnable` provided by
         * libGDX, which cannot be used as it does not allow for running code
         * each frame before the runnables - which is needed to lock the client
         * and prevent race conditions.
         * Only works while in-game - MenuScreen does not process this!
         * For menus and other things that might be called while not
         * in-game you should simply use the libGDX provided method. */
        fun postRunnable(runnable: () -> Unit) = runnables.add(runnable)

        /** Called once per frame by [GameScreen], executes all runnables. */
        fun execRunnables() {
            runnables.forEach { it() }
            runnables.clear()
        }
    }
}
