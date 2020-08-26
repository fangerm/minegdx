package xyz.angm.terra3d.server

import ch.qos.logback.classic.Level
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import ktx.assets.file
import ktx.assets.toInternalFile
import ktx.assets.toLocalFile
import xyz.angm.terra3d.common.level
import xyz.angm.terra3d.common.log
import xyz.angm.terra3d.common.world.WorldSaveManager
import xyz.angm.terra3d.common.yaml
import kotlin.system.exitProcess

private const val CONFIG_LOCATION = "server-configuration.yaml"

/** To be used with HeadlessApplication for creating a standalone server */
class ServerLauncher : ApplicationAdapter() {

    /** Called on application creation */
    override fun create() {
        // Create server config if it is missing (should only occur on first boot)
        val serverConfigFile = CONFIG_LOCATION.toLocalFile()
        if (!serverConfigFile.exists()) serverConfigFile.writeBytes(CONFIG_LOCATION.toInternalFile().readBytes(), false)

        val world = WorldSaveManager.addWorld("world", System.currentTimeMillis().toString())
        Server(world, yaml.decodeFromString(ServerConfiguration.serializer(), file(CONFIG_LOCATION).readString()))
    }
}

/** Starts a server on the current thread. */
fun main(arg: Array<String>) {
    log.level = if (arg.isNotEmpty() && arg[0] == "--debug") Level.ALL else Level.INFO
    Thread.setDefaultUncaughtExceptionHandler(::handleException)
    HeadlessApplication(ServerLauncher())
}

/** Handle exceptions */
private fun handleException(thread: Thread, throwable: Throwable) {
    Gdx.app?.exit()
    log.error { "Whoops. This shouldn't have happened..." }
    log.error(throwable) { "Exception in thread ${thread.name}:\n" }
    log.error { "Server is shutting down." }
    exitProcess(-1)
}
