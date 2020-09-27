/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.actor
import ktx.scene2d.defaultStyle
import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.KVisTextButton
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.click
import xyz.angm.terra3d.client.graphics.screens.Screen
import xyz.angm.terra3d.client.resources.I18N

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
    }

    /** A function that will add a back button to a panel constructed with KTX,
     * see most panels in menu for an example. */
    internal fun KVisTable.backButton(screen: Screen): KVisTextButton {
        return visTextButton(I18N["back"]) {
            it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f)
            onClick { screen.popPanel() }
        }
    }

    /** A wrapper for visTextButton that adds a clicking noise capture listener. */
    inline fun <S> KWidget<S>.visTextButton(
        text: String,
        style: String = defaultStyle,
        init: KVisTextButton.(S) -> Unit = {}
    ): KVisTextButton {
        val a = actor(KVisTextButton(text, style), init)
        a.click()
        return a
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