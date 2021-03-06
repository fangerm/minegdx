/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.menu

import com.badlogic.gdx.Input
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.actors.plusAssign
import ktx.scene2d.scene2d
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.Screen
import xyz.angm.terra3d.client.resources.I18N

/** A panel for displaying a message.
 * @param visLabelText The text to display.
 * @param returnText The text of the return button.
 * @param callback Called when the user pressed the back button. */
class MessagePanel(screen: Screen, visLabelText: String, returnText: String = I18N["back"], callback: () -> Unit) : Panel(screen) {

    init {
        focusedActor = scene2d.visTable {
            visLabel(visLabelText) { it.pad(20f).row() }

            visTextButton(returnText) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick { callback() }
            }

            onKeyDown { keycode ->
                when (keycode) {
                    Input.Keys.ESCAPE, Input.Keys.ENTER -> callback()
                }
            }

            setFillParent(true)
        }
        this += focusedActor
        clearListeners() // Remove escape listener in Panel
    }
}