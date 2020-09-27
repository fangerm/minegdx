/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 8:37 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server.ecs.systems

import xyz.angm.rox.Entity
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.rox.IteratingSystem

import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent
import xyz.angm.terra3d.common.ecs.item
import xyz.angm.terra3d.common.ecs.network

/** System that updates the pickup timer of all item entities. */
class ItemSystem : IteratingSystem(allOf(ItemComponent::class)) {

    /** Update the pickup timer of the item entity. */
    override fun process(entity: Entity, delta: Float) {
        val item = entity[item]
        if (item.pickupTimeout > 0) {
            item.pickupTimeout -= delta
            entity[network].needsSync = true
        }
    }
}
