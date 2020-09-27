/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/27/20, 1:48 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata

import java.io.Serializable

/** An interface for item or block metadata.
 *
 * Metadata in the context of items/blocks is used to store arbitrary information about the item or block.
 * An example would be the inventory of a chest or the item slots of a furnace.
 *
 * Since many different types of items require different metadata, they can implement this interface
 * to create a metadata class that holds the required data ('data bag', should not hold any complicated behaviour).
 *
 * Additionally, any implementors should override [Object.toString] with an implementation that displays info
 * to be shown to the user in the block tooltip, as well as [Object.equals] for item stacking.
 *
 * To attach metadata to a specific item type, use [DefaultMeta].
 * Implementors must have a zero-arg constructor if [DefaultMeta] is going to be used.
 *
 * This approach to metadata is chosen over a hash table since it is:
 * - Fast (no hashing)
 * - Memory efficient (No property keys in memory)
 * - Type-safe (Only the metadata itself needs to be cast)
 */
interface IMetadata : Serializable
