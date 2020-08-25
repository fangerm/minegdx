package xyz.angm.terra3d.client.graphics.panels.menu

import com.badlogic.gdx.Gdx
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.image
import ktx.scene2d.table
import ktx.scene2d.textButton
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.panels.menu.options.OptionsPanel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen

/** Main menu panel. */
class MainMenuPanel(screen: MenuScreen) : Panel(screen) {

    init {
        this + table {
            pad(0f, 0f, 100f, 0f)

            image("logo") {
                it.height(232f).width(800f).pad(20f).row()
            }

            textButton("Start Singleplayer") {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick { screen.pushPanel(SingleplayerWorldSelectionPanel(screen)) }
            }
            textButton("Connect to Server") {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick { screen.pushPanel(MultiplayerMenuPanel(screen)) }
            }
            textButton("Options") {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick { screen.pushPanel(OptionsPanel(screen)) }
            }
            textButton("Exit Game") {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick { Gdx.app.exit() }
            }

            setFillParent(true)
        }
    }
}