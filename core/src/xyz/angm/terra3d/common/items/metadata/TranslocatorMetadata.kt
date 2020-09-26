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
}