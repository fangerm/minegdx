/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.networking

import xyz.angm.terra3d.common.world.WorldSaveManager
import xyz.angm.terra3d.server.Server
import xyz.angm.terra3d.server.ServerConfiguration

object LocalServer {

    /** The locally running server. (Static to prevent multiple trying to run on the same port) */
    private var server: Server? = null

    /** Start a local server; shut down the old one if still running */
    fun start(save: WorldSaveManager.Save) {
        server?.close()
        server = Server(save, ServerConfiguration(isLocalServer = true))
    }

    /** Shut down the currently running server, saving the world. */
    fun stop() {
        server?.close()
        server = null
    }
}
