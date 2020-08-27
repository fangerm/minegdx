package xyz.angm.terra3d.client.graphics.panels.menu

import com.badlogic.gdx.scenes.scene2d.ui.Label
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.resources.I18N

/** Panel that runs after loading a world or connecting to a server.
 * Will wait for the initial sync with the server to load the game screen.
 * Doesn't actually do anything - the client handles sync. */
class ServerSyncPanel(screen: MenuScreen) : Panel(screen) {

    private var dots = 0
    private var delta = 0f
    private val label = Label(I18N["world-loading-msg"], skin)

    init {
        clearListeners() // Prevent the user from exiting the screen
        this.add(label).pad(15f)
    }

    override fun act(delta: Float) {
        super.act(delta)
        this.delta += delta
        if (this.delta > 0.5f) {
            this.delta = 0f
            if (++dots > 3) dots = 0
            label.setText(I18N["world-loading-msg"] + ".".repeat(dots))
        }
    }
}