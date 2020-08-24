package xyz.angm.terra3d.server.networking

import com.badlogic.gdx.utils.Array
import xyz.angm.terra3d.server.Server

/** A socket used for communicating with clients.
 * For approriate events, use [Server.connect], [Server.receive] and [Server.disconnect].
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