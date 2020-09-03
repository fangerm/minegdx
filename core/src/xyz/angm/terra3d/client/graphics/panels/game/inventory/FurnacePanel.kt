package xyz.angm.terra3d.client.graphics.panels.game.inventory

import xyz.angm.terra3d.client.graphics.actors.FurnaceWindow
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.common.items.metadata.FurnaceMetadata
import xyz.angm.terra3d.common.world.Block

/** Panel for the furnace block. */
class FurnacePanel(screen: GameScreen, private val furnace: Block) : NetworkInventoryPanel(screen, furnace) {

    private val window = FurnaceWindow(this, furnace.metadata as FurnaceMetadata)

    init {
        addActor(window)
    }

    override fun updateNetInventory() = window.updateNetInventory(furnace.metadata as FurnaceMetadata)
}