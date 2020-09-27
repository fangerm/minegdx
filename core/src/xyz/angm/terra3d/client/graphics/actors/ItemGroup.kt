/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/27/20, 2:04 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.widget.VisTable
import xyz.angm.terra3d.common.items.Inventory

/** Displays a group of items in a draggable window.
 * @param window The window this group belongs to
 * @param inventory The inventory to display
 * @param startOffset The offset of the inventory
 * @param row The amount of rows
 * @param column The amount of columns
 * @param mutable If the items of the group can be set, or only taken out (used for crafting interfaces) */
class ItemGroup(
    private val window: InventoryWindow?,
    var inventory: Inventory,
    val mutable: Boolean = true,
    startOffset: Int = 0,
    padding: Float = 4f,
    row: Int,
    column: Int,
) : VisTable() {

    init {
        for (index in startOffset until (row * column) + startOffset) {
            val actor = GroupedItemActor(this, index)
            add(actor).pad(padding)
            if (index != 0 && index % column == column - 1) row()
        }
        pack()
    }

    /** An actor for an item inside an [ItemGroup].
     * @param group The group the item belongs to
     * @param slot The slot of the item in the group */
    class GroupedItemActor(
        val group: ItemGroup,
        val slot: Int
    ) : ItemActor(window = group.window) {

        override var item
            get() = group.inventory[slot]
            set(value) = group.inventory.set(slot, value)

        init {
            addListener(object : ClickListener(-1) {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    when (event.button) {
                        Input.Buttons.LEFT -> {
                            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) group.window?.itemShiftClicked(this@GroupedItemActor)
                            else group.window?.itemLeftClicked(this@GroupedItemActor)
                        }
                        Input.Buttons.RIGHT -> group.window?.itemRightClicked(this@GroupedItemActor)
                    }
                }
            })
        }
    }
}