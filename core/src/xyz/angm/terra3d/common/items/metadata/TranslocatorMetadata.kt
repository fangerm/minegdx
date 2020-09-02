package xyz.angm.terra3d.common.items.metadata

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
        if (other == null) "Unlinked!\nUse a Configurator to link."
        else "${if (push) "Pushing to" else "Pulling from"} $other"
}