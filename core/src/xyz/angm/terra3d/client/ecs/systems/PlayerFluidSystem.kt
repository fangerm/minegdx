/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/30/20, 12:20 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.ecs.systems

import xyz.angm.rox.systems.EntitySystem
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.soundPlayer
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.health
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.world.TYPE

/** A system used for checking the player's interactions
 * with fluids like water. */
class PlayerFluidSystem(
    private val screen: GameScreen,
    private val physicsSystem: PlayerPhysicsSystem,
) : EntitySystem() {

    private val tmpIV = IntVector3()
    private val lavaId = Item.Properties.fromIdentifier("lava").type
    private var lavaInterval = 0f

    override fun update(delta: Float) {
        val position = screen.player[position]
        val fluidFeet = screen.world.getFluidLevel(tmpIV.set(position))

        position.y += PLAYER_HEIGHT * 2f
        val fluidHead = screen.world.getFluidLevel(tmpIV.set(position))
        position.y -= PLAYER_HEIGHT * 2f

        physicsSystem.inFluid = fluidFeet > 0
        screen.gameplayPanel.inFluid = fluidHead > 0

        checkLava(delta)
    }

    private fun checkLava(delta: Float) {
        val position = screen.player[position]
        val blockFeet = screen.world.getBlockRaw(tmpIV.set(position)) and TYPE

        if (blockFeet == lavaId) {
            lavaInterval += delta
            if (lavaInterval > 0.5f) {
                screen.player[health].health -= 10
                soundPlayer.playSound("entity/player/hurt/fire_hurt1")
                screen.inputHandler.playerHurt()
                lavaInterval = 0f
            }
        }
    }
}
