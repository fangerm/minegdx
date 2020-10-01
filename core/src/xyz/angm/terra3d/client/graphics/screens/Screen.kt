/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/1/20, 11:19 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.screens

import com.badlogic.gdx.utils.viewport.ExtendViewport
import xyz.angm.terra3d.client.graphics.panels.Panel

/** The viewport currently in use by the game.
 * This is a global out of legacy support reasons; viewport
 * was fixed before meaning world width/height were constants;
 * changing all code using them would be a lot of work so
 * this was the easiest reasonable solution. */
val viewport = ExtendViewport(1920f, 1080f)

/** World width of the viewport. */
val worldWidth get() = viewport.worldWidth

/** World height of the viewport. */
val worldHeight get() = viewport.worldHeight

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
