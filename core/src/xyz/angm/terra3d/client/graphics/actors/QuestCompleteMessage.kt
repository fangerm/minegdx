/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/1/20, 10:22 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.actors

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import ktx.actors.alpha
import ktx.collections.*
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.quest.Quest

/** A simple actor showing a "quest complete!" message. */
class QuestCompleteMessage : VisTable() {

    private val queued = GdxArray<Quest>(2)

    init {
        setFillParent(true)
    }

    /** Display itself for the given quest. Will queue
     * display if already displaying; queued will show right after
     * current one finishes. */
    fun display(quest: Quest) {
        if (!children.isEmpty) {
            queued.add(quest)
            return
        }

        isVisible = true
        alpha = 0f
        add(ItemActor(quest.item)).row()
        add(VisLabel(I18N["quests.complete"])).row()
        add(VisLabel(quest.name)).row()
        add().height(350f) // Padding to move message up
        pack()

        addAction(
            Actions.sequence(
                Actions.fadeIn(0.7f, Interpolation.pow2),
                Actions.delay(4f),
                Actions.fadeOut(0.7f, Interpolation.pow2),
                Actions.visible(false),
                Actions.run {
                    clearChildren()
                    if (!queued.isEmpty) display(queued.removeIndex(0))
                }
            )
        )
    }
}