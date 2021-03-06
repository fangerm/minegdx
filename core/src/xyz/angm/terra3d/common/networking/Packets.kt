/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/19/20, 12:56 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.networking

import xyz.angm.rox.Entity
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.Chunk
import java.io.Serializable

/** Interface for data sent between client and server.
 * Not all data sent is wrapped in this; only in cases where the type of the object sent is not enough context. */
interface Packet : Serializable


/** A request for the server to send chunks. See [ChunksLine]
 * @property position The position of the chunks requested */
class ChunkRequest(val position: IntVector3 = IntVector3()) : Packet


/** Sent when the client joins the server.
 * @property uuid The UUID of the client connecting.
 * @property name The name of the client. Only used if the client is connecting for the first time. */
class JoinPacket(val name: String = "Player", val uuid: Int = 0) : Packet


/** Contains chunks. Sent after a [ChunkRequest]. Only contains chunks that were
 * changed by players since world generation.
 * @property position The position of the line
 * @property chunks The chunks requested */
class ChunksLine(val position: IntVector3, val chunks: Array<Chunk> = emptyArray()) : Packet


/** Contains info of a single block change.
 * Note that this is one of the few cases where [Block.type] can be 0 (if the block was removed).
 * This can be sent by either client or server; server should echo to all clients. */
typealias BlockUpdate = Block


/** A packet sent on first connect as a response to [JoinPacket].
 * Contains all data required by the client to begin init and world loading. */
class InitPacket(
    val seed: String,
    val player: Entity? = null, // never actually null, just there to allow empty constructor
    val entities: Array<Entity> = emptyArray(),
    val world: Array<Chunk> = emptyArray(),
) : Packet


/** Contains a chat message. Client sends it to server; server sends it to all clients.
 * @param message The message to send */
class ChatMessagePacket(val message: String = "") : Packet


/** Packet containing info about the server. Sent by the server
 * and shown by the client in the multiplayer select. */
class ServerInfo(
    val maxPlayers: Int = 0,
    val onlinePlayers: Int = 0,
    val motd: String = ""
) : Packet