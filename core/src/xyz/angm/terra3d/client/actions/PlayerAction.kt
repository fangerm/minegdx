package xyz.angm.terra3d.client.actions

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.ObjectMap
import ktx.ashley.get
import ktx.collections.*
import xyz.angm.terra3d.client.graphics.panels.game.ChatPanel
import xyz.angm.terra3d.client.graphics.panels.game.PausePanel
import xyz.angm.terra3d.client.graphics.panels.game.inventory.PlayerInventoryPanel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.ecs.velocity

/** An action represents a function to be executed when the player presses a key.
 * @property type The internal name for an action, ex. 'walkLeft'
 * @property name The name shown to the player, ex. 'Walk Left'
 * @property keyDown Function to be executed when the key is pressed down
 * @property keyUp Function to be executed when the key is released, can be null (which will be an empty function) */
data class PlayerAction(
    val type: String,
    val name: String,
    val keyDown: (GameScreen) -> Unit,
    val keyUp: (GameScreen) -> Unit = {}
)

/** The object that contains all actions and allows retrieving them. */
object PlayerActions {

    private val actions = ObjectMap<String, PlayerAction>()

    init {
        addAction(PlayerAction("walkForward", "Walk Forward", { it.player[velocity]!!.x++ }, { it.player[velocity]!!.x-- }))
        addAction(PlayerAction("walkBackward", "Walk Backward", { it.player[velocity]!!.x-- }, { it.player[velocity]!!.x++ }))
        addAction(PlayerAction("walkRight", "Walk Right", { it.player[velocity]!!.z++ }, { it.player[velocity]!!.z-- }))
        addAction(PlayerAction("walkLeft", "Walk Left", { it.player[velocity]!!.z-- }, { it.player[velocity]!!.z++ }))

        addAction(PlayerAction("jump", "Jump", { it.playerInputSystem.jump() }))
        addAction(PlayerAction("sneak", "Sneak", { it.playerInputSystem.sneak(true) }, { it.playerInputSystem.sneak(false) }))
        addAction(PlayerAction("sprint", "Sprint", { it.playerInputSystem.sprint(true) }, { it.playerInputSystem.sprint(false) }))
        addAction(PlayerAction("dropItem", "Drop Held Item", { it.playerInputSystem.dropItem() }))

        addAction(PlayerAction("debugMenu", "Toggle Debug Menu", { it.gameplayPanel.toggleDebugInfo() }))
        addAction(PlayerAction("pauseMenu", "Open Pause Menu", { it.pushPanel(PausePanel(it)) }))
        addAction(PlayerAction("chat", "Chat", { it.pushPanel(ChatPanel(it)) }))
        addAction(PlayerAction("openInventory", "Open Inventory", { it.pushPanel(PlayerInventoryPanel(it)) }))
        addAction(PlayerAction("fullscreen", "Toggle Fullscreen", {
            if (Gdx.graphics.isFullscreen) Gdx.graphics.setWindowedMode(Gdx.graphics.displayMode.width, Gdx.graphics.displayMode.height)
            else Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
        }))

        for (i in 1..9) {
            addAction(PlayerAction("hotbarSlot$i", "Select Hotbar Item $i", {
                it.player[playerM]!!.inventory.hotbarPosition = i - 1
                it.gameplayPanel.updateHotbarSelector(it.player[playerM]!!.inventory.hotbarPosition)
            }))
        }
    }

    /** Get an action. */
    operator fun get(type: String): PlayerAction? = actions[type]

    private fun addAction(action: PlayerAction) {
        actions[action.type] = action
    }
}
