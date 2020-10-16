/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/16/20, 5:54 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.items.metadata.blocks

import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.metadata.IMetadata

/** Metadata for a configurator, which is used to link translocators.
 * The class itself is simply the block currently set for linking. */
class ConfiguratorMetadata : IMetadata {

    var linking = false
    var position = IntVector3()

    override fun toString() = if (linking) I18N["configurator.linking-at"] + position else I18N["configurator.click-to-start"]
}