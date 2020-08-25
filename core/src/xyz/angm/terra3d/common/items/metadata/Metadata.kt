package xyz.angm.terra3d.common.items.metadata

import java.io.Serializable

/** An interface for item or block metadata.
 *
 * Metadata in the context of items/blocks is used to store arbitrary information about the item or block.
 * An example would be the durability of tools or the item slots of a furnace.
 *
 * Since many different types of items require different metadata, they can implement this interface
 * to create a metadata class that holds the required data ('data bag', should not hold any complicated behaviour).
 *
 * Additionally, any implementors should override [Object.toString] with an implementation that displays info
 * to be shown to the user in the block tooltip.
 *
 * This approach to metadata is chosen over a hash table since it is:
 * - Fast (no hashing)
 * - Memory efficient (No property keys in memory)
 * - Type-safe (Only the metadata itself needs to be cast)
 * - Metadata class makes telling the block type easier
 */
interface Metadata : Serializable
