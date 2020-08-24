package xyz.angm.terra3d.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.physics.bullet.Bullet
import com.kotcrab.vis.ui.VisUI
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.networking.Client
import xyz.angm.terra3d.client.networking.startLocalServer
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
    fun connectToServer(ip: String) {
        screen = GameScreen(this, Client(ip))
    }

    /** Creates a local server and switches to the game screen.
     * @param world The world to initialize the server with. */
    fun localServer(world: WorldSaveManager.Save) {
        startLocalServer(world)
        screen = GameScreen(this, Client())
    }
}
