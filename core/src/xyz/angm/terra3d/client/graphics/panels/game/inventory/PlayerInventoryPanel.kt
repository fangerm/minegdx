/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/1/20, 11:01 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.utils.Align
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.worldHeight
import xyz.angm.terra3d.client.graphics.screens.worldWidth
import xyz.angm.terra3d.client.graphics.windows.CraftingWindow
import xyz.angm.terra3d.client.graphics.windows.PlayerInventoryWindow
import xyz.angm.terra3d.client.graphics.windows.QuestWindow
import xyz.angm.terra3d.common.ecs.playerM

/** Player inventory panel. */
class PlayerInventoryPanel(screen: GameScreen) : InventoryPanel(screen) {
    init {
        addActor(PlayerInventoryWindow(this, screen.player[playerM].inventory).apply { setPosition(worldWidth / 2, worldHeight / 2, Align.center) })
        addActor(CraftingWindow(this))
        addActor(QuestWindow(screen.player[playerM]))
    }
}