package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Image
import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.metadata.ChestMetadata
import xyz.angm.terra3d.common.world.Block

/** Panel for a chest's inventory. */
class ChestPanel(screen: GameScreen, private val chest: Block) : NetworkInventoryPanel(screen, chest) {

    override val inventoryImage = Image(ResourceManager.getTextureRegion("textures/gui/container/generic_54.png", 0, 0, 352, 444))
    private val chestInv = ItemGroup(this, Inventory(54), rows = 6, columns = 9)

    init {
        val blockInv = (chest.metadata as ChestMetadata).inventory
        for (i in 0 until chestInv.inventory.size) {
            chestInv.inventory[i] = blockInv[i]
        }

        postSubclassInit()

        removeActor(craftingGrid)
        removeActor(craftingResult)
        addActor(chestInv)
        chestInv.setPosition(16f, 196f)
        Gdx.app.postRunnable {
            // The position of actors in the table only get calculated at the first draw call
            chestInv.setPosition(inventoryImage.x + chestInv.x, inventoryImage.y + chestInv.y)
        }
    }

    override fun updateNetInventory() {
        (chest.metadata as ChestMetadata).inventory = chestInv.inventory
    }
}