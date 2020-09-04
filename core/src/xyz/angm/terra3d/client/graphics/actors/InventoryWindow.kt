package xyz.angm.terra3d.client.graphics.actors

import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisWindow
import xyz.angm.terra3d.client.graphics.panels.game.inventory.InventoryPanel
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.metadata.FurnaceMetadata


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
class GenericInventoryWindow(panel: InventoryPanel, inventory: Inventory) : InventoryWindow(panel, "inventory") {

    init {
        add(ItemGroup(this, inventory, row = inventory.size / 9, column = 9))
        pack()
        setPosition(WORLD_WIDTH / 2, (WORLD_HEIGHT / 3) * 2, Align.center)
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
        setPosition(WORLD_WIDTH / 2, WORLD_HEIGHT / 3, Align.center)
    }

    override fun itemShiftClicked(actor: ItemGroup.GroupedItemActor) {
        val item = actor.item ?: return
        actor.item = null
        if (actor.slot > 8) playerInv += item
        else playerInv.addToRange(item, 9 until 36) // 9 until 36 is inventory without hotbar
        super.itemShiftClicked(actor)
    }
}


class FurnaceWindow(panel: InventoryPanel, metadata: FurnaceMetadata) : InventoryWindow(panel, Item.Properties.fromIdentifier("furnace").name) {

    private val fuelItem = ItemGroup(this, Inventory(1), row = 1, column = 1)
    private val burntItem = ItemGroup(this, Inventory(1), row = 1, column = 1)
    private val resultItem = ItemGroup(this, Inventory(1), row = 1, column = 1, mutable = false)

    init {
        updateNetInventory(metadata)
        add(burntItem).padRight(50f)
        add(resultItem).row()
        add(fuelItem)
        pack()
        setPosition(WORLD_WIDTH / 2, (WORLD_HEIGHT / 3) * 2, Align.center)
    }

    fun updateNetInventory(metadata: FurnaceMetadata) {
        metadata.fuel = fuelItem.inventory[0]
        metadata.baking = burntItem.inventory[0]
        metadata.result = resultItem.inventory[0]
    }

    fun refresh(metadata: FurnaceMetadata) {
        fuelItem.inventory[0] = metadata.fuel
        burntItem.inventory[0] = metadata.baking
        resultItem.inventory[0] = metadata.result
    }
}