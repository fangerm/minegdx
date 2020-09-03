package xyz.angm.terra3d.client.graphics.panels

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.KVisTextButton
import ktx.scene2d.vis.visTextButton
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.screens.Screen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.soundPlayer

/** A panel is overlaid onto a screen, and used for UI.
 * @param screen The screen currently active
 * @property focusedActor The actor to receive keyboard & scroll focus. Defaults to the panel itself. */
@Suppress("LeakingThis") // While this can be an issue, the methods called should not be overridden anyways
abstract class Panel(screen: Screen) : Table(Scene2DSkin.defaultSkin) {

    protected open var focusedActor: Actor = this

    init {
        setFillParent(true)
        background = skin.getDrawable("black-transparent")

        onKeyDown { keycode ->
            if (keycode == Input.Keys.ESCAPE) screen.popPanel()
        }

        addCaptureListener {
            // Play a sound when a button was clicked
            if ((it as? InputEvent)?.type == InputEvent.Type.touchDown && (it.target is Button || it.target is Label))
                soundPlayer.playSound("random/wood_click")
            false
        }
    }

    /** A function that will add a back button to a panel constructed with KTX,
     * see most panels in menu for an example. */
    internal fun backButton(local: KVisTable, screen: Screen): KVisTextButton {
        return local.visTextButton(I18N["back"]) {
            it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f)
            onClick { screen.popPanel() }
        }
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)
        stage?.keyboardFocus = focusedActor
        stage?.scrollFocus = focusedActor
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (visible) {
            stage?.keyboardFocus = focusedActor
            stage?.scrollFocus = focusedActor
        }
    }

    /** Should be called when panel is to be removed. */
    open fun dispose() {}
}