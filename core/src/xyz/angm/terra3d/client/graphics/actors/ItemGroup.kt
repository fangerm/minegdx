package xyz.angm.terra3d.client.graphics.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import ktx.scene2d.Scene2DSkin
import xyz.angm.terra3d.client.graphics.panels.game.inventory.InventoryPanel
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.Item

/** Displays a group of items.
 * @param panel The currently active panel
 * @param inventory The inventory to display
 * @param startOffset The offset of the inventory
 * @param itemOffsetX The offset between every item, X axis
 * @param itemOffsetY The offset between every item, Y axis
 * @param rows The amount of rows
 * @param columns The amount of columns
 * @param mutable If the items of the group can be set, or only taken out (useful for crafting interfaces) */
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
                val actor = ItemActor(this, index)
                actor.setPosition(itemOffsetX * xOffset.toFloat(), itemOffsetY * yOffset.toFloat())
                addActor(actor)
                index++
            }
    }

    /** An actor showing a single item.
     * @param group The group the item belongs to
     * @param slot The slot of the item in the group
     * @param forceItem The item to use. Will override the first two parameters.
     * @property item The item currently represented by the actor,  */
    class ItemActor(
        val group: ItemGroup,
        val slot: Int,
        private val forceItem: Item? = null
    ) : Actor() {

        private var mouseOver = false
        var item
            get() = forceItem ?: group.inventory[slot]
            set(value) {
                if (forceItem == null) group.inventory[slot] = value
            }

        init {
            width = 32f
            height = 32f

            addListener(object : ClickListener(-1) {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    when (event.button) {
                        Input.Buttons.LEFT -> {
                            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) group.panel?.itemShiftClicked(this@ItemActor)
                            else group.panel?.itemLeftClicked(this@ItemActor)
                        }
                        Input.Buttons.RIGHT -> group.panel?.itemRightClicked(this@ItemActor)
                        else -> return // Prevent redraw when no change happened
                    }
                    group.redraw()
                }

                override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                    mouseOver = true
                    group.panel?.itemHovered(this@ItemActor)
                }

                override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                    mouseOver = false
                    group.panel?.itemLeft()
                }
            })
        }

        /** Returns a clone of itself, with the current item locked in.
         * @param item Override item to show */
        fun lockedClone(item: Item? = group.inventory[slot]) = ItemActor(group, slot, item)

        /** @see com.badlogic.gdx.scenes.scene2d.Actor.draw */
        override fun draw(batch: Batch, parentAlpha: Float) {
            val item = forceItem ?: group.inventory[slot]
            if (item != null) {
                batch.draw(item.texture, x, y, width, height)

                if (item.amount > 1) font.draw(
                    batch, item.amount.toString(),
                    x + fontOffsetX,
                    y + fontOffsetY
                )
            }
            if (mouseOver) batch.draw(selectorTexture, x, y)
        }

        private companion object {
            private const val fontOffsetX = 14
            private const val fontOffsetY = 12

            private val font = Scene2DSkin.defaultSkin.getFont("default-16pt")
            private val selectorTexture = Scene2DSkin.defaultSkin.get("item-selector", Texture::class.java)
        }
    }
}