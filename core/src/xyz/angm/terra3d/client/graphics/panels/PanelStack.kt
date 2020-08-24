package xyz.angm.terra3d.client.graphics.panels

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import ktx.actors.alpha
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH

private const val TRANSITION_DURATION = 0.2f
private val TRANSITION_IN = Interpolation.pow3In
private val TRANSITION_OUT = Interpolation.pow3Out

/** A stack of panels. Always displays the panel at the top of the stack,
 * while also using nice animations for transitioning between panels.
 * @property panelsInStack The amount of panels currently in the stack. */
class PanelStack : Actor(), Disposable {

    private val panels = Array<Panel>(true, 5)
    val panelsInStack: Int
        get() = panels.size

    /** Pops the top panel off the stack. Will automatically display the next panel. */
    fun popPanel(): Panel {
        val panel = panels.pop()!!
        panel.dispose()
        panel.addAction(
            Actions.sequence(
                Actions.fadeOut(TRANSITION_DURATION, TRANSITION_OUT),
                Actions.removeActor()
            )
        )

        if (!panels.isEmpty) transitionIn(panels.peek())
        return panel
    }

    /** Pushes a panel on top of the stack. Hides the current top panel and displays the new one. */
    fun pushPanel(panel: Panel) {
        if (!panels.isEmpty) transitionOut(panels.peek())
        panels.add(panel)
        panel.setSize(WORLD_WIDTH, WORLD_HEIGHT)
        stage.addActor(panel)
        panel.alpha = 0f
        transitionIn(panel)
    }

    private fun transitionIn(panel: Panel) {
        panel.addAction(
            Actions.sequence(
                Actions.visible(true),
                Actions.fadeIn(TRANSITION_DURATION, TRANSITION_IN)
            )
        )
    }

    private fun transitionOut(panel: Panel) {
        panel.addAction(
            Actions.sequence(
                Actions.fadeOut(TRANSITION_DURATION, TRANSITION_OUT),
                Actions.visible(false)
            )
        )
    }

    /** Disposes all panels in the stack. */
    override fun dispose() = panels.forEach { it.dispose() }
}