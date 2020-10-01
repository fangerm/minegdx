/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/1/20, 9:50 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.rox.systems

import xyz.angm.rox.Engine

/** A system inside the ECS, acting on entities every update cycle.
 * Add your systems with [Engine.add].
 * If you want to act only on certain entities, use [IteratingSystem].
 *
 * @property priority Priority determines the order of execution
 * every update. Lower means it'll execute first.
 * @property engine The engine this system is a part of. */
abstract class EntitySystem(private val priority: Int = 0) : Comparable<EntitySystem> {

    lateinit var engine: Engine

    /** Called once per engine update cycle, put your logic here.
     * @param delta The time since the last call to this method. */
    abstract fun update(delta: Float)

    override fun compareTo(other: EntitySystem): Int {
        return if (priority > other.priority) 1 else if (priority == other.priority) 0 else -1
    }
}