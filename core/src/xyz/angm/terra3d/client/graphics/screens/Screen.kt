package xyz.angm.terra3d.client.graphics.screens

import xyz.angm.terra3d.client.graphics.panels.Panel

/** World width. Used for viewports */
const val WORLD_WIDTH = 1920f

/** World height. Used for viewports */
const val WORLD_HEIGHT = 1080f

/** A basic interface for a Screen.
 * Every screen must provide an interface for pushing and popping panels.
 * The exact implementation of this is abstract, since the game screen in particular
 * needs some special handling in regards to player input. */
interface Screen {

    /** Push a new panel on top of the PanelStack active. */
    fun pushPanel(panel: Panel)

    /** Pops the current panel of the PanelStack and returns it. */
    fun popPanel()
}
