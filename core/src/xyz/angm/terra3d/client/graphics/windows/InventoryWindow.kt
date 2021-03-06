/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/29/20, 4:35 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.windows

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.scene2d.scene2d
import ktx.scene2d.vis.visProgressBar
import xyz.angm.terra3d.client.graphics.actors.ItemActor
import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.panels.game.inventory.InventoryPanel
import xyz.angm.terra3d.client.graphics.screens.worldHeight
import xyz.angm.terra3d.client.graphics.screens.worldWidth
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.metadata.blocks.FurnaceMetadata
import xyz.angm.terra3d.common.items.metadata.blocks.GenericProcessingMachineMetadata


abstract class InventoryWindow(protected val panel: InventoryPanel, name: String) : VisWindow(I18N[name]) {

    /** When a slot in an inventory is left clicked */
    open fun itemLeftClicked(actor: ItemGroup.GroupedItemActor) = panel.itemLeftClicked(actor)

    /** When a slot in an inventory is right clicked */
    open fun itemRightClicked(actor: ItemGroup.GroupedItemActor) = panel.itemRightClicked(actor)

    /** When a slot in an inventory is left clicked with shift held */
    open fun itemShiftClicked(actor: ItemGroup.GroupedItemActor) = panel.itemShiftClicked(actor)

    /** When a slot is hovered */
    open fun itemHovered(actor: ItemActor) = panel.itemHovered(actor)

    /** When a slot is no longer hovered */
    open fun itemLeft() = panel.itemLeft()
}


/** Window containing some random inventory, like a chest. */
class GenericInventoryWindow(
    panel: InventoryPanel,
    inventory: Inventory,
    row: Int = inventory.size / 9,
    column: Int = 9,
    name: String = "inventory"
) : InventoryWindow(panel, name) {

    init {
        add(ItemGroup(this, inventory, row = row, column = column))
        pack()
        setPosition(worldWidth / 2, (worldHeight / 3) * 2, Align.center)
    }
}


/** Window containing the player inventory. */
class PlayerInventoryWindow(panel: InventoryPanel, private val playerInv: Inventory) : InventoryWindow(panel, "inventory") {

    init {
        val inventoryItems = ItemGroup(this, playerInv, row = 3, column = 9, startOffset = 9)
        val hotbarItems = ItemGroup(this, playerInv, row = 1, column = 9)
        add(inventoryItems).padBottom(15f).row()
        add(hotbarItems)
        pack()
        setPosition(worldWidth / 2, worldHeight / 3, Align.center)
    }

    override fun itemShiftClicked(actor: ItemGroup.GroupedItemActor) {
        val item = actor.item ?: return
        actor.item = null
        if (actor.slot > 8) playerInv += item
        else playerInv.addToRange(item, 9 until 36) // 9 until 36 is inventory without hotbar
        super.itemShiftClicked(actor)
    }
}


class FurnaceWindow(panel: InventoryPanel, metadata: FurnaceMetadata) : InventoryWindow(panel, "furnace") {

    private val fuelItem = ItemGroup(this, Inventory(1), row = 1, column = 1)
    private val burntItem = ItemGroup(this, Inventory(1), row = 1, column = 1)
    private val resultItem = ItemGroup(this, Inventory(1), row = 1, column = 1, mutable = false)
    private val progressBar = scene2d.visProgressBar(max = 90f, step = 10f)
    private val burnBar = scene2d.visProgressBar(step = 10f, vertical = true) {
        color = Color.ORANGE
    }

    init {
        refresh(metadata)
        add(burntItem)
        add(progressBar).width(50f)
        add(resultItem).row()
        add(burnBar).height(35f).row()
        add(fuelItem)
        pad(50f, 50f, 20f, 50f)
        pack()
        setPosition(worldWidth / 2, (worldHeight / 3) * 2, Align.center)
    }

    fun updateNetInventory(metadata: FurnaceMetadata) {
        metadata.fuel = fuelItem.inventory
        metadata.baking = burntItem.inventory
        metadata.result = resultItem.inventory
    }

    fun refresh(metadata: FurnaceMetadata) {
        fuelItem.inventory = metadata.fuel
        burntItem.inventory = metadata.baking
        resultItem.inventory = metadata.result
        progressBar.value = metadata.progress.toFloat()
        if (fuelItem.inventory[0] != null) burnBar.setRange(0f, fuelItem.inventory[0]!!.properties.burnTime.toFloat())
        burnBar.value = metadata.burnTime.toFloat()
    }
}


class GenericProcessingWindow(panel: InventoryPanel, metadata: GenericProcessingMachineMetadata, name: String) : InventoryWindow(panel, name) {

    private val processingItem = ItemGroup(this, Inventory(1), row = 1, column = 1)
    private val resultItem = ItemGroup(this, Inventory(1), row = 1, column = 1, mutable = false)
    private val progressBar = scene2d.visProgressBar(max = 90f, step = 10f)

    init {
        refresh(metadata)
        add(processingItem)
        add(progressBar).width(50f)
        add(resultItem).row()
        pad(50f, 50f, 20f, 50f)
        pack()
        setPosition(worldWidth / 2, (worldHeight / 3) * 2, Align.center)
    }

    fun updateNetInventory(metadata: GenericProcessingMachineMetadata) {
        metadata.processing = processingItem.inventory
        metadata.result = resultItem.inventory
    }

    fun refresh(metadata: GenericProcessingMachineMetadata) {
        processingItem.inventory = metadata.processing
        resultItem.inventory = metadata.result
        progressBar.value = metadata.progress.toFloat()
    }
}