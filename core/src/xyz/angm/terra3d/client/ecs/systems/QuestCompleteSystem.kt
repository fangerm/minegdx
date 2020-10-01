/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/1/20, 9:59 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.ecs.systems

import xyz.angm.rox.systems.IntervalSystem
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.soundPlayer
import xyz.angm.terra3d.common.ecs.components.specific.PlayerComponent
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.quest.Quests

/** A simple system that checks the player's quest status every few
 * seconds to see if they have completed their quest. */
class QuestCompleteSystem(private val screen: GameScreen) : IntervalSystem(5f) {

    override fun run() {
        val player = screen.player[playerM]
        if (player.quest.checkComplete(player.inventory)) questComplete(player)
    }

    private fun questComplete(player: PlayerComponent) {
        screen.gameplayPanel.questComplete(player.quest)
        player.quest = Quests.next(player.quest)
        soundPlayer.playSound("random/orb")
    }
}