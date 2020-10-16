/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/16/20, 7:01 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.utils.Align
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.worldHeight
import xyz.angm.terra3d.client.graphics.screens.worldWidth
import xyz.angm.terra3d.client.graphics.windows.GenericInventoryWindow
import xyz.angm.terra3d.client.graphics.windows.PlayerInventoryWindow
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.metadata.blocks.GeneratorMetadata
import xyz.angm.terra3d.common.world.Block

/** Panel for a generator's inventory. */
class GeneratorPanel(screen: GameScreen, private val generator: Block) : NetworkInventoryPanel(screen, generator) {

    private val genInv = Inventory(1)

    init {
        addActor(PlayerInventoryWindow(this, screen.player[playerM].inventory).apply { setPosition(worldWidth / 2, worldHeight / 3, Align.center) })
        val blockInv = (generator.metadata as GeneratorMetadata).fuel
        copyToInv(blockInv)
        addActor(GenericInventoryWindow(this, genInv, 1, 1, "generator"))
    }

    private fun copyToInv(blockInv: Inventory) {
        for (i in 0 until genInv.size) genInv[i] = blockInv[i]
    }

    override fun refreshInventory(block: Block) {
        val meta = block.metadata as? GeneratorMetadata
        if (meta == null) screen.popPanel() // Generator probably got destroyed
        else copyToInv(meta.fuel)
    }

    override fun updateNetInventory() {
        (generator.metadata as GeneratorMetadata).fuel = genInv
    }
}