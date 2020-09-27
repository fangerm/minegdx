/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 9:42 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.utils.Align

import xyz.angm.terra3d.client.graphics.actors.CraftingWindow
import xyz.angm.terra3d.client.graphics.actors.PlayerInventoryWindow
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH
import xyz.angm.terra3d.common.ecs.playerM

/** Player inventory panel. */
class PlayerInventoryPanel(screen: GameScreen) : InventoryPanel(screen) {
    init {
        addActor(PlayerInventoryWindow(this, screen.player[playerM].inventory).apply { setPosition(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, Align.center) })
        addActor(CraftingWindow(this))
    }
}