/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/29/20, 3:32 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.ecs.systems

import com.badlogic.gdx.math.Vector3
import xyz.angm.rox.Entity
import xyz.angm.rox.systems.EntitySystem
import xyz.angm.terra3d.client.actions.Event
import xyz.angm.terra3d.client.actions.EventContext
import xyz.angm.terra3d.client.actions.PlayerInteractions
import xyz.angm.terra3d.client.ecs.components.FOV
import xyz.angm.terra3d.client.ecs.components.SPRINT_FOV
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.soundPlayer
import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent
import xyz.angm.terra3d.common.ecs.direction
import xyz.angm.terra3d.common.ecs.localPlayer
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.NOTHING

/** Sprinting speed multiplier. */
const val SPRINT_SPEED_MULTIPLIER = 1.5f

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
    private val localPlayerC = player[localPlayer]
    private val playerC = player[playerM]
    private var breakSound = 0
    private var breakType = 0

    override fun update(delta: Float) = inputHandler.update(delta)

    /** Called when the player is holding the left mouse button. */
    internal fun leftHeld(delta: Float) {
        val block = screen.world.getBlock(localPlayerC.blockLookingAt)
        checkSound(block)
        block ?: return

        localPlayerC.blockHitTime += delta
        val breakTime = block.properties!!.block!!.getBreakTime(playerC.inventory.heldItem?.properties?.tool)
        localPlayerC.blockHitPercent = (localPlayerC.blockHitTime / breakTime) * 100

        if (localPlayerC.blockHitPercent > 100f) screen.world.setBlock(Block(NOTHING, localPlayerC.blockLookingAt))
    }

    /** Called when the player lets go of the left mouse button */
    internal fun leftUp() {
        if (breakSound != 0) {
            soundPlayer.stopPlaying(breakSound)
            breakSound = 0
            breakType = 0
        }
    }

    /** Called when the player is holding the left mouse button to make sure sound stays correct. */
    internal fun checkSound(block: Block?) {
        if (block == null && breakSound != 0) {
            soundPlayer.stopPlaying(breakSound)
            breakSound = 0
            breakType = 0
        } else if (block != null && block.type != breakType) {
            soundPlayer.stopPlaying(breakSound)
            breakSound = soundPlayer.playLooping(block.properties?.block?.hitSound ?: return, block.position.toV3(), 0.4f)
            breakType = block.type
        }
    }

    /** Called when the player right-clicks. */
    internal fun rightClick() {
        val blockLookedAt = screen.world.getBlock(localPlayerC.blockLookingAt)
        val context = EventContext(screen = screen, item = playerC.inventory.heldItem, block = blockLookedAt)

        if (blockLookedAt != null) {
            if (!physicsSystem.sneaking) { // Don't trigger block events when sneaking
                val blockE = PlayerInteractions.get(blockLookedAt, Event.BLOCK_CLICKED)
                if (blockE?.invoke(context) != null) return // Block event triggered, done here
            }

            // Try placing a block
            val block = playerC.inventory.heldItem ?: return
            if (screen.world.updateBlockRaycast(player[position], player[direction], block))
                playerC.inventory.subtractFromHeldItem(1)
        }

        // Only option left is ITEM_CLICKED, try that
        PlayerInteractions.get(playerC.inventory.heldItem ?: return, Event.ITEM_CLICKED)?.invoke(context)
    }

    /** Causes the player to sprint or stop sprinting.
     * @param sprint If the player should sprint (else will stop sprinting) */
    fun sprint(sprint: Boolean) {
        physicsSystem.sprinting = sprint
        player[localPlayer].fov = if (sprint) SPRINT_FOV else FOV
    }

    /** Causes the player to jump. */
    fun jump() = physicsSystem.jump()

    /** Causes the player to sneak.
     * @param sneak If the player should sneak (else they will stop sneaking) */
    fun sneak(sneak: Boolean) = physicsSystem.sneak(sneak)

    /** Causes the player to drop the item they are currently holding. */
    fun dropItem() {
        ItemComponent.create(engine, playerC.inventory.heldItem?.copy() ?: return, tmpV.set(player[position]), 4f)
        playerC.inventory.subtractFromHeldItem(Int.MAX_VALUE)
    }
}