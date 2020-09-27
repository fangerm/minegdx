/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server.networking

import com.badlogic.gdx.utils.Array
import xyz.angm.terra3d.server.Server

/** A socket used for communicating with clients.
 * For appropriate events, use [Server.onConnected], [Server.received] and [Server.onDisconnected].
 * @property connections A list of all active connections. See [Connection] */
abstract class ServerSocketInterface(internal val server: Server) {

    val connections = Array<Connection>()

    /** Send a packet to a client. */
    abstract fun send(packet: Any, connection: Connection)

    /** Send a packet to all clients. */
    abstract fun sendAll(packet: Any)

    /** Close the specified connection. */
    abstract fun closeConnection(connection: Connection)

    /** Close the socket. */
    abstract fun close()
}