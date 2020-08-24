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
import xyz.angm.terra3d.common.world.WorldSaveManager

/** Panel for creating a new world/save. */
class SingleplayerWorldCreatePanel(screen: MenuScreen) : Panel(screen) {


    init {
        this + table {
            label("Enter A Name:") { it.pad(20f).row() }
            val nameField = textField { it.width(400f).pad(20f).row() }
            focusedActor = nameField

            label("Enter Seed:") { it.pad(20f).row() }
            val seedField = textField(System.currentTimeMillis().toString()) { it.width(400f).pad(20f).row() }

            textButton("Create") {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick {
                    WorldSaveManager.addWorld(nameField.text, seedField.text)
                    screen.popPanel()
                }
            }

            onKeyDown { keycode ->
                when (keycode) {
                    Input.Keys.ESCAPE -> screen.popPanel()
                    Input.Keys.ENTER -> {
                        WorldSaveManager.addWorld(nameField.text, seedField.text)
                        screen.popPanel()
                        screen.popPanel()
                        screen.pushPanel(SingleplayerWorldSelectionPanel(screen))
                    }
                }
            }

            setFillParent(true)
        }
        clearListeners()
    }
}