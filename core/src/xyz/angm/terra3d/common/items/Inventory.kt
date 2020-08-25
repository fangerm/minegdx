package xyz.angm.terra3d.common.items

import java.io.Serializable

/** An inventory, capable of holding and modifying a specified amount of items.
 * @property size The size of the inventory. */
open class Inventory(size: Int = 0) : Serializable {

    private var items = Array<Item?>(size) { null }
    val size get() = items.size

    /** Gets items.
     * @param index Slot/index of the item.
     * @return Item at specified slot, or null if slot is empty. */
    operator fun get(index: Int) = items.getOrNull(index)

    /** Sets slots.
     * @param index Slot/index to set.
     * @param item Item to set it to. */
    operator fun set(index: Int, item: Item?) = items.set(index, item)

    /** Add item to inventory, taking already existing stacks into account.
     * @param newItem Item to add. */
    operator fun plusAssign(newItem: Item) = addToRange(newItem, 0 until size)

    /** Add item to inventory, taking already existing stacks into account. Allows specifying range of slots allowed to add to.
     * @param newItem Item to add.
     * @param range The range of slots allowed to add to. */
    fun addToRange(newItem: Item, range: IntRange) {
        for (i in range) {
            val item = this[i] ?: continue
            if (newItem stacksWith item) {
                if ((item.amount + newItem.amount) > item.properties.stackSize) {
                    // This stack cannot fit all newly added items, fill it and keep iterating
                    newItem.amount -= (item.properties.stackSize - item.amount)
                    item.amount = item.properties.stackSize
                } else {
                    // It fits all, set correct amount and return
                    item.amount += newItem.amount
                    return
                }
            }
        }

        // No fitting stack found, just put it into the first empty slot
        for (i in range) {
            if (this[i] == null) {
                this[i] = newItem
                return
            }
        }
    }

    /** Removes amount specified from slot. Empties slot if items left <= 0.
     * @param slot The slot to remove from.
     * @param amount The amount to remove. */
    fun subtractFromSlot(slot: Int, amount: Int) {
        if (this[slot] != null) {
            this[slot]!!.amount -= amount
            if (this[slot]!!.amount <= 0) this[slot] = null
        }
    }

    /** Returns if inventory contains specified item.
     * @param item The item to search for.
     * @param amount The amount needed; item.amount used when not specified. */
    fun contains(item: Item, amount: Int = item.amount): Boolean {
        var amountLeft = amount
        for (i in 0 until size) {
            val it = items[i] ?: continue
            if (it stacksWith item) {
                amountLeft -= it.amount
                if (amountLeft < 0) return true
            }
        }
        return false
    }

    /** Returns if inventory contains specified item type.
     * @param type The item type to search for.
     * @param amount The amount needed; item.amount used when not specified. */
    fun contains(type: ItemType, amount: Int = 1): Boolean {
        for (i in 0 until size) {
            val it = items[i] ?: continue
            if (it.type == type && it.amount >= amount) return true
        }
        return false
    }

    /** Clears all items. */
    fun clear() {
        for (i in 0 until size) items[i] = null
    }

    /** @return The amount of slots that contain an item. */
    fun occupiedSize(): Int {
        var usedSize = size
        items.forEach { if (it == null) usedSize-- }
        return usedSize
    }

    /** Returns a formatted list of all items in the inventory; 1 slot per line. */
    override fun toString(): String {
        var s = ""
        items.forEach { if (it != null) s += "\n" + it.toString() }
        return s
    }
}