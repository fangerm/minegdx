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
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.resources.I18N

/** A simple panel for getting a yes/no answer from the user.
 * Does not automatically remove itself once user inputs something!
 * @param screen Active menu screen
 * @param callback The method called once the user has confirmed their input */
class ConfirmationPanel(screen: MenuScreen, callback: (Boolean) -> Unit) : Panel(screen) {

    init {
        focusedActor = scene2d.visTable {
            visLabel(I18N["confirm"]) { it.pad(20f).row() }

            visTextButton(I18N["yes"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick { callback(true) }
            }

            visTextButton(I18N["no"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick { callback(false) }
            }

            onKeyDown { keycode ->
                when (keycode) {
                    Input.Keys.ESCAPE -> callback(false)
                    Input.Keys.ENTER -> callback(true)
                }
            }

            setFillParent(true)
        }
        this += focusedActor
        clearListeners()
    }
}