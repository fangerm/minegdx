package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Image
import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.metadata.FurnaceMetadata
import xyz.angm.terra3d.common.world.Block

/** Panel for the furnace block. */
class FurnacePanel(screen: GameScreen, private val furnace: Block) : NetworkInventoryPanel(screen, furnace) {

    override val inventoryImage = Image(ResourceManager.getTextureRegion("textures/gui/container/furnace.png", 0, 0, 352, 332))
    private val fuelItem = ItemGroup(this, Inventory(1), rows = 1, columns = 1)
    private val burntItem = ItemGroup(this, Inventory(1), rows = 1, columns = 1)
    private val resultItem = ItemGroup(this, Inventory(1), rows = 1, columns = 1, mutable = false)

    init {
        val metadata = furnace.metadata as FurnaceMetadata
        fuelItem.inventory[0] = metadata.fuel
        burntItem.inventory[0] = metadata.baking
        resultItem.inventory[0] = metadata.result

        postSubclassInit()

        removeActor(craftingGrid)
        removeActor(craftingResult)

        addActor(fuelItem)
        addActor(burntItem)
        addActor(resultItem)

        fuelItem.setPosition(112f, 194f)
        burntItem.setPosition(112f, 266f)
        resultItem.setPosition(232f, 230f)

        Gdx.app.postRunnable {
            // The position of actors in the table only get calculated at the first draw call
            val x = inventoryImage.x
            val y = inventoryImage.y
            fuelItem.setPosition(x + fuelItem.x, y + fuelItem.y)
            burntItem.setPosition(x + burntItem.x, y + burntItem.y)
            resultItem.setPosition(x + resultItem.x, y + resultItem.y)
        }
    }

    override fun updateNetInventory() {
        val metadata = furnace.metadata as FurnaceMetadata
        metadata.fuel = fuelItem.inventory[0]
        metadata.baking = burntItem.inventory[0]
        metadata.result = resultItem.inventory[0]
    }
}