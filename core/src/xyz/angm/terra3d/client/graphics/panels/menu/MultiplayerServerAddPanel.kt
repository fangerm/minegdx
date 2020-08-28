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
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.configuration

/** Multiplayer panel for adding a server to the list. */
class MultiplayerServerAddPanel(screen: MenuScreen, parent: MultiplayerMenuPanel) : Panel(screen) {

    init {
        this + table {
            label(I18N["multi-add.name"]) { it.pad(20f).row() }

            val nameInputField = textField { it.width(400f).pad(20f).row() }
            focusedActor = nameInputField

            label(I18N["multi-add.ip"]) { it.pad(20f).row() }

            val ipInputField = textField { it.width(400f).pad(20f).row() }

            textButton(I18N["multi.add"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick {
                    configuration.addServer(nameInputField.text, ipInputField.text)
                    parent.reload(screen)
                    screen.popPanel()
                }
            }

            onKeyDown { keycode ->
                when (keycode) {
                    Input.Keys.ESCAPE -> screen.popPanel()
                    Input.Keys.ENTER -> {
                        configuration.addServer(nameInputField.text, ipInputField.text)
                        parent.reload(screen)
                        screen.popPanel()
                    }
                }
            }

            setFillParent(true)
        }
        clearListeners()
    }
}