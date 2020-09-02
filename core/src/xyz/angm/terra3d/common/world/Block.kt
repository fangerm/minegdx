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
 * @property metadata The blocks metadata, can contain information specific to the instance of the block
 * @property properties Properties of this item type */
class Block(
    val type: ItemType = 0,
    val position: IntVector3 = IntVector3(),
    var metadata: IMetadata? = null
) : Serializable {

    val properties get() = Item.Properties.fromType(type)

    /** Alternative constructor for constructing from an item instead of values directly. */
    constructor(item: Item, position: IntVector3) : this(item.type, position, item.metadata)

    /** Orientation of a block. */
    enum class Orientation {
        NORTH, EAST, SOUTH, WEST, UP, DOWN
    }
}
