/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/18/20, 11:01 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.ecs.systems

import xyz.angm.rox.Entity
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.rox.IteratingSystem
import xyz.angm.terra3d.common.ecs.components.RemoveFlag

/** A system that removes all entities with a [RemoveFlag].
 * ALWAYS ADD LAST TO ENSURE IT GETS EXECUTED AT THE END OF A CYCLE. */
class RemoveSystem : IteratingSystem(allOf(RemoveFlag::class), Int.MAX_VALUE) {
    /** Removes all entities with [RemoveFlag]. */
    override fun process(entity: Entity, delta: Float) = engine.remove(entity)
}