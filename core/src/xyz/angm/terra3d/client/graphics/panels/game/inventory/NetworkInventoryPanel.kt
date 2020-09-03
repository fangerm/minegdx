package xyz.angm.terra3d.client.graphics.panels.game.inventory

import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.common.world.Block

/** A panel whose inventory is bound to a block. Anytime an inventory in this type of panel gets modified,
 * the block given to the constructor will be sent to the server.
 * Usually, the inventory resides in the block's metadata.
 *
 * @param block The block to update on inventory change. */
abstract class NetworkInventoryPanel(private val screen: GameScreen, private val block: Block) : InventoryPanel(screen) {

    /** Called before the block is synced with the server, use it to change the inventories as needed. */
    abstract fun updateNetInventory()

    override fun itemLeftClicked(actor: ItemGroup.GroupedItemActor) {
        super.itemLeftClicked(actor)
        syncNetwork()
    }

    override fun itemRightClicked(actor: ItemGroup.GroupedItemActor) {
        super.itemRightClicked(actor)
        syncNetwork()
    }

    override fun itemShiftClicked(actor: ItemGroup.GroupedItemActor) {
        super.itemShiftClicked(actor)
        syncNetwork()
    }

    private fun syncNetwork() {
        updateNetInventory()
        screen.client.send(block)
    }
}