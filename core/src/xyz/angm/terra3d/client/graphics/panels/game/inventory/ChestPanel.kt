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
import xyz.angm.terra3d.client.graphics.windows.GenericInventoryWindow
import xyz.angm.terra3d.client.graphics.windows.PlayerInventoryWindow
import xyz.angm.terra3d.client.resources.soundPlayer
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.metadata.ChestMetadata
import xyz.angm.terra3d.common.world.Block

/** Panel for a chest's inventory. */
class ChestPanel(screen: GameScreen, private val chest: Block) : NetworkInventoryPanel(screen, chest) {

    private val chestInv = Inventory(54)

    init {
        addActor(PlayerInventoryWindow(this, screen.player[playerM].inventory).apply { setPosition(WORLD_WIDTH / 2, WORLD_HEIGHT / 3, Align.center) })
        val blockInv = (chest.metadata as ChestMetadata).inventory
        copyToInv(blockInv)
        addActor(GenericInventoryWindow(this, chestInv))
        soundPlayer.playSound3D("random/chestopen", chest.position.toV3())
    }

    private fun copyToInv(blockInv: Inventory) {
        for (i in 0 until chestInv.size) chestInv[i] = blockInv[i]
    }

    override fun refreshInventory(block: Block) {
        val meta = block.metadata as? ChestMetadata
        if (meta == null) screen.popPanel() // Chest probably got destroyed
        else copyToInv(meta.inventory)
    }

    override fun updateNetInventory() {
        (chest.metadata as ChestMetadata).inventory = chestInv
    }

    override fun dispose() {
        soundPlayer.playSound3D("random/chestclosed", chest.position.toV3())
    }
}