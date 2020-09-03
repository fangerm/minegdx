package xyz.angm.terra3d.client.graphics.panels.game.inventory

import xyz.angm.terra3d.client.graphics.actors.GenericInventoryWindow
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.metadata.ChestMetadata
import xyz.angm.terra3d.common.world.Block

/** Panel for a chest's inventory. */
class ChestPanel(screen: GameScreen, private val chest: Block) : NetworkInventoryPanel(screen, chest) {

    private val chestInv = Inventory(54)

    init {
        val blockInv = (chest.metadata as ChestMetadata).inventory
        for (i in 0 until chestInv.size) chestInv[i] = blockInv[i]
        addActor(GenericInventoryWindow(this, chestInv))
    }

    override fun updateNetInventory() {
        (chest.metadata as ChestMetadata).inventory = chestInv
    }
}