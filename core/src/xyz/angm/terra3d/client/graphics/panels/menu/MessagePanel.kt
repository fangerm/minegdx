package xyz.angm.terra3d.client.graphics.panels.menu

import com.badlogic.gdx.Input
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.actors.plus
import ktx.scene2d.label
import ktx.scene2d.table
import ktx.scene2d.textButton
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.Screen

/** A panel for displaying a message.
 * @param labelText The text to display.
 * @param returnText The text of the return button.
 * @param callback Called when the user pressed the back button. */
class MessagePanel(screen: Screen, labelText: String, returnText: String = "Back", callback: () -> Unit) : Panel(screen) {

    init {
        focusedActor = table {
            label(labelText) { it.pad(20f).row() }

            textButton(returnText) {
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
        this + focusedActor
        clearListeners() // Remove escape listener in Panel
    }
}