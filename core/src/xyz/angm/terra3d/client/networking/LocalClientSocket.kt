/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 7:31 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.networking

import xyz.angm.terra3d.common.fst
import xyz.angm.terra3d.server.networking.LocalServerSocket

/** A socket for a server running locally (singleplayer). Does not send data over network to improve performance.
 * Use the companion object to get the socket. */
class LocalClientSocket private constructor(client: Client) : ClientSocket(client) {

    override fun connect(ip: String) = Unit

    override fun send(packet: Any) = LocalServerSocket.receiveAny(packet)

    override fun close() = LocalServerSocket.disconnect()

    companion object {
        private var socket: LocalClientSocket? = null

        /** Returns the local socket 'connected' to the server. */
        fun getSocket(client: Client): LocalClientSocket {
            socket = LocalClientSocket(client)
            return socket!!
        }

        /** Called when the server sent a packet. */
        fun receiveAny(packet: Any) {
            val clone = fst.asObject(fst.asByteArray(packet))
            socket?.client?.receive(clone)
        }

        /** Called when the other socket wants to disconnect. */
        fun disconnect() {
            socket?.client?.disconnected()
        }
    }
}