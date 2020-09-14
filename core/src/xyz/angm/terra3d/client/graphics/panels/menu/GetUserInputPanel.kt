package xyz.angm.terra3d.client.graphics.panels.menu

import com.badlogic.gdx.Input
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.actors.plusAssign
import ktx.scene2d.scene2d
import ktx.scene2d.textField
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.resources.I18N

/** A simple panel for getting a string from the user.
 * Does not automatically remove itself once user inputs something!
 * @param screen Active menu screen
 * @param callback The method called once the user has confirmed their input
 * @param visLabelText The text for the visLabel above the input field
 * @param confirmText The text of the confirmation button */
class GetUserInputPanel(screen: MenuScreen, visLabelText: String, confirmText: String, callback: (String?) -> Unit) : Panel(screen) {

    init {
        this += scene2d.visTable {
            visLabel(visLabelText) { it.pad(20f).row() }

            val inputField = textField { it.width(400f).pad(20f).padBottom(40f).row() }
            focusedActor = inputField

            visTextButton(confirmText) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick { callback(inputField.text) }
            }

            visTextButton(I18N["back"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick { callback(null) }
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