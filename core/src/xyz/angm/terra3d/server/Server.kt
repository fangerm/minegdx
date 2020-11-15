/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 6:02 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server

import com.badlogic.gdx.utils.IntMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import ktx.collections.*
import xyz.angm.rox.Engine
import xyz.angm.rox.Entity
import xyz.angm.rox.EntityListener
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.rox.systems.EntitySystem
import xyz.angm.terra3d.common.SyncChannel
import xyz.angm.terra3d.common.TICK_RATE
import xyz.angm.terra3d.common.ecs.components.NetworkSyncComponent
import xyz.angm.terra3d.common.ecs.components.RemoveFlag
import xyz.angm.terra3d.common.ecs.components.specific.DayTimeComponent
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

    val engine = SyncChannel(Engine(), coScope)
    val world = World(this)
    private val netSystem = NetworkSystem(::sendToAll)
    private val players = IntMap<Entity>() // Key is the connection id
    private val playerFamily = allOf(PlayerComponent::class)
    private val networkedFamily = allOf(NetworkSyncComponent::class)

    init {
        schedule(2000, 1000 / TICK_RATE, coScope, ::tick)
        schedule(30000, 30000, coScope) {
            engine { world.updateLoadedChunksByPlayers(this[playerFamily]) }
        }

        engine {
            add(ItemSystem())
            add(netSystem as EntityListener)
            add(netSystem as EntitySystem)
            add(RemoveSystem())

            save.getAllEntities(this)

            // If this is the first launch, add the DayTime entity used for
            // keeping track of the time
            if (entities.isEmpty) {
                entity {
                    with<DayTimeComponent>()
                    with<NetworkSyncComponent>()
                }
            }
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
            is BlockUpdate -> world.setBlock(packet)
            is ChunkRequest -> send(connection, ChunksLine(packet.position, world.getChunkLine(packet.position)))
            is ChatMessagePacket -> sendToAll(packet)
            is JoinPacket -> registerPlayer(connection, packet)
            is Entity -> {
                engine {
                    netSystem.receive(packet)
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
            val entities = this[networkedFamily].toArray(Entity::class)
            val playerEntity = save.getPlayer(this, packet)
            players[connection.id] = playerEntity

            send(connection, InitPacket(world.seed, playerEntity, entities, world.getInitData(playerEntity[position])))
            playerEntity[network].needsSync = true // Ensure player gets synced next tick
        }
    }

    internal fun onDisconnected(connection: Connection) {
        val player = players[connection.id] ?: return
        save.savePlayer(player)
        engine { RemoveFlag.flag(this, player) }
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
