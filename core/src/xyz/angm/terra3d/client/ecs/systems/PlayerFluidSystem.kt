package xyz.angm.terra3d.client.ecs.systems

import xyz.angm.rox.EntitySystem
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.position

/** A system used for checking the player's interactions
 * with fluids like water. */
class PlayerFluidSystem(
    private val screen: GameScreen,
    private val physicsSystem: PlayerPhysicsSystem,
) : EntitySystem() {

    private val tmpIV = IntVector3()

    override fun update(delta: Float) {
        val position = screen.player[position]
        position.y -= PLAYER_HEIGHT
        val fluidFeet = screen.world.getFluidLevel(tmpIV.set(position))

        position.y += PLAYER_HEIGHT * 2f
        val fluidHead = screen.world.getFluidLevel(tmpIV.set(position))
        position.y -= PLAYER_HEIGHT

        physicsSystem.inFluid = fluidFeet > 0
        screen.gameplayPanel.inFluid = fluidHead > 0
    }
}
