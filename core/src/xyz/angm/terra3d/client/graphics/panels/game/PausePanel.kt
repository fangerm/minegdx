package xyz.angm.terra3d.client.graphics.panels.game

import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import ktx.actors.onClick
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.panels.menu.options.OptionsPanel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.I18N

/** In-game pause screen. */
class PausePanel(screen: GameScreen) : Panel(screen) {

    init {
        val continueButton = TextButton(I18N["pause.continue"], skin)
        add(continueButton).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

        val optionsButton = TextButton(I18N["pause.options"], skin)
        add(optionsButton).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

        val exitButton = TextButton(I18N["pause.exit"], skin)
        add(exitButton).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

        continueButton.onClick { screen.popPanel() }

        optionsButton.onClick { screen.pushPanel(OptionsPanel(screen)) }

        exitButton.onClick { screen.returnToMenu() }
    }
}