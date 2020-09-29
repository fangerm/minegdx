/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 7:31 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.networking

/** A client socket used for communicating with a server.
 * Incoming packets should be given to the [Client] via [Client.receive],
 * disconnect is handled by [Client.disconnected]. */
abstract class ClientSocket(internal val client: Client) {

    /** Connect to the server at the specified IP. */
    abstract fun connect(ip: String)

    /** Send a packet to the server. */
    abstract fun send(packet: Any)

    /** Close the socket. */
    abstract fun close()
}
