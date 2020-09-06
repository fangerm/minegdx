package xyz.angm.terra3d.server

import kotlinx.serialization.Serializable

/** A server configuration. Controls various aspects of the server.
 * The default values are the ones used by a server running in singleplayer.
 *
 * If the server is running on a singleplayer world, it's configuration has to be changed at compile time.
 * Defaults in this file are used, with the exception of [isLocalServer].
 * If it is running standalone, the configuration file will be created on first run and may be modified by the server operator.
 * Defaults are the ones in this file.
 *
 * TODO: This used to be a simple data class, but this for some reason causes an internal compiler error.
 *
 * @property isLocalServer If the server is running in singleplayer mode. Will cause it to communicate with the client without networking.
 * @property maxPlayers The maximum amount of players allowed to join the server.
 * @property motd The MOTD shown to users on the multiplayer tab. */
@Serializable
class ServerConfiguration() {
    var isLocalServer = false
    val maxPlayers = if (isLocalServer) 1 else 5
    val motd = "A Terra3D Server"

    constructor(isLocalServer: Boolean) : this() {
        this.isLocalServer = isLocalServer
    }
}