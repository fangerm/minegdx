/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/1/20, 10:25 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.rox.systems

/** An extension of [EntitySystem] that only runs
 * after a specific delta was reached.
 *
 * @param interval The interval in seconds.
 * @param priority [EntitySystem.priority] */
abstract class IntervalSystem(
    private val interval: Float,
    priority: Int = 0
) : EntitySystem(priority) {

    private var counter = 0f

    override fun update(delta: Float) {
        counter += delta
        if (counter > interval) {
            counter = 0f
            run()
        }
    }

    /** This function is called every interval. */
    abstract fun run()
}