package xyz.angm.terra3d.client.graphics.panels.menu

import com.badlogic.gdx.scenes.scene2d.ui.Label
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.world.World

/** Panel that runs after loading a world or connecting to a server.
 * Will wait for the initial sync with the server to load the game screen.
 * Doesn't actually do anything - the client handles sync. */
class ServerSyncPanel(screen: MenuScreen) : Panel(screen) {

    private var dots = 0
    private var delta = 0f
    private val loadingMsg = Label(I18N["client-loading-msg"], skin)
    private val chunksLeft = Label("", skin)
    private var world: World? = null

    init {
        clearListeners() // Prevent the user from exiting the screen
        this.add(loadingMsg).pad(15f).row()
        this.add(chunksLeft).pad(15f)
    }

    override fun act(delta: Float) {
        super.act(delta)

        // Update dots
        this.delta += delta
        if (this.delta > 0.5f) {
            this.delta = 0f
            if (++dots > 3) dots = 0
        }

        // Update label
        if (world != null) {
            loadingMsg.setText(I18N["world-meshing-msg"] + ".".repeat(dots))
            chunksLeft.setText("${world!!.waitingForRender} ${I18N["world-meshing-remaining"]}")
        } else {
            loadingMsg.setText(I18N["client-loading-msg"] + ".".repeat(dots))
        }

    }

    /** See [MenuScreen.setWorldLoading] and [Terra3D], called when
     * server is connected and world will start meshing. */
    fun secondPhase(world: World) {
        this.world = world
    }
}