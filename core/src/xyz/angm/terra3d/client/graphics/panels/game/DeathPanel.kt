package xyz.angm.terra3d.client.graphics.panels.game

import ktx.actors.onClick
import ktx.actors.plusAssign

import ktx.scene2d.scene2d
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import xyz.angm.terra3d.client.Terra3D
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.ecs.components.specific.MAX_HUNGER
import xyz.angm.terra3d.common.ecs.health
import xyz.angm.terra3d.common.ecs.localPlayer
import xyz.angm.terra3d.common.ecs.playerM

/** Shown when the player dies. */
class DeathPanel(screen: GameScreen) : Panel(screen) {

    init {
        background = skin.getDrawable("red-transparent")

        this += scene2d.visTable {
            visLabel(I18N["death.message"], style = "default-48pt") { it.pad(75f).row() }

            visTextButton(I18N["death.respawn"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).row()
                onClick {
                    screen.player[localPlayer].teleport(screen.player[playerM].spawnPosition)
                    screen.player[health].restore()
                    screen.player[playerM].hunger = MAX_HUNGER
                    Terra3D.postRunnable { // Need a slight delay to prevent item pickup race conditions...
                        screen.player[playerM].isDead = false
                        screen.popAllPanels()
                    }
                }
            }

            setFillParent(true)
        }
    }
}