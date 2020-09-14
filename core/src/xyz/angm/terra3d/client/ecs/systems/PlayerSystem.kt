package xyz.angm.terra3d.client.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.math.Vector3
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import xyz.angm.terra3d.client.graphics.panels.game.DeathPanel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.client.resources.soundPlayer
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.*
import xyz.angm.terra3d.common.ecs.components.PositionComponent
import xyz.angm.terra3d.common.ecs.components.RemoveFlag
import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent

/** Frequency of sending the player entity to the server for updating. */
private const val NETWORK_SYNC_TIME = 0.1f

/** Base multiplier of the player's hunger. */
private const val HUNGER_TIME_MULTI = 0.05f

/** A system used for updating state of the local player.
 * Handles all various small tasks not covered by other systems. */
class PlayerSystem(
    private val screen: GameScreen,
    private val player: Entity
) : EntitySystem() {

    private val pPosition = player[position]!!
    private val pDirection = player[direction]!!
    private val localPlayerC = player[localPlayer]!!
    private val playerC = player[playerM]!!
    private val pRender = player[playerRender]!!
    private val pHealth = player[health]!!

    private val allDroppedItems = allOf(ItemComponent::class, PositionComponent::class).exclude(RemoveFlag::class).get()
    private var timeSinceSync = 0f
    private var hungerLeft = 1f // 1 hunger point is removed when this reaches 0
    private var starveTime = 1f // Time until the player takes starving damage again

    /** Updates various things; mostly positions.
     * @param delta Unused; time since last call. */
    override fun update(delta: Float) {
        updatePositions(delta)
        updateHunger(delta)
        checkPickedUpItems()
        checkBelowWorld()
        checkDeath()

        timeSinceSync += delta
        if (timeSinceSync > NETWORK_SYNC_TIME) {
            timeSinceSync = 0f
            player[network]!!.needsSync = true
        }
    }

    private fun updatePositions(delta: Float) {
        // Update player position
        pDirection.set(screen.cam.direction)

        // Update position of the block looked at
        localPlayerC.blockLookingAt = screen.world.getBlockRaycast(pPosition, pDirection, false) ?: defaultSelectorPosition
        ResourceManager.models.updateDamageModelPosition(localPlayerC.blockLookingAt.toV3(tmpV), localPlayerC.blockHitPercent)

        // Update camera position
        screen.cam.position.set(pPosition)
        screen.cam.update()

        // Update camera FOV
        screen.cam.fieldOfView -= (screen.cam.fieldOfView - localPlayerC.fov) * 10f * delta

        // Update rendering-related positions
        pRender.blockSelector.transform.setToTranslation(localPlayerC.blockLookingAt.toV3(tmpV).add(0.5f))
        soundPlayer.updateListenerPosition(pPosition, pDirection)
    }

    private fun updateHunger(delta: Float) {
        when {
            (playerC.hunger == 0) -> {
                starveTime -= delta
                if (starveTime < 0f) {
                    pHealth.health--
                    playerHurt()
                    starveTime = 1f
                }
            }
            (hungerLeft < 0f) -> {
                playerC.hunger--
                hungerLeft = 1f
            }
            else -> hungerLeft -= delta * HUNGER_TIME_MULTI
        }
    }

    private fun checkPickedUpItems() {
        if (playerC.isDead) return
        engine.getEntitiesFor(allDroppedItems).forEach {
            if (it[item]!!.pickupTimeout <= 0f && tmpV.set(it[position]!!).dst2(tmpV2.set(pPosition)) < 4f) {
                playerC.inventory += it[item]!!.item
                RemoveFlag.flag(it)
            }
        }
    }

    private fun checkBelowWorld() {
        if (pPosition.y < 0f) {
            pHealth.health--
            playerHurt()
        }
    }

    private fun checkDeath() {
        if (pHealth.health <= 0 && !playerC.isDead) {
            playerHurt()
            playerC.inventory.dropAll(engine, pPosition)
            playerC.isDead = true
            screen.pushPanel(DeathPanel(screen))
        }
    }

    private fun playerHurt() {
        soundPlayer.playSound("random/hurt")
        screen.inputHandler.playerHurt()
    }

    private companion object {
        private val tmpV = Vector3()
        private val tmpV2 = Vector3()
        private val defaultSelectorPosition = IntVector3(0, -10000, 0)
    }
}