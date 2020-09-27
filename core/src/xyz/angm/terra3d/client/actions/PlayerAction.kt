/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.actions

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.ObjectMap
import ktx.collections.*
import xyz.angm.terra3d.client.graphics.panels.game.ChatPanel
import xyz.angm.terra3d.client.graphics.panels.game.PausePanel
import xyz.angm.terra3d.client.graphics.panels.game.inventory.PlayerInventoryPanel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.ecs.velocity

/** An action represents a function to be executed when the player presses a key.
 * @property type The internal name for an action, ex. 'walkLeft'
 * @property keyDown Function to be executed when the key is pressed down
 * @property keyUp Function to be executed when the key is released, can be null (which will be an empty function) */
data class PlayerAction(
    val type: String,
    val keyDown: (GameScreen) -> Unit,
    val keyUp: (GameScreen) -> Unit
)

/** The object that contains all actions and allows retrieving them. */
object PlayerActions {

    val actions = ObjectMap<String, PlayerAction>()

    init {
        fun add(name: String, down: (GameScreen) -> Unit, up: (GameScreen) -> Unit) {
            actions[name] = PlayerAction(name, down, up)
        }

        fun add(name: String, down: (GameScreen) -> Unit) = add(name, down, {})

        add("walkForward", { it.player[velocity].x++ }, { it.player[velocity].x-- })
        add("walkBackward", { it.player[velocity].x-- }, { it.player[velocity].x++ })
        add("walkRight", { it.player[velocity].z++ }, { it.player[velocity].z-- })
        add("walkLeft", { it.player[velocity].z-- }, { it.player[velocity].z++ })

        add("jump") { it.playerInputSystem.jump() }
        add("sneak", { it.playerInputSystem.sneak(true) }, { it.playerInputSystem.sneak(false) })
        add("sprint", { it.playerInputSystem.sprint(true) }, { it.playerInputSystem.sprint(false) })
        add("dropItem") { it.playerInputSystem.dropItem() }

        add("debugInfo") { it.gameplayPanel.toggleDebugInfo() }
        add("pauseMenu") { it.pushPanel(PausePanel(it)) }
        add("onlinePlayers", { it.gameplayPanel.toggleOnlinePlayers() }, { it.gameplayPanel.toggleOnlinePlayers() })

        add("chat") { it.pushPanel(ChatPanel(it)) }
        add("openInventory") { it.pushPanel(PlayerInventoryPanel(it)) }
        add("fullscreen") {
            if (Gdx.graphics.isFullscreen) Gdx.graphics.setWindowedMode(Gdx.graphics.displayMode.width, Gdx.graphics.displayMode.height)
            else Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
        }

        for (i in 1..9) {
            add("hotbarSlot$i") {
                it.player[playerM].inventory.hotbarPosition = i - 1
                it.gameplayPanel.updateHotbarSelector(it.player[playerM].inventory.hotbarPosition)
            }
        }
    }

    /** Get an action. */
    operator fun get(type: String): PlayerAction? = actions[type]
}
