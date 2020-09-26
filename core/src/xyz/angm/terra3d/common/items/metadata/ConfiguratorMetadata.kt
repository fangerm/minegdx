package xyz.angm.terra3d.common.items.metadata

import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.IntVector3

/** Metadata for a configurator, which is used to link translocators.
 * The class itself is simply the block currently set for linking. */
class ConfiguratorMetadata : IMetadata {

    var linking = false
    var position = IntVector3()

    override fun toString() = if (linking) I18N["configurator.linking-at"] + position else I18N["configurator.click-to-start"]
}