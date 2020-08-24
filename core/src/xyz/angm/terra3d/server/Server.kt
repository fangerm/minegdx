package xyz.angm.terra3d.server

import xyz.angm.terra3d.common.log
import xyz.angm.terra3d.server.networking.Connection
import xyz.angm.terra3d.server.networking.LocalServerSocket
import xyz.angm.terra3d.server.networking.NettyServerSocket

class Server(private val configuration: ServerConfiguration = ServerConfiguration()) {

    private val socket = if (configuration.isLocalServer) LocalServerSocket.getSocket(this) else NettyServerSocket(this)

    internal fun connect(connection: Connection) {
        log.info { "[SERVER] Player connected. IP: ${connection.ip}" }
        if (socket.connections.size > configuration.maxPlayers) {
            socket.closeConnection(connection)
            log.info { "[SERVER] Player disconnected: Server is full!" }
        }
    }

    internal fun received(connection: Connection, packet: Any) {
        log.debug { "[SERVER] Received object of class ${packet.javaClass.name}" }
    }

    internal fun disconnect(connection: Connection) {
        log.info { "[SERVER] Disconnected from connection id ${connection.id}." }
    }

    fun close() {
        socket.close()
    }
}