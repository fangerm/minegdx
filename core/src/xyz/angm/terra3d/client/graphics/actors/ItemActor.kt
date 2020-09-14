package xyz.angm.terra3d.client.graphics.actors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisTextButton
import ktx.scene2d.Scene2DSkin
import xyz.angm.terra3d.common.items.Item
import kotlin.math.min

/** An actor showing a single item.
 * @param item The item to display. */
open class ItemActor(open var item: Item? = null, window: InventoryWindow?) : Actor() {

    // If the mouse is currently over this actor.
    protected var mouseOver = false
    val full get() = item!!.amount == item!!.properties.stackSize
    val empty get() = item == null

    init {
        width = 32f
        height = 32f

        addListener(object : ClickListener(-1) {
            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                mouseOver = true
                window?.itemHovered(this@ItemActor)
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                mouseOver = false
                window?.itemLeft()
            }
        })
    }

    infix fun stacksWith(other: Item?) = item?.stacksWith(other) == true

    /** @see com.badlogic.gdx.scenes.scene2d.Actor.draw */
    override fun draw(batch: Batch, parentAlpha: Float) {
        baseTexture.draw(batch, x, y, width, height)

        val item = item
        if (item != null) {
            batch.draw(item.texture, x, y, width, height)

            if (item.amount > 1) font.draw(
                batch, item.amount.toString(),
                x + fontOffsetX - (fontOffsetAdd * min(item.amount / 100, 1)) - (fontOffsetAdd * min(item.amount / 10, 1)),
                y + fontOffsetY
            )
        }
        if (mouseOver) selectorTexture.draw(batch, x, y, width, height)
    }

    private companion object {
        private const val fontOffsetX = 22
        private const val fontOffsetAdd = 10
        private const val fontOffsetY = 14

        private val font = Scene2DSkin.defaultSkin.getFont("default-16pt")
        private val selectorTexture: Drawable
        private val baseTexture = VisUI.getSkin().get("vis-default", VisTextButton.VisTextButtonStyle::class.java).up

        init {
            val text = VisUI.getSkin().get("vis-default", VisTextButton.VisTextButtonStyle::class.java).over
            selectorTexture = (text as NinePatchDrawable).tint(Color(1f, 1f, 1f, 0.5f))
        }
    }
}