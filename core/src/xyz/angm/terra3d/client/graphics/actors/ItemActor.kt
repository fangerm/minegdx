package xyz.angm.terra3d.client.graphics.actors

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import ktx.scene2d.Scene2DSkin
import xyz.angm.terra3d.common.items.Item

/** An actor showing a single item.
 * @param item The item to display. */
open class ItemActor(open var item: Item? = null) : Actor() {

    // If the mouse is currently over this actor.
    protected var mouseOver = false
    val full get() = item!!.amount == item!!.properties.stackSize
    val empty get() = item == null

    init {
        width = 32f
        height = 32f
    }

    infix fun stacksWith(other: Item?) = item?.stacksWith(other) == true

    /** @see com.badlogic.gdx.scenes.scene2d.Actor.draw */
    override fun draw(batch: Batch, parentAlpha: Float) {
        val item = item
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