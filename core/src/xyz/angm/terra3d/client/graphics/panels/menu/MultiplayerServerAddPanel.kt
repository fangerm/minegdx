package xyz.angm.terra3d.client.graphics.panels.menu

import com.badlogic.gdx.Input
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.actors.plusAssign
import ktx.scene2d.scene2d
import ktx.scene2d.textField
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.configuration

/** Multiplayer panel for adding a server to the list. */
class MultiplayerServerAddPanel(screen: MenuScreen, parent: MultiplayerMenuPanel) : Panel(screen) {

    init {
        this += scene2d.visTable {
            visLabel(I18N["multi-add.name"]) { it.pad(20f).row() }

            val nameInputField = textField { it.width(400f).pad(20f).row() }
            focusedActor = nameInputField

            visLabel(I18N["multi-add.ip"]) { it.pad(20f).row() }

            val ipInputField = textField { it.width(400f).pad(20f).padBottom(40f).row() }

            visTextButton(I18N["multi.add"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick {
                    configuration.addServer(nameInputField.text, ipInputField.text)
                    parent.reload(screen)
                    screen.popPanel()
                }
            }

            backButton(this, screen)
            row()

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