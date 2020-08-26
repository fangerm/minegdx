package xyz.angm.terra3d.client.graphics.panels.menu.options

import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.onKey
import ktx.actors.plus
import ktx.scene2d.*
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.panels.menu.MainMenuPanel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.graphics.screens.Screen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.configuration

/** Main options menu. */
class OptionsPanel(private val screen: Screen, parent: MainMenuPanel? = null) : Panel(screen) {

    init {
        reload(screen, parent)
    }

    private fun reload(screen: Screen, parent: MainMenuPanel?) {
        clearChildren()
        this + table {
            // Only show certain options on menu screen
            if (screen is MenuScreen) {
                textButton(I18N["options.video"]) {
                    it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).colspan(2).row()
                    onClick { screen.pushPanel(VideoOptionsPanel(screen)) }
                }

                textButton(I18N["options.resourcepack"]) {
                    it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).colspan(2).row()
                    onClick { screen.pushPanel(ResourcePackPanel(screen)) }
                }

                val box = selectBoxOf(I18N.languages())
                box.selected = configuration.language
                box.onChange {
                    I18N.setLanguage(box.selected)
                    parent!!.reload(screen)
                    screen.popPanel()
                }
                row()
            }

            textButton(I18N["options.controls"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).colspan(2).row()
                onClick { screen.pushPanel(KeybindsPanel(screen)) }
            }

            label(I18N["options.playername"], style = "default-24pt") {
                it.pad(10f)
            }

            textField(configuration.playerName) {
                it.width(400f).pad(20f).row()
                onKey { configuration.playerName = text }
            }

            setFillParent(true)
        }
    }

    override fun dispose() = configuration.save()
}