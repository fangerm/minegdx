package xyz.angm.terra3d.client.graphics.panels.menu.options

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import ktx.actors.onClick
import ktx.actors.onKey
import ktx.actors.onKeyDown
import ktx.actors.plusAssign
import ktx.scene2d.scene2d
import ktx.scene2d.scrollPane
import ktx.scene2d.textField
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import xyz.angm.terra3d.client.actions.PlayerAction
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.Screen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.configuration

/** Options submenu for controls. */
class ControlsPanel(private var screen: Screen) : Panel(screen) {

    private var current: Pair<Int, PlayerAction>? = null
    private var currentBtn: TextButton? = null
    private var table: Table

    init {
        this += scene2d.visTable {
            visTextButton(I18N["back"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).colspan(2)
                onClick { screen.popPanel() }
            }

            visLabel(I18N["options.sensitivity"]) {
                it.pad(10f).align(Align.right)
            }

            textField((configuration.sensitivity * 100).toString()) {
                it.width(200f).row()
                onKey { configuration.sensitivity = (text.toFloatOrNull() ?: return@onKey) / 100f }
            }

            focusedActor = scrollPane {
                table = visTable {}

                onKeyDown { keycode ->
                    if (current == null && keycode == Input.Keys.ESCAPE) {
                        configuration.save()
                        screen.popPanel()
                    } else {
                        val current = current ?: return@onKeyDown
                        configuration.keybinds.unregisterKeybind(current.first)
                        configuration.keybinds.unregisterKeybind(keycode)
                        configuration.keybinds.registerKeybind(keycode, current.second.type)
                        this@ControlsPanel.current = null
                        updateBinds()
                    }
                }

                it.colspan(4).pad(50f, 0f, 50f, 0f).expand().row()
            }

            setFillParent(true)
        }
        clearListeners()
        updateBinds()
    }

    private fun updateBinds() {
        table.clearChildren()

        configuration.keybinds.getAllSorted().forEach { action ->
            val label = Label("${I18N["keybind.${action.second.type}"]}:", skin)
            table.add(label).pad(20f)

            val text = if (action.first == 0) "---" else Input.Keys.toString(action.first)
            val button = TextButton(text, skin)
            table.add(button).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

            button.onClick {
                currentBtn?.label?.setColor(1f, 1f, 1f, 1f)
                if (current == action) {
                    current = null
                    currentBtn = null
                } else {
                    current = action
                    currentBtn = button
                    button.label.setColor(0f, 1f, 0f, 1f)
                }
            }
        }
    }
}