/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server.networking

import xyz.angm.terra3d.client.networking.LocalClientSocket
import xyz.angm.terra3d.common.fst
import xyz.angm.terra3d.server.Server

/** A 'socket' for a local connection (singleplayer). Does not actually send data over the network to improve performance.
 * Use the companion object to get the socket. */
class LocalServerSocket private constructor(server: Server) : ServerSocketInterface(server) {

    override fun send(packet: Any, connection: Connection) = sendAll(packet)

    override fun sendAll(packet: Any) = LocalClientSocket.receiveAny(packet)

    override fun closeConnection(connection: Connection): Unit = LocalClientSocket.disconnect()

    override fun close() = closeConnection(localConnection)

    companion object {
        private var socket: LocalServerSocket? = null
        private val localConnection = Connection("127.0.0.1", 0)

        /** Returns the local server socket interfacing with the client. */
        fun getSocket(server: Server): LocalServerSocket {
            socket = LocalServerSocket(server)
            socket!!.connections.add(localConnection)
            return socket!!
        }

        /** Called when the client sent packet. */
        fun receiveAny(packet: Any) {
            val clone = fst.asObject(fst.asByteArray(packet))
            socket?.server?.received(localConnection, clone)
        }

        /** Called when the other socket wants to disconnect. */
        fun disconnect() {
            socket?.server?.onDisconnected(localConnection)
        }
    }
}