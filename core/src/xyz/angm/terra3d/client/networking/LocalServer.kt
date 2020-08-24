package xyz.angm.terra3d.client.networking

import xyz.angm.terra3d.common.world.WorldSaveManager
import xyz.angm.terra3d.server.Server

/** The locally running server. (Static to prevent multiple trying to run on the same port) */
private var server: Server? = null

/** Start a local server; shut down the old one if still running */
fun startLocalServer(save: WorldSaveManager.Save) {
    server?.close()
    // server = Server(save, ServerConfiguration(isLocalServer = true))
}

/** Shut down the currently running server, saving the world. */
fun stopLocalServer() {
    server?.close()
    server = null
}
