/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/30/20, 5:09 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.utils.Align
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH
import xyz.angm.terra3d.client.graphics.windows.FurnaceWindow
import xyz.angm.terra3d.client.graphics.windows.PlayerInventoryWindow
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.items.metadata.FurnaceMetadata
import xyz.angm.terra3d.common.world.Block

/** Panel for the furnace block. */
class FurnacePanel(screen: GameScreen, private val furnace: Block) : NetworkInventoryPanel(screen, furnace) {

    private val window = FurnaceWindow(this, furnace.metadata as FurnaceMetadata)

    init {
        addActor(PlayerInventoryWindow(this, screen.player[playerM].inventory).apply { setPosition(WORLD_WIDTH / 2, WORLD_HEIGHT / 3, Align.center) })
        addActor(window)
    }

    override fun refreshInventory(block: Block) {
        val meta = block.metadata as? FurnaceMetadata
        if (meta == null) screen.popPanel() // Furnace probably got destroyed
        else window.refresh(meta)
    }

    override fun updateNetInventory() = window.updateNetInventory(furnace.metadata as FurnaceMetadata)
}