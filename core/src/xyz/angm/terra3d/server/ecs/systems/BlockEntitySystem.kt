/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 9:56 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server.ecs.systems

import com.badlogic.gdx.utils.ObjectMap
import ktx.collections.set
import xyz.angm.rox.Engine
import xyz.angm.rox.Entity
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.rox.IteratingSystem
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.SyncChannel
import xyz.angm.terra3d.common.ecs.block
import xyz.angm.terra3d.server.ecs.components.BlockComponent
import xyz.angm.terra3d.server.world.World

/** A system that handles and ticks any block entities.
 * See [xyz.angm.terra3d.server.ecs.components.BlockComponent] for more info about block entities. */
class BlockEntitySystem(private val world: World) : IteratingSystem(allOf(BlockComponent::class)) {

    private var tickCount = 0
    private val blockEntities = ObjectMap<IntVector3, Entity>()

    /** Increase tick count every update. */
    override fun update(delta: Float) {
        super.update(delta)
        tickCount++
    }

    /** Tick any entity that need to be. */
    override fun process(entity: Entity, delta: Float) {
        val blockC = entity[block]
        if (shouldTick(blockC)) blockC(world, world.getBlock(blockC.blockPosition) ?: return)
    }

    private fun shouldTick(block: BlockComponent) = tickCount % block.tickInterval == 0

    /** Creates a new block entity. All entities should be created with this helper. */
    fun createBlockEntity(engine: SyncChannel<Engine>, component: BlockComponent) {
        engine {
            blockEntities[component.blockPosition] = entity { add(component) }
        }
    }

    /** Removes a block entity at the given position. All block entities should be removed with this helper. */
    fun removeBlockEntity(engine: SyncChannel<Engine>, position: IntVector3) {
        engine {
            val entity = blockEntities[position] ?: return@engine
            blockEntities.remove(position)
            remove(entity)
        }
    }
}