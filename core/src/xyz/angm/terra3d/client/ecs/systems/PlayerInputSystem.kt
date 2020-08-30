package xyz.angm.terra3d.client.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.math.Vector3
import ktx.ashley.get
import xyz.angm.terra3d.client.actions.Event
import xyz.angm.terra3d.client.actions.EventContext
import xyz.angm.terra3d.client.actions.PlayerInteractions
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.common.ecs.*
import xyz.angm.terra3d.common.ecs.components.set
import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.NOTHING

/** Sprinting speed multiplier. */
const val SPRINT_SPEED_MULTIPLIER = 1.5f

/** How much the camera FOV changes when sprinting */
const val SPRINT_FOV_MULTIPLIER = 1.08f

/** System responsible for handling player input.
 * Does not use Ashley's update system for the most part; input is event-driven.
 * Since it is event-driven, it does not depend on the engine. The System parent class is purely semantic.
 *
 * @param screen The screen active.
 * @param player The player player used to store information. Should already be part of the ECS engine. */
class PlayerInputSystem(
    private val screen: GameScreen,
    private val player: Entity,
    private val physicsSystem: PlayerPhysicsSystem,
    private val inputHandler: PlayerInputHandler
) : EntitySystem() {

    private val tmpV = Vector3()
    private val localPlayerC = player[localPlayer]!!
    private val playerC = player[playerM]!!

    override fun update(delta: Float) = inputHandler.update(delta)

    /** Called when the player is holding the left mouse button. */
    internal fun leftClick(delta: Float) {
        val block = screen.world.getBlock(localPlayerC.blockLookingAt) ?: return

        localPlayerC.blockHitTime += delta
        val breakTime = block.properties!!.block!!.getBreakTime(playerC.inventory.heldItem?.properties?.tool)
        localPlayerC.blockHitPercent = (localPlayerC.blockHitTime / breakTime) * 100

        if (localPlayerC.blockHitPercent > 100f) screen.world.setBlock(Block(NOTHING, localPlayerC.blockLookingAt))
    }

    /** Called when the player right-clicks. */
    internal fun rightClick() {
        val blockLookedAt = screen.world.getBlock(localPlayerC.blockLookingAt)
        val context = EventContext(screen = screen, item = playerC.inventory.heldItem, block = blockLookedAt)

        if (blockLookedAt == null)
            PlayerInteractions.getListener(playerC.inventory.heldItem ?: return, Event.ITEM_CLICKED)?.invoke(context)
        else {
            if (PlayerInteractions.getListener(blockLookedAt, Event.BLOCK_CLICKED)?.invoke(context) != null) return

            val block = playerC.inventory.heldItem ?: return
            if (block.properties.isBlock &&
                screen.world.updateBlockRaycast(player[position]!!, player[direction]!!, block)
            )
                playerC.inventory.subtractFromHeldItem(1)
        }
    }

    /** Causes the player to sprint or stop sprinting.
     * @param sprint If the player should sprint (else will stop sprinting) */
    fun sprint(sprint: Boolean) {
        if (sprint) {
            player[velocity]!!.speedModifier *= SPRINT_SPEED_MULTIPLIER
            screen.cam.fieldOfView *= SPRINT_FOV_MULTIPLIER
        } else {
            player[velocity]!!.speedModifier /= SPRINT_SPEED_MULTIPLIER
            screen.cam.fieldOfView /= SPRINT_FOV_MULTIPLIER
        }
    }

    /** Causes the player to jump. */
    fun jump() = physicsSystem.jump()

    /** Causes the player to sneak.
     * @param sneak If the player should sneak (else they will stop sneaking) */
    fun sneak(sneak: Boolean) = physicsSystem.sneak(sneak)

    /** Causes the player to drop the item they are currently holding. */
    fun dropItem() {
        ItemComponent.create(engine, playerC.inventory.heldItem ?: return, tmpV.set(player[position]!!), 4f)
        playerC.inventory.subtractFromHeldItem(Int.MAX_VALUE)
    }
}