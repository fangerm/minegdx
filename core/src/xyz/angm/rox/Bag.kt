package xyz.angm.rox

import kotlin.math.max

class Bag(internal var data: Array<Component?>) {

    val capacity get() = data.size
    var size = data.size
        private set

    constructor(capacity: Int) : this(arrayOfNulls<Component?>(capacity)) {
        size = 0
    }

    operator fun get(index: Int) = data[index]

    operator fun set(index: Int, component: Component?) {
        if (index >= data.size) grow(index * 2)
        size = max(index + 1, size)
        data[index] = component
    }

    fun add(component: Component) {
        if (size == data.size) grow()
        data[size++] = component
    }

    fun remove(index: Int): Component? {
        val e = data[index]
        data[index] = data[--size]
        data[size] = null
        return e
    }

    fun clear() {
        while (size > 0) {
            data[--size] = null
        }
    }

    private fun grow(size: Int = capacity * 2) {
        val oldData = data
        data = arrayOfNulls(size)
        System.arraycopy(oldData, 0, data, 0, oldData.size)
    }
}