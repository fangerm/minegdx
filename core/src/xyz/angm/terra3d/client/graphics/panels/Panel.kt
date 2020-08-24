package xyz.angm.terra3d.client.graphics.panels

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import ktx.actors.onKeyDown
import ktx.scene2d.Scene2DSkin
import xyz.angm.terra3d.client.graphics.screens.Screen
import xyz.angm.terra3d.client.resources.sound

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
                sound.playSound("random/wood_click")
            false
        }
    }

    /** @see [com.badlogic.gdx.scenes.scene2d.Actor.setStage] */
    override fun setStage(stage: Stage?) {
        super.setStage(stage)
        stage?.keyboardFocus = focusedActor
        stage?.scrollFocus = focusedActor
    }

    /** @see [com.badlogic.gdx.scenes.scene2d.Actor.setVisible] and [PanelStack] */
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