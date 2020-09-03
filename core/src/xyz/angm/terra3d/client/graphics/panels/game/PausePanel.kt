package xyz.angm.terra3d.client.graphics.panels.game

import com.kotcrab.vis.ui.widget.VisTextButton
import ktx.actors.onClick
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.panels.menu.options.OptionsPanel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.I18N

/** In-game pause screen. */
class PausePanel(screen: GameScreen) : Panel(screen) {

    init {
        val continueButton = VisTextButton(I18N["pause.continue"])
        add(continueButton).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

        val optionsButton = VisTextButton(I18N["pause.options"])
        add(optionsButton).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

        val exitButton = VisTextButton(I18N["pause.exit"])
        add(exitButton).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

        continueButton.onClick { screen.popPanel() }
        optionsButton.onClick { screen.pushPanel(OptionsPanel(screen)) }
        exitButton.onClick { screen.returnToMenu() }
    }
}