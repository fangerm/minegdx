/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/30/20, 5:04 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.windows

import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.scene2d.scene2d
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visProgressBar
import ktx.scene2d.vis.visTable
import xyz.angm.terra3d.client.graphics.actors.ItemActor
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.ecs.components.specific.PlayerComponent

/** Window shown in the player's inventory telling the player about their
 * current quest progress. */
class QuestWindow(player: PlayerComponent) : VisWindow(I18N["quests"]) {

    init {
        add(VisLabel(player.quest.name, "vis-default")).width(400f).row()
        add(VisLabel(player.quest.description, "vis-default")).width(400f).row()

        add(scene2d.visTable {
            visLabel(I18N["quests.required"], "vis-default")
            add(ItemActor(player.quest.item)).pad(10f)
            visProgressBar(max = player.quest.item.amount.toFloat()) {
                value = player.inventory.count(player.quest.item).toFloat()
                width = 300f
            }
        }).width(400f).pad(10f)

        pack()
        setPosition(WORLD_WIDTH, WORLD_HEIGHT)
    }
}