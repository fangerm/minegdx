/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/29/20, 4:35 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.utils.Align
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.worldHeight
import xyz.angm.terra3d.client.graphics.screens.worldWidth
import xyz.angm.terra3d.client.graphics.windows.GenericProcessingWindow
import xyz.angm.terra3d.client.graphics.windows.PlayerInventoryWindow
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.items.metadata.blocks.GenericProcessingMachineMetadata
import xyz.angm.terra3d.common.world.Block

/** Panel for a generic machine like electric furnaces. */
class GenericProcessingPanel(screen: GameScreen, private val machine: Block, name: String) : NetworkInventoryPanel(screen, machine) {

    private val window = GenericProcessingWindow(this, machine.metadata as GenericProcessingMachineMetadata, name)

    init {
        addActor(PlayerInventoryWindow(this, screen.player[playerM].inventory).apply { setPosition(worldWidth / 2, worldHeight / 3, Align.center) })
        addActor(window)
    }

    override fun refreshInventory(block: Block) {
        val meta = block.metadata as? GenericProcessingMachineMetadata
        if (meta == null) screen.popPanel() // Machine probably got destroyed
        else window.refresh(meta)
    }

    override fun updateNetInventory() = window.updateNetInventory(machine.metadata as GenericProcessingMachineMetadata)
}