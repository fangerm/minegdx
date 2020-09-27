/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/19/20, 12:27 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.rox

import java.util.*

/** A system inside the ECS, acting on entities every update cycle.
 * Add your systems with [Engine.add].
 * If you want to act only on certain entities, use [IteratingSystem].
 *
 * @property priority Priority determines the order of execution
 * every update. Lower means it'll execute first. */
abstract class EntitySystem(private val priority: Int = 0) {

    internal lateinit var engine: Engine

    /** Called once per engine update cycle, put your logic here.
     * @param delta The time since the last call to this method. */
    abstract fun update(delta: Float)

    internal object SystemComparator : Comparator<EntitySystem> {
        override fun compare(a: EntitySystem, b: EntitySystem): Int {
            return if (a.priority > b.priority) 1 else if (a.priority == b.priority) 0 else -1
        }
    }
}