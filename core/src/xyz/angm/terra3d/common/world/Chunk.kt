package xyz.angm.terra3d.common.world

import org.nustaq.serialization.FSTBasicObjectSerializer
import org.nustaq.serialization.FSTClazzInfo
import org.nustaq.serialization.FSTObjectInput
import org.nustaq.serialization.FSTObjectOutput
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.items.metadata.IMetadata
import xyz.angm.terra3d.server.ecs.systems.PhysicsSystem
import java.io.Serializable

/** Simple convenience mask. */
const val ALL = Int.MAX_VALUE

/** Mask for getting a block's type. */
const val TYPE = 0b00000000111111111111111111111111 // 0-24 bits

/** The orientation of the block. Index in [Block.Orientation]. */
const val ORIENTATION_MASK = 0b11100000000000000000000000000000 // 28-32

/** A chunk is a 3D array of blocks of size [CHUNK_SIZE]. It should only be used by the world itself, and not exposed to other classes.
 * @property position The chunk's origin.
 * @property blockData Array of all blocks in the chunk; XYZ. Lower 16 bits are type, higher are status bits - see above
 * @property blockMetadata Metadata for blocks. Blocks without metadata are not is this map. */
open class Chunk private constructor(
    protected val blockData: IntArray = IntArray(CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE),
    protected val blockMetadata: HashMap<IntVector3, IMetadata> = HashMap(),
    val position: IntVector3 = IntVector3()
) : Serializable {

    /** Construct an empty chunk at the given position. */
    constructor(chunkPosition: IntVector3 = IntVector3()) : this(position = chunkPosition)

    /** Construct a chunk from a preexisting chunk. */
    protected constructor(fromChunk: Chunk) : this(fromChunk.blockData, fromChunk.blockMetadata, fromChunk.position)

    @Suppress("NOTHING_TO_INLINE")
    protected inline operator fun get(x: Int, y: Int, z: Int, mask: Int) = blockData[x + (y * CHUNK_SIZE) + (z * CHUNK_SIZE * CHUNK_SIZE)] and mask

    @Suppress("NOTHING_TO_INLINE")
    protected inline operator fun set(x: Int, y: Int, z: Int, value: Int) {
        blockData[x + (y * CHUNK_SIZE) + (z * CHUNK_SIZE * CHUNK_SIZE)] = value
    }

    /** Returns the block at the specified location, or null if there is none. */
    fun getBlock(p: IntVector3): Block? {
        return if (p.isInBounds(0, CHUNK_SIZE) && this[p.x, p.y, p.z, TYPE] != 0) {
            Block(this[p.x, p.y, p.z, TYPE], p.cpy().add(position), blockMetadata[p])
        } else null
    }

    /** Returns the block's collider. Used by the physics system for meshing. */
    fun getCollider(x: Int, y: Int, z: Int) = Item.Properties.fromType(blockData[x + (y * CHUNK_SIZE) + (z * CHUNK_SIZE * CHUNK_SIZE)])?.block?.collider
        ?: PhysicsSystem.BlockCollider.NONE

    /** @return If a block at the given position exists */
    fun blockExists(p: IntVector3) = p.isInBounds(0, CHUNK_SIZE) && blockExists(p.x, p.y, p.z)

    /** @see blockExists */
    private fun blockExists(x: Int, y: Int, z: Int) = this[x, y, z, TYPE] != 0

    /** Sets the block.
     * @param position The position to place it at
     * @param block The block to place */
    fun setBlock(position: IntVector3, block: Block?) {
        setBlock(position, block?.properties?.type ?: 0)
        if (block?.metadata != null) blockMetadata[position] = block.metadata!!
    }

    /** Same as [setBlock], but constructs a new block from specified type. */
    fun setBlock(position: IntVector3, type: ItemType) {
        if (position.isInBounds(0, CHUNK_SIZE)) {
            this[position.x, position.y, position.z] = type
            blockMetadata.remove(position)
        }
    }

    /** Custom chunk serializer. It's about 3x faster than regular serialization with a chunk filled with the same block, but is 3x SLOWER than
     * the default when the chunk is generated from random noise. Since most chunks are not random, the speed is still faster on average. */
    class FSTChunkSerializer : FSTBasicObjectSerializer() {

        /** Write the chunk */
        override fun writeObject(out: FSTObjectOutput, chunk: Any, cInfo: FSTClazzInfo, fInfo: FSTClazzInfo.FSTFieldInfo, strPos: Int) {
            chunk as Chunk
            out.writeInt(chunk.position.x)
            out.writeInt(chunk.position.y)
            out.writeInt(chunk.position.z)
            out.writeObject(chunk.blockMetadata)
            writeBlockTypes(out, chunk.blockData)
        }

        /** Creates the chunk, also reads it during instance creation */
        override fun instantiate(oClass: Class<*>, input: FSTObjectInput, cInfo: FSTClazzInfo, fInfo: FSTClazzInfo.FSTFieldInfo, strPos: Int): Any {
            @Suppress("UNCHECKED_CAST")
            return Chunk(
                position = IntVector3(input.readInt(), input.readInt(), input.readInt()),
                blockMetadata = input.readObject(HashMap::class.java) as HashMap<IntVector3, IMetadata>,
                blockData = readBlockTypes(input)
            )
        }

        /** Write block types into the stream. */
        private fun writeBlockTypes(out: FSTObjectOutput, types: IntArray) {
            var chainLength: Short = 0
            var last = types[0]

            for (block in types) {
                if (block == last) chainLength++ // The chain of blocks continues
                else { // The block is different than the last, stop the chain and write it
                    out.codec.writeFShort(chainLength)
                    out.codec.writeFInt(last)
                    chainLength = 1
                }
                last = block
            }
            out.codec.writeFShort(chainLength)
            out.codec.writeFInt(last)
        }

        /** Reads block types from the stream. */
        private fun readBlockTypes(input: FSTObjectInput): IntArray {
            val out = IntArray(CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE)
            val zero: Short = 0 // Why, Kotlin...
            var left = input.codec.readFShort()
            var current = input.codec.readFInt()

            for (i in 0 until CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE) {
                if (left == zero) {
                    left = input.codec.readFShort()
                    current = input.codec.readFInt()
                }
                out[i] = current
                left--
            }
            return out
        }
    }
}