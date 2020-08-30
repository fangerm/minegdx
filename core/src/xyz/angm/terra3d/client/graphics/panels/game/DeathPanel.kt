package xyz.angm.terra3d.client.graphics.panels.game

import ktx.actors.onClick
import ktx.actors.plus
import ktx.ashley.get
import ktx.scene2d.label
import ktx.scene2d.table
import ktx.scene2d.textButton
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.ecs.health
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.ecs.position

/** Shown when the player dies. */
class DeathPanel(screen: GameScreen) : Panel(screen) {

    init {
        background = skin.getDrawable("red-transparent")

        this + table {
            label(I18N["death.message"], style = "default-48pt") { it.pad(75f).row() }

            textButton(I18N["death.respawn"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).row()
                onClick {
                    screen.player[position]!!.set(screen.player[playerM]!!.spawnPosition)
                    screen.player[health]!!.restore()
                    screen.player[playerM]!!.isDead = false
                    screen.player[playerM]!!.hunger = 20
                    screen.popAllPanels()
                }
            }

            setFillParent(true)
        }
    }
}