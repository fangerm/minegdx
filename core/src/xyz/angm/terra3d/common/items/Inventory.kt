package xyz.angm.terra3d.common.items

import java.io.Serializable

/** An inventory, capable of holding and modifying a specified amount of items.
 * @property size The size of the inventory. */
open class Inventory(size: Int = 0) : Serializable {

    protected var items = Array<Item?>(size) { null }
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
    operator fun plusAssign(newItem: Item) {
        addToRange(newItem, 0 until size)
    }

    /** Add item to inventory, taking already existing stacks into account.
     * @param newItem Item to add. */
    operator fun minusAssign(newItem: Item) = removeFromRange(newItem, 0 until size)

    /** Add item to inventory, taking already existing stacks into account.
     * @param item Item to add.
     * @return The amount of items not added due to full inventory. 0 indicates all were successfully added. */
    fun add(item: Item) = addToRange(item, 0 until size)

    /** Add item to inventory, taking already existing stacks into account. Allows specifying range of slots allowed to add to.
     * @param newItem Item to add.
     * @param range The range of slots allowed to add to.
     * @return The amount of items not added due to full inventory. 0 indicates all were successfully added. */
    fun addToRange(newItem: Item, range: IntRange): Int {
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
                    return 0
                }
            }
        }

        // No fitting stack found, just put it into the first empty slot
        for (i in range) {
            if (this[i] == null) {
                this[i] = newItem
                return 0
            }
        }
        return newItem.amount
    }


    /** Removes a given item type and amount from the inventory.
     * Does not report on failure, check yourself. */
    private fun removeFromRange(removeItem: Item, range: IntRange) {
        var left = removeItem.amount
        for (i in range) {
            val item = this[i] ?: continue
            if (removeItem stacksWith item) {
                subtractFromSlot(i, left)
                if (left - item.amount <= 0) return
                else left -= item.amount
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

    /** Removes the first item stack in this inventory if there are any items,
     * and returns it. */
    fun takeFirst(): Item? {
        for (i in 0 until size) {
            val item = items[i]
            if (item != null) {
                items[i] = null
                return item
            }
        }
        return null
    }

    /** Clears all items. */
    fun clear() {
        for (i in 0 until size) items[i] = null
    }

    /** Returns a formatted list of all items in the inventory; 1 slot per line. */
    override fun toString(): String {
        var s = ""
        items.forEach { if (it != null) s += "\n" + it.toString() }
        return s
    }
}