package xyz.angm.terra3d.common.networking

import java.io.Serializable

/** Interface for data sent between client and server.
 * Not all data sent is wrapped in this; only in cases where the type of the object sent is not enough context. */
interface Packet : Serializable

/*
// Sent by client
/** Contains info about the player interacting with a block.
 * @property block The block the player interacted with
 * @property position It's position */
class PlayerBlockInteractionPacket(val block: Block? = null, val position: IntVector3 = IntVector3()) : Packet

/** A request for the server to send chunks. See [ChunksPacket]
 * @property position The position of the chunks requested */
class ChunkRequest(val position: IntVector3 = IntVector3()) : Packet

/** Sent when the client joins the server.
 * @property uuid The UUID of the client connecting.
 * @property uuidEntity The UUID of the entity that shall be used.
 * @property name The name of the client. Only used if the client is connecting for the first time. */
class JoinPacket(val name: String = "Player", val uuid: Long = 0L, val uuidEntity: Long = 0L) : Packet


// Sent by server
/** Contains chunks. Sent after a [ChunkRequest].
 * @property chunks The chunks requested */
class ChunksPacket(val chunks: Array<Chunk> = emptyArray()) : Packet

/** Contains info of a single block change. See [PlayerBlockInteractionPacket]
 * @property block The block that changed
 * @property blockPosition It's position */
class BlockPacket(val blockPosition: IntVector3 = IntVector3(), val block: Block? = Block()) : Packet

/** Contains all entities on the server. Sent to the client on first connect.
 * @property entities All entities on the server. */
class EntitiesPacket(val entities: Array<Entity> = emptyArray()) : Packet


// Sent by both
/** Contains a chat message. Client sends it to server; server sends it to all clients.
 * @param message The message to send */
class ChatMessagePacket(val message: String = "") : Packet
*/