package xyz.angm.terra3d.client.networking

/** A client socket used for communicating with a server.
 * Incoming packets should be given to the [Client] via [Client.receive],
 * disconnect is handled by [Client.disconnected]. */
abstract class ClientSocketInterface(internal val client: Client) {

    /** Connect to the server at the specified IP. */
    abstract fun connect(ip: String)

    /** Send a packet to the server. */
    abstract fun send(packet: Any)

    /** Close the socket. */
    abstract fun close()
}
