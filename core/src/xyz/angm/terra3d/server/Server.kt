package xyz.angm.terra3d.server

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.IntMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import ktx.ashley.allOf
import ktx.ashley.get
import ktx.collections.*
import xyz.angm.terra3d.common.TICK_RATE
import xyz.angm.terra3d.common.ecs.EntityData
import xyz.angm.terra3d.common.ecs.components.NetworkSyncComponent
import xyz.angm.terra3d.common.ecs.components.RemoveFlag
import xyz.angm.terra3d.common.ecs.components.specific.PlayerComponent
import xyz.angm.terra3d.common.ecs.network
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.ecs.systems.NetworkSystem
import xyz.angm.terra3d.common.ecs.systems.RemoveSystem
import xyz.angm.terra3d.common.log
import xyz.angm.terra3d.common.networking.*
import xyz.angm.terra3d.common.schedule
import xyz.angm.terra3d.common.world.WorldSaveManager
import xyz.angm.terra3d.server.ecs.systems.ItemSystem
import xyz.angm.terra3d.server.networking.Connection
import xyz.angm.terra3d.server.networking.LocalServerSocket
import xyz.angm.terra3d.server.networking.NettyServerSocket
import xyz.angm.terra3d.server.world.World


/** A server, handles the world and interacts with clients.
 * @property save The save to use.
 * @property configuration The server configuration to use.
 * @property world The world being used
 * @property engine Manager for all types of entities */
class Server(
    val save: WorldSaveManager.Save,
    private val configuration: ServerConfiguration = ServerConfiguration()
) {

    private val serverSocket = if (configuration.isLocalServer) LocalServerSocket.getSocket(this) else NettyServerSocket(this)
    internal val coScope = CoroutineScope(Dispatchers.Default)

    val engine = ConcurrentEngine(coScope)
    val world = World(this)
    private val players = IntMap<Entity>() // Key is the connection id
    private val playerFamily = allOf(PlayerComponent::class).get()
    private val networkedFamily = allOf(NetworkSyncComponent::class).get()

    init {
        schedule(2000, 1000 / TICK_RATE, coScope, ::tick)
        schedule(30000, 30000, coScope) {
            engine { world.updateLoadedChunksByPlayers(getEntitiesFor(playerFamily)) }
        }

        engine {
            addSystem(ItemSystem())
            addSystem(RemoveSystem())
            val netSystem = NetworkSystem(::sendToAll)
            addEntityListener(allOf(NetworkSyncComponent::class).get(), netSystem)
            addSystem(netSystem)
            save.getAllEntities(this)
        }

        // Executed on SIGTERM
        Runtime.getRuntime().addShutdownHook(Thread { close() })
    }

    private fun send(connection: Connection, packet: Packet) {
        serverSocket.send(packet, connection)
        log.debug { "[SERVER] Sent packet of class ${packet.javaClass.name} to ${connection.ip}" }
    }

    /** Send a packet to all connected clients. */
    fun sendToAll(packet: Any) {
        serverSocket.sendAll(packet)
        log.debug { "[SERVER] Sent packet of class ${packet.javaClass.name} to all" }
    }

    internal fun received(connection: Connection, packet: Any) {
        log.debug { "[SERVER] Received object of class ${packet.javaClass.name}" }

        when (packet) {
            is BlockUpdate -> world.setBlock(packet.position, packet)
            is ChunkRequest -> send(connection, ChunksLine(packet.position, world.getChunkLine(packet.position)))
            is ChatMessagePacket -> sendToAll(packet)
            is JoinPacket -> registerPlayer(connection, packet)
            is EntityData -> {
                engine {
                    getSystem(NetworkSystem::class.java).receive(packet)
                    sendToAll(packet) // Ensure it syncs to all players
                }
            }
        }
    }

    internal fun onConnected(connection: Connection) {
        log.info { "[SERVER] Player connected. IP: ${connection.ip}. Sending server info..." }
        send(connection, ServerInfo(configuration.maxPlayers, serverSocket.connections.size - 1, configuration.motd))

        if (serverSocket.connections.size > configuration.maxPlayers) {
            serverSocket.closeConnection(connection)
            log.info { "[SERVER] Player disconnected: Server is full!" }
        }
    }

    private fun registerPlayer(connection: Connection, packet: JoinPacket) {
        engine {
            val entities = EntityData.from(getEntitiesFor(networkedFamily))
            val playerEntity = save.getPlayer(this, packet)
            players[connection.id] = playerEntity

            send(connection, InitPacket(EntityData.from(playerEntity), entities, world.getInitData(playerEntity[position]!!), world.seed))
            playerEntity[network]!!.needsSync = true // Ensure player gets synced next tick
        }
    }

    internal fun onDisconnected(connection: Connection) {
        val player = players[connection.id] ?: return
        save.savePlayer(player)
        RemoveFlag.flag(player)
        log.info { "[SERVER] Disconnected from connection id ${connection.id}." }
    }

    /** Perform a tick, stepping the world forward. */
    private fun tick() = engine { update(1f / TICK_RATE) }

    /** Close the server. Will save world and close all connections, making the object unusable. */
    fun close() {
        log.info { "[SERVER] Shutting down..." }
        serverSocket.close()
        world.close()
        engine { save.saveAllEntities(this) }
        coScope.cancel()
    }
}
