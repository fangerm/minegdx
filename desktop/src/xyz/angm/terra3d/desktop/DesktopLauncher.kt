package xyz.angm.terra3d.desktop

import ch.qos.logback.classic.Level
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl.LwjglGraphics
import xyz.angm.terra3d.client.Terra3D
import xyz.angm.terra3d.client.resources.soundPlayer
import xyz.angm.terra3d.common.level
import xyz.angm.terra3d.common.log
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/** The LWJGL configuration used for the game */
val configuration = LwjglApplicationConfiguration()

/** The game instance */
val game = Terra3D()

/** Initialize and launch the game. */
fun main(arg: Array<String>) {
    log.level = if (arg.isNotEmpty() && arg[0] == "--debug") Level.ALL else Level.WARN
    Thread.setDefaultUncaughtExceptionHandler(::handleException)

    setConfiguration()
    LwjglApplication(game, configuration)
    showWarnings()
    soundPlayer = Sound
}

/** Handle exceptions */
private fun handleException(thread: Thread, throwable: Throwable) {
    configuration.forceExit = false // Setting this during boot would cause the game to not exit when closing normally as well
    Gdx.app?.exit()

    log.error { "Whoops. This shouldn't have happened..." }
    log.error(throwable) { "Exception in thread ${thread.name}:\n" }
    log.error { "Client is exiting." }

    val builder = StringBuilder()
    builder.append("The game encountered an exception, and is forced to close.\n")
    builder.append("Exception: ${throwable.javaClass.name}: ${throwable.localizedMessage}\n")
    builder.append("For more information, see the console output or log.")

    showDialog(builder.toString(), JOptionPane.ERROR_MESSAGE)
    exitProcess(-1)
}

/** Simple method for showing a dialog. Type should be a type from JOptionPane */
private fun showDialog(text: String, type: Int) = JOptionPane.showMessageDialog(null, text, "MineGDX", type)

/** Shows any warnings related to the user's system. */
private fun showWarnings() {
    if ((Runtime.getRuntime().maxMemory() / 1024 / 1024) < 2500)
        showDialog("Your device does not have 2.5GB free RAM!\nExpect the game to crash.", JOptionPane.ERROR_MESSAGE)

    if ((Gdx.graphics as LwjglGraphics).isSoftwareMode)
        showDialog("Your device does not support OpenGL!\nFalling back to software render. Expect bad performance.", JOptionPane.WARNING_MESSAGE)
}

/** Returns the LWJGL configuration. */
private fun setConfiguration() {
    configuration.backgroundFPS = 15
    configuration.vSyncEnabled = false
    configuration.allowSoftwareMode = true
    configuration.title = "MineGDX"
}