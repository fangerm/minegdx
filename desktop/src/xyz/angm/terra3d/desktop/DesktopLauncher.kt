package xyz.angm.terra3d.desktop

import ch.qos.logback.classic.Level
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import xyz.angm.terra3d.client.Terra3D
import xyz.angm.terra3d.client.resources.soundPlayer
import xyz.angm.terra3d.common.level
import xyz.angm.terra3d.common.log
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/** The LWJGL configuration used for the game */
val configuration = Lwjgl3ApplicationConfiguration()

/** The game instance */
val game = Terra3D()

/** Initialize and launch the game. */
fun main(arg: Array<String>) {
    log.level = if (arg.isNotEmpty() && arg[0] == "--debug") Level.ALL else Level.WARN
    Thread.setDefaultUncaughtExceptionHandler(::handleException)

    setConfiguration()
    soundPlayer = Sound
    Lwjgl3Application(game, configuration)
}

/** Handle exceptions */
private fun handleException(thread: Thread, throwable: Throwable) {
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
private fun showDialog(text: String, type: Int) = JOptionPane.showMessageDialog(null, text, "Terra3D", type)

/** Returns the LWJGL configuration. */
private fun setConfiguration() {
    configuration.setIdleFPS(15)
    configuration.useVsync(true)
    configuration.setTitle("Terra3D")
}