package xyz.angm.terra3d.client.graphics.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import xyz.angm.terra3d.client.graphics.panels.game.inventory.InventoryPanel
import xyz.angm.terra3d.common.items.Inventory

/** Displays a group of items.
 * @param panel The currently active panel
 * @param inventory The inventory to display
 * @param startOffset The offset of the inventory
 * @param itemOffsetX The offset between every item, X axis
 * @param itemOffsetY The offset between every item, Y axis
 * @param rows The amount of rows
 * @param columns The amount of columns
 * @param mutable If the items of the group can be set, or only taken out (used for crafting interfaces) */
class ItemGroup(
    private val panel: InventoryPanel?,
    val inventory: Inventory,
    private val startOffset: Int = 0,
    private val itemOffsetX: Int = 36,
    private val itemOffsetY: Int = 36,
    val rows: Int,
    val columns: Int,
    val mutable: Boolean = true
) : Group() {

    init {
        width = columns * itemOffsetX.toFloat()
        height = rows * itemOffsetY.toFloat()
        redraw()
    }

    private fun redraw() {
        clearChildren()

        var index = startOffset
        for (yOffset in 0 until rows)
            for (xOffset in 0 until columns) {
                val actor = GroupedItemActor(this, index)
                actor.setPosition(itemOffsetX * xOffset.toFloat(), itemOffsetY * yOffset.toFloat())
                addActor(actor)
                index++
            }
    }

    /** An actor for an item inside an [ItemGroup].
     * @param group The group the item belongs to
     * @param slot The slot of the item in the group */
    class GroupedItemActor(
        val group: ItemGroup,
        val slot: Int
    ) : ItemActor() {

        override var item
            get() = group.inventory[slot]
            set(value) = group.inventory.set(slot, value)

        init {
            addListener(object : ClickListener(-1) {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    when (event.button) {
                        Input.Buttons.LEFT -> {
                            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) group.panel?.itemShiftClicked(this@GroupedItemActor)
                            else group.panel?.itemLeftClicked(this@GroupedItemActor)
                        }
                        Input.Buttons.RIGHT -> group.panel?.itemRightClicked(this@GroupedItemActor)
                        else -> return // Prevent redraw when no change happened
                    }
                    group.redraw()
                }

                override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                    mouseOver = true
                    group.panel?.itemHovered(this@GroupedItemActor)
                }

                override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                    mouseOver = false
                    group.panel?.itemLeft()
                }
            })
        }
    }
}