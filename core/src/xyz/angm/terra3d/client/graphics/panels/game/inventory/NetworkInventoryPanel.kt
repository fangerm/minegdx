/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.game.inventory

import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.common.networking.BlockUpdate
import xyz.angm.terra3d.common.world.Block

/** A panel whose inventory is bound to a block. Anytime an inventory in this type of panel gets modified,
 * the block given to the constructor will be sent to the server.
 * Usually, the inventory resides in the block's metadata.
 *
 * @param block The block to update on inventory change. */
abstract class NetworkInventoryPanel(protected val screen: GameScreen, private val block: Block) : InventoryPanel(screen) {

    private val listener: (Any) -> Unit

    init {
        listener = {
            if (it is BlockUpdate && it.position == block.position) {
                refreshInventory(it)
            }
        }
        screen.client.addListener(listener)
    }

    /** Called before the block is synced with the server, use it to change the inventories as needed. */
    abstract fun updateNetInventory()

    /** Called when the block changed on the server, should refresh. */
    abstract fun refreshInventory(block: Block)

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

    override fun dispose() {
        super.dispose()
        screen.client.removeListener(listener)
    }
}