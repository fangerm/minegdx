package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.scenes.scene2d.ui.Image
import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.items.Inventory

/** Panel containing a 3x3 crafting grid. */
class CraftingGridPanel(screen: GameScreen) : PlayerInventoryPanel(screen) {

    override val inventoryImage =
        Image(ResourceManager.getTextureRegion("textures/gui/container/crafting_table.png", 0, 0, 352, 332))
    override val craftingGrid = ItemGroup(this, Inventory(9), rows = 3, columns = 3)
    override val craftingResult = ItemGroup(this, Inventory(1), rows = 1, columns = 1, mutable = false)

    init {
        craftingGrid.setPosition(60f, 194f)
        craftingResult.setPosition(246f, 228f)
        postSubclassInit()
    }
}