/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/16/20, 7:01 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.world

import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.items.metadata.IMetadata
import java.io.Serializable

/** A simple constant for 'air' blocks and no items to make code slightly easier
 * to understand. */
const val NOTHING = 0

/** Represents a block.
 * @property type Block/Item type, can be 0 ('air' block) in some cases
 * @property position Position of the block; origin is the world origin
 * @property orientation The orientation of the block. Not all blocks can be rotated.
 * @property metadata The blocks metadata, can contain information specific to the instance of the block
 * @property fluidLevel The fluid level, if this block is a fluid.
 * @property properties Properties of this item type */
data class Block(
    val type: ItemType = 0,
    val position: IntVector3 = IntVector3(),
    var metadata: IMetadata? = null,
    var orientation: Orientation = Orientation.UP,
    var fluidLevel: Int = 0
) : Serializable {

    val properties get() = Item.Properties.fromType(type)

    /** Alternative constructor for constructing from an item instead of values directly. */
    constructor(item: Item, position: IntVector3, orientation: Orientation) : this(item.type, position, item.metadata, orientation)

    constructor(type: ItemType, position: IntVector3, metadata: IMetadata?, orientation: Int, fluidLevel: Int)
            : this(type, position, metadata, Orientation.fromId(orientation), fluidLevel)

    init {
        orientation = properties?.block?.orientation?.get(orientation) ?: orientation
    }

    /** Returns the raw data of this block to be used for [Chunk.setBlock(IntVector3, ItemType)]. */
    fun toRaw() = type or (orientation.toId() shl ORIENTATION_SHIFT) or (fluidLevel shl FLUID_LEVEL_SHIFT)

    /** Orientation of a block. */
    enum class Orientation {
        NORTH, UP, EAST, SOUTH, DOWN, WEST;

        fun toId() = list.indexOf(this)

        /** Takes a position of a block and returns the position
         * of the block on this side of it.
         * Returns the vector given. */
        fun applyIV(iv: IntVector3): IntVector3 {
            when (this) {
                NORTH -> iv.x++
                SOUTH -> iv.x--
                EAST -> iv.z++
                WEST -> iv.z--
                UP -> iv.y++
                DOWN -> iv.y--
            }
            return iv
        }

        /** Same as [applyIV] but returns block on the opposite side.
         * TODO: deduplicate. */
        fun applyIVInv(iv: IntVector3): IntVector3 {
            when (this) {
                NORTH -> iv.x--
                SOUTH -> iv.x++
                EAST -> iv.z--
                WEST -> iv.z++
                UP -> iv.y--
                DOWN -> iv.y++
            }
            return iv
        }

        companion object {
            private val list = values()

            fun fromId(id: Int) = list[id]

            /** Returns if this face is the block's front face.
             * @param faceId Face ID as used by RenderableChunk.
             * @param blockDat Block int data inside the chunk. */
            fun isFront(faceId: Int, blockDat: Int): Boolean {
                val orient = (blockDat and ORIENTATION) shr ORIENTATION_SHIFT
                return orient == faceId // This works because orientations
                // are in the same order as block faces in RenderableChunk
            }
        }
    }
}
