package xyz.angm.terra3d.client.graphics.panels.menu

import com.badlogic.gdx.Input
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.actors.plus
import ktx.scene2d.label
import ktx.scene2d.table
import ktx.scene2d.textButton
import ktx.scene2d.textField
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen

/** A simple panel for getting a string from the user.
 * @param screen Active menu screen
 * @param callback The method called once the user has confirmed their input
 * @param labelText The text for the label above the input field
 * @param confirmText The text of the confirmation button */
class GetUserInputPanel(screen: MenuScreen, labelText: String, confirmText: String, callback: (String?) -> Unit) : Panel(screen) {

    init {
        this + table {
            label(labelText) { it.pad(20f).row() }

            val inputField = textField { it.width(400f).pad(20f).row() }
            focusedActor = inputField

            textButton(confirmText) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick { callback(inputField.text) }
            }

            onKeyDown { keycode ->
                when (keycode) {
                    Input.Keys.ESCAPE -> callback(null)
                    Input.Keys.ENTER -> callback(inputField.text)
                }
            }

            setFillParent(true)
        }
        clearListeners()
    }
}