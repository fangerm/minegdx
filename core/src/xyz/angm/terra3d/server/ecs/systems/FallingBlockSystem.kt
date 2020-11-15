/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 7:07 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server.ecs.systems

import xyz.angm.rox.Entity
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.rox.systems.IteratingSystem
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.components.RemoveFlag
import xyz.angm.terra3d.common.ecs.components.specific.FallingBlockComponent
import xyz.angm.terra3d.common.ecs.fallingBlock
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.ecs.velocity
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.NOTHING
import xyz.angm.terra3d.server.world.World

/** System that updates falling blocks. */
class FallingBlockSystem(private val world: World) : IteratingSystem(allOf(FallingBlockComponent::class)) {

    /** Place the block if it is on solid ground and not falling anymore. */
    override fun process(entity: Entity, delta: Float) {
        val velocity = entity[velocity]
        if (velocity.y > -0.1f) {
            val ty = entity[fallingBlock].block
            val block = Block(ty, IntVector3(entity[position]))
            world.setBlock(block, true)
            RemoveFlag.flag(engine, entity)
        }
    }

    /** Checks if a block needs to be falling when another is removed.
     * @param position The position of the block *that was removed*; below the one to check for falling. */
    fun maybeFall(position: IntVector3) {
        position.y++
        val above = world.getBlock(position) ?: return
        blockPlaced(above, false)
    }

    /** Call when a new block was placed.
     * @param belowExists If there is a block below the new block. */
    fun blockPlaced(block: Block, belowExists: Boolean) {
        if (!belowExists) {
            FallingBlockComponent.maybeFall(engine, block.type, block.position) {
                world.setBlock(Block(NOTHING, block.position), true)
            }
        }
    }
}
