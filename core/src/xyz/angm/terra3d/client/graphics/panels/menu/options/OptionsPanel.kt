package xyz.angm.terra3d.client.graphics.panels.menu.options

import ktx.actors.onClick
import ktx.actors.onKey
import ktx.actors.plus
import ktx.scene2d.label
import ktx.scene2d.table
import ktx.scene2d.textButton
import ktx.scene2d.textField
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.graphics.screens.Screen
import xyz.angm.terra3d.client.resources.configuration

/** Main options menu. */
class OptionsPanel(private val screen: Screen) : Panel(screen) {

    init {
        this + table {
            // Only show video options panel on menu screen
            if (screen is MenuScreen) {
                textButton("Video Options") {
                    it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).colspan(2).row()
                    onClick { screen.pushPanel(VideoOptionsPanel(screen)) }
                }
            }

            textButton("Key Bindings / Controls") {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).colspan(2).row()
                // onClick { screen.pushPanel(KeybindsPanel(screen)) }
            }

            // Only show resource pack panel on menu screen
            if (screen is MenuScreen) {
                textButton("Resource/Texture Pack") {
                    it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).colspan(2).row()
                    onClick { screen.pushPanel(ResourcePackPanel(screen)) }
                }
            }

            label("Player Name:", style = "default-24pt") {
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