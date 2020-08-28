package xyz.angm.terra3d.client.graphics.panels.menu.options

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.actors.plus
import ktx.scene2d.scrollPane
import ktx.scene2d.table
import xyz.angm.terra3d.client.actions.PlayerAction
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.Screen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.configuration

/** Options submenu for keybinds. */
class KeybindsPanel(private var screen: Screen) : Panel(screen) {

    private lateinit var current: Pair<Int, PlayerAction>
    private lateinit var table: Table

    init {
        focusedActor = scrollPane {
            table = table {}

            onKeyDown { keycode ->
                if (keycode == Input.Keys.ESCAPE) {
                    configuration.save()
                    screen.popPanel()
                } else {
                    configuration.keybinds.unregisterKeybind(current.first)
                    configuration.keybinds.registerKeybind(keycode, current.second.type)
                    updateBinds()
                }
            }

            setFillParent(true)
        }
        this + focusedActor
        clearListeners()
        updateBinds()
    }

    private fun updateBinds() {
        table.clearChildren()

        configuration.keybinds.getAllSorted().forEach { action ->
            val label = Label("${I18N["keybind.${action.second.type}"]}:", skin)
            table.add(label).pad(20f)

            val button = TextButton(Input.Keys.toString(action.first), skin)
            table.add(button).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

            button.onClick { current = action }
        }
    }
}