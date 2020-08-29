package xyz.angm.terra3d.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.physics.bullet.Bullet
import com.kotcrab.vis.ui.VisUI
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.networking.Client
import xyz.angm.terra3d.client.networking.LocalServer
import xyz.angm.terra3d.client.resources.configuration
import xyz.angm.terra3d.client.world.RENDER_TIME_LOAD
import xyz.angm.terra3d.client.world.World
import xyz.angm.terra3d.common.networking.InitPacket
import xyz.angm.terra3d.common.networking.JoinPacket
import xyz.angm.terra3d.common.world.WorldSaveManager

/** The game itself. Only sets the screen, everything else is handled per-screen. */
class Terra3D : Game() {

    /** Called when libGDX environment is ready. */
    override fun create() {
        Bullet.init()
        VisUI.load()
        setScreen(MenuScreen(this))
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
        world.addChunks(data.world)
        (screen as MenuScreen).setWorldLoading(world)
        Gdx.app.postRunnable { updateWorld(client, world, data) }
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

    private fun startGame(client: Client, world: World, data: InitPacket) = setScreen(GameScreen(this, client, world, data.player, data.entities))
}
