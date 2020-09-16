package xyz.angm.terra3d.server.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import xyz.angm.terra3d.server.world.BfsFluid

/** A very simple system that ticks fluids. */
class FluidSystem(private val alg: BfsFluid) : EntitySystem() {

    var delta = 0f

    override fun update(deltaT: Float) {
        delta += deltaT
        if (delta > 0.4f) {
            alg.tick()
            delta = 0f
        }
    }
}