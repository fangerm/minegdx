/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/27/20, 12:59 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

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