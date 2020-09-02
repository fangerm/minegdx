package xyz.angm.terra3d.common.items.metadata

import xyz.angm.terra3d.common.IntVector3

/** Metadata for a configurator, which is used to link translocators.
 * The class itself is simply the block currently set for linking. */
class ConfiguratorMetadata : IMetadata, IntVector3() {
    override fun toString() = "Linking block at ${super.toString()}"
}