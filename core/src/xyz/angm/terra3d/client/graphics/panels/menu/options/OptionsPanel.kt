package xyz.angm.terra3d.client.graphics.panels.menu.options

import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.onKey
import ktx.actors.plusAssign
import ktx.scene2d.scene2d
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visSelectBoxOf
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextField
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.panels.menu.MainMenuPanel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.graphics.screens.Screen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.configuration

/** Main options menu. */
class OptionsPanel(screen: Screen, parent: MainMenuPanel? = null) : Panel(screen) {

    init {
        reload(screen, parent)
    }

    private fun reload(screen: Screen, parent: MainMenuPanel?) {
        clearChildren()
        this += scene2d.visTable {
            // Only show certain options on menu screen
            if (screen is MenuScreen) {
                visTextButton(I18N["options.video"]) {
                    it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).colspan(2).row()
                    onClick { screen.pushPanel(VideoOptionsPanel(screen)) }
                }

                visTextButton(I18N["options.resourcepack"]) {
                    it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).colspan(2).row()
                    onClick { screen.pushPanel(ResourcePackPanel(screen)) }
                }

                val box = visSelectBoxOf(I18N.languages())
                box.selected = configuration.language
                box.onChange {
                    I18N.setLanguage(box.selected)
                    parent!!.reload(screen)
                    reload(screen, parent)
                    this@OptionsPanel.isVisible = true // Regrab focus lost by reload
                }
                box.inCell.colspan(2)
                row()
            }

            visTextButton(I18N["options.controls"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).colspan(2).row()
                onClick { screen.pushPanel(ControlsPanel(screen)) }
            }

            visLabel(I18N["options.playername"]) {
                it.pad(10f)
            }

            visTextField(configuration.playerName) {
                it.width(400f).pad(20f).row()
                onKey { configuration.playerName = text }
            }

            visTextButton(I18N["back"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).colspan(2)
                onClick { screen.popPanel() }
            }

            setFillParent(true)
        }
    }

    override fun dispose() = configuration.save()
}