/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 9:20 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import xyz.angm.terra3d.client.graphics.screens.worldHeight
import xyz.angm.terra3d.client.graphics.screens.worldWidth

private const val TRANSITION_DURATION = 0.2f
private val TRANSITION = Interpolation.pow3

/** A stack of panels. Always displays the panel at the top of the stack,
 * while also using nice animations for transitioning between panels.
 * @property panelsInStack The amount of panels currently in the stack. */
class PanelStack : Actor(), Disposable {

    private val panels = Array<Panel>(true, 5)
    val current get() = panels.last()!!
    val panelsInStack: Int
        get() = panels.size

    /** Pops the top panel off the stack. Will automatically display the next panel. */
    fun popPanel(direction: Int = 1) {
        val panel = if (panels.isEmpty) null else panels.pop()
        panel?.addAction(
            Actions.sequence(
                Actions.moveTo(worldWidth * direction, 0f, TRANSITION_DURATION, TRANSITION),
                Actions.visible(false),
                Actions.run { panel.dispose() },
                Actions.removeActor()
            )
        )

        if (!panels.isEmpty) transitionIn(panels.peek(), -1)
    }

    /** Pushes a panel on top of the stack. Hides the current top panel and displays the new one. */
    fun pushPanel(panel: Panel) {
        if (!panels.isEmpty) transitionOut(panels.peek())
        panels.add(panel)
        panel.setSize(worldWidth, worldHeight)
        stage.addActor(panel)
        transitionIn(panel)
    }

    private fun transitionIn(panel: Panel, direction: Int = 1) {
        panel.x = worldWidth * direction
        panel.addAction(
            Actions.sequence(
                Actions.visible(true),
                Actions.moveTo(0f, 0f, TRANSITION_DURATION, TRANSITION),
                Actions.visible(true)
            )
        )
    }

    private fun transitionOut(panel: Panel) {
        panel.addAction(
            Actions.sequence(
                Actions.moveTo(-worldWidth, 0f, TRANSITION_DURATION, TRANSITION),
                Actions.visible(false)
            )
        )
    }

    /** Disposes all panels in the stack. */
    override fun dispose() = panels.forEach { it.dispose() }
}