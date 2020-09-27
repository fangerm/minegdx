/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/27/20, 1:44 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata

import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.IntVector3

/** Metadata for a translocator, which is a block attached to another
 * block that pulls any items to another translocator-adjacent block.
 *
 * @property push Does this translocator push items to the other? Pull if false.
 * @property other Location of the linked translocator. Unlinked if null.
 * */
class TranslocatorMetadata : IMetadata {

    var push = true
    var other: IntVector3? = null

    override fun toString() =
        if (other == null) I18N["translocator.unlinked"]
        else "${if (push) I18N["translocator.to"] else I18N["translocator.from"]} $other"

    // This is only used to check if translocators can stack in inventories;
    // their metadata is only relevant when it's a block so it's fine
    override fun equals(other: Any?) = other is TranslocatorMetadata
}