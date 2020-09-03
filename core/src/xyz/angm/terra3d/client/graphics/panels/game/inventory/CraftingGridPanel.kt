package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.utils.Align
import ktx.ashley.get
import xyz.angm.terra3d.client.graphics.actors.CraftingWindow
import xyz.angm.terra3d.client.graphics.actors.PlayerInventoryWindow
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH
import xyz.angm.terra3d.common.ecs.playerM

/** Panel containing a 3x3 crafting grid. */
class CraftingGridPanel(screen: GameScreen) : InventoryPanel(screen) {
    init {
        addActor(PlayerInventoryWindow(this, screen.player[playerM]!!.inventory).apply { setPosition(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, Align.center) })
        addActor(CraftingWindow(this, 3))
    }
}