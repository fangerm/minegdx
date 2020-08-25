package xyz.angm.terra3d.common.networking

import com.badlogic.ashley.core.Entity
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.Chunk
import java.io.Serializable

/** Interface for data sent between client and server.
 * Not all data sent is wrapped in this; only in cases where the type of the object sent is not enough context. */
interface Packet : Serializable


// Sent by client
/** Contains info about the player interacting with a block.
 * @property block The block the player interacted with
 * @property position It's position */
class PlayerBlockInteractionPacket(val block: Block = Block(), val position: IntVector3 = IntVector3()) : Packet

/** A request for the server to send chunks. See [ChunksUpdate]
 * @property position The position of the chunks requested */
class ChunkRequest(val position: IntVector3 = IntVector3()) : Packet

/** Sent when the client joins the server.
 * @property uuid The UUID of the client connecting.
 * @property uuidEntity The UUID of the entity that shall be used.
 * @property name The name of the client. Only used if the client is connecting for the first time. */
class JoinPacket(val name: String = "Player", val uuid: Int = 0, val uuidEntity: Int = 0) : Packet


// Sent by server
/** Contains chunks. Sent after a [ChunkRequest].
 * @property chunks The chunks requested */
class ChunksUpdate(val chunks: Array<Chunk> = emptyArray()) : Packet

/** Contains info of a single block change.
 * Note that this is one of the few cases where [Block.type] can be 0 (if the block was removed).
 * This can be sent by either client or server; server should echo to all clients. */
typealias BlockUpdate = Block

/** Contains all entities on the server. Sent to the client on first connect.
 * @property entities All entities on the server. */
class EntitiesPacket(val entities: Array<Entity> = emptyArray()) : Packet


// Sent by both
/** Contains a chat message. Client sends it to server; server sends it to all clients.
 * @param message The message to send */
class ChatMessagePacket(val message: String = "") : Packet
