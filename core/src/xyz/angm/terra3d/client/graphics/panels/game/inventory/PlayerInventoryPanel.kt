package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Image
import ktx.ashley.get
import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.recipes.CraftingRecipe

/** Player inventory panel. Override for inventories containing a simple crafting interface.
 * @property inventoryImage The image for the inventory interface.
 * @property craftingGrid The crafting grid inventory. Should a subclass not need it, the position should be set off-screen.
 * @property craftingResult The crafting result inventory. Should a subclass not need it, the position should be set off-screen. */
@Suppress("LeakingThis") // Can only be an issue in the init block, where it is handled anyways
open class PlayerInventoryPanel(screen: GameScreen) : InventoryPanel(screen) {

    private val inventory = screen.player[playerM]!!.inventory

    protected open val inventoryImage =
        Image(ResourceManager.getTextureRegion("textures/gui/container/inventory.png", 0, 0, 352, 332))
    private val inventoryItems = ItemGroup(this, inventory, rows = 3, columns = 9, startOffset = 9)
    private val hotbarItems = ItemGroup(this, inventory, rows = 1, columns = 9)
    protected open val craftingGrid = ItemGroup(this, Inventory(4), rows = 2, columns = 2)
    protected open val craftingResult = ItemGroup(this, Inventory(1), rows = 1, columns = 1, mutable = false)

    init {
        // This init block should only act if the class is not being subclassed. If it is, the subclass should handle init
        // TODO: This is NOT how initialization is supposed to work
        if (this::class == PlayerInventoryPanel::class) {
            craftingGrid.setPosition(196f, 228f)
            craftingResult.setPosition(308f, 244f)
            postSubclassInit()
        }
    }

    /** Call this after initialization in a subclass; adds all actors part of PlayerInventory and their position */
    protected fun postSubclassInit() {
        add(inventoryImage)
            .size(inventoryImage.prefWidth / ResourceManager.pack.sizeMod, inventoryImage.prefHeight / ResourceManager.pack.sizeMod)
        addActor(inventoryItems)
        addActor(hotbarItems)
        addActor(craftingGrid)
        addActor(craftingResult)

        Gdx.app.postRunnable {
            // The position of actors in the table only get calculated at the first draw call
            val x = inventoryImage.x
            val y = inventoryImage.y
            inventoryItems.setPosition(x + 16, y + 60)
            hotbarItems.setPosition(x + 16, y + 16)
            craftingGrid.setPosition(x + craftingGrid.x, y + craftingGrid.y)
            craftingResult.setPosition(x + craftingResult.x, y + craftingResult.y)
        }
    }

    override fun itemLeftClicked(actor: ItemGroup.GroupedItemActor) {
        if (actor.group == craftingResult && craftingResult.inventory[0] != null) {
            for (i in 0 until craftingGrid.inventory.size) craftingGrid.inventory.subtractFromSlot(i, 1)
        }
        super.itemLeftClicked(actor)
        updateCraftingGrid(actor)
    }

    override fun itemRightClicked(actor: ItemGroup.GroupedItemActor) {
        if (actor.group == craftingResult) itemLeftClicked(actor)
        else super.itemRightClicked(actor)
        updateCraftingGrid(actor)
    }

    override fun itemShiftClicked(actor: ItemGroup.GroupedItemActor) {
        if (actor.group == craftingResult) {
            var match = CraftingRecipe.matchAll(craftingGrid.inventory)
            while (match != null) {
                for (i in 0 until craftingGrid.inventory.size) craftingGrid.inventory.subtractFromSlot(i, 1)
                inventory += match
                match = CraftingRecipe.matchAll(craftingGrid.inventory)
            }
        } else {
            updateCraftingGrid(actor)
            val item = actor.item ?: return
            actor.item = null
            if (actor.slot > 8 || actor.group.inventory != inventory) inventory += item
            else inventory.addToRange(item, 9 until 36) // 9 until 36 is inventory without hotbar
        }
        updateCraftingGrid(actor)
    }

    private fun updateCraftingGrid(actor: ItemGroup.GroupedItemActor) {
        if (actor.group == craftingGrid || actor.group == craftingResult) {
            craftingResult.inventory[0] = CraftingRecipe.matchAll(craftingGrid.inventory)
        }
    }
}