/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 7:30 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.items.metadata.IMetadata
import xyz.angm.terra3d.common.world.Block

/** Tooltip showing info about an item.
 * @param panel The currently active panel. */
class ItemTooltip(panel: Panel) : Table(panel.skin) {

    private var guideLabel = Label("", skin, "default-16pt")
    private var guideText: String? = ""
    private var noShiftText = ""

    init {
        background = skin.getDrawable("black-transparent")
        pad(6f, 12f, 6f, 12f)
    }

    /** Update the item the panel is showing. */
    private fun update(properties: Item.Properties?, id: ItemType?, metadata: IMetadata?) {
        clearChildren()
        if (properties == null || id == null) {
            isVisible = false
            return
        }

        add(Label("${properties.name} (#$id)", skin, "default-24pt")).row()
        if (metadata != null) add(Label(metadata.toString(), skin, "italic-16pt")).row()
        guideText = properties.guide
        noShiftText = if (guideText != null) {
            add(guideLabel).padTop(10f).row()
            I18N["guide.available"]
        } else ""

        zIndex = 100 // Always on top
        isVisible = true
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) guideLabel.setText(guideText)
        else guideLabel.setText(noShiftText)
        pack()
    }

    fun update(block: Block?) = update(block?.properties, block?.type, block?.metadata)
    fun update(item: Item?) = update(item?.properties, item?.type, item?.metadata)
}