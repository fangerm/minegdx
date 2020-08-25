package xyz.angm.terra3d.client.graphics.panels.game

import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import ktx.actors.onClick
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.panels.menu.options.OptionsPanel
import xyz.angm.terra3d.client.graphics.screens.GameScreen

/** In-game pause screen. */
class PausePanel(screen: GameScreen) : Panel(screen) {

    init {
        val continueButton = TextButton("Continue Playing", skin)
        add(continueButton).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

        val optionsButton = TextButton("Options", skin)
        add(optionsButton).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

        val exitButton = TextButton("Return to Menu", skin)
        add(exitButton).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

        continueButton.onClick { screen.popPanel() }

        optionsButton.onClick { screen.pushPanel(OptionsPanel(screen)) }

        exitButton.onClick { screen.returnToMenu() }
    }
}