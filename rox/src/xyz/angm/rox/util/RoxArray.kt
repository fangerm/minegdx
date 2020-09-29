/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 6:42 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.rox.util

import java.lang.reflect.Array.newInstance
import kotlin.reflect.KClass

/** A simple growable array similar to libGDX's Array.
 * This is reimplemented in rox to allow for outside immutability.
 * Note that nested iteration of this array is NOT allowed! It'll
 * lead to some very weird and nasty bugs. */
class RoxArray<T : Any>(private val ordered: Boolean, initial: Int) : Iterable<T> {

    private var data = Array<Any?>(initial) { null } as Array<T?>
    val capacity get() = data.size
    var size = 0
        private set
    private val iterator = RoxIterator(this)

    val isEmpty get() = size == 0

    constructor(initial: Int) : this(true, initial)

    /** Get item at index, or null. */
    operator fun get(index: Int) = data[index]

    /** Returns a raw array copy of this RoxArray.
     * @param type The generic type of this array (thanks java type erasure) */
    fun toArray(type: KClass<out T>): Array<T> {
        val out = newInstance(type.java, size) as Array<T>
        System.arraycopy(data, 0, out, 0, size)
        return out
    }

    /** Set component at index. */
    internal operator fun set(index: Int, elem: T?) {
        if (index >= size) throw ArrayIndexOutOfBoundsException()
        data[index] = elem
    }

    internal fun add(elem: T?) {
        if (size >= data.size) grow(size * 2)
        data[size++] = elem
    }

    internal fun remove(elem: T) {
        if (ordered) throw UnsupportedOperationException()
        val idx = indexOfFirst { it == elem }
        data[idx] = data[size - 1]
        data[--size] = null
    }

    internal fun pop(): T? {
        val v = data[--size]
        data[size] = null
        return v
    }

    /** Clear this array, emptying it. */
    internal fun clear() {
        while (size > 0) {
            data[--size] = null
        }
    }

    internal fun sort() = data.sort(0, size)

    /** Grow the array to the given size, keeping all elements. */
    private fun grow(size: Int = capacity * 2) {
        val oldData = data
        data = arrayOfNulls<Any?>(size) as Array<T?>
        System.arraycopy(oldData, 0, data, 0, oldData.size)
    }

    /** Get the iterator. This array does not support nested iteration!
     * Create your own iterator if you need this. */
    override fun iterator(): Iterator<T> {
        iterator.index = 0
        return iterator
    }

    class RoxIterator<T : Any>(private val array: RoxArray<T>) : Iterator<T>, Iterable<T> {
        var index = 0
        override fun hasNext() = index < array.size
        override fun next() = array[index++]!!
        override fun iterator() = this
    }
}