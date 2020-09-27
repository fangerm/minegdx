/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/27/20, 12:02 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.rox

import kotlin.math.max

/** A collection of elements very similar to a dynamically growing array.
 * Main difference to a regular array is that out-of-bounds operations
 * like get or set cause the bag to resize to fit instead of being
 * an illegal operation. Used by [Entity].
 *
 * @property data Backing array of components
 * @property capacity Current capacity of this bag
 * @property size Size of this bag, or the index of the highest set. */
class Bag internal constructor(internal var data: Array<Component?>) {

    val capacity get() = data.size
    var size = data.size
        private set

    internal constructor(capacity: Int) : this(arrayOfNulls<Component?>(capacity)) {
        size = 0
    }

    /** Get component at index, or null. */
    operator fun get(index: Int) = if (index >= data.size) null else data[index]

    /** Set component at index, expanding the bag if needed to fit. */
    operator fun set(index: Int, component: Component?) {
        if (index >= data.size) grow(index * 2)
        size = max(index + 1, size)
        data[index] = component
    }

    /** Clear this bag, emptying it. */
    fun clear() {
        while (size > 0) {
            data[--size] = null
        }
    }

    /** Grow the bag to the given size, keeping all elements. */
    private fun grow(size: Int = capacity * 2) {
        val oldData = data
        data = arrayOfNulls(size)
        System.arraycopy(oldData, 0, data, 0, oldData.size)
    }
}