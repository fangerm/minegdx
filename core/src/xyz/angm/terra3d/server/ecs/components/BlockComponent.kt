package xyz.angm.terra3d.server.ecs.components

import com.badlogic.ashley.core.Component
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.server.world.World

/** Component for block entities.
 * A block entity is an entity for blocks that need to be 'ticked' (code executed at a tick interval), like a machine or a furnace.
 * Define a block entity for a block type in [xyz.angm.terra3d.server.world.BlockEvents];
 * position will be overridden to match the actual blocks when it is applied.
 *
 * @property tickInterval The interval between ticks, in ticks. (1 = every tick, 20 = every second)
 * @property blockPosition Position of the block that ticks. Leave at default when defining a new BlocEntity.
 * @property runner The function executed on block tick */
data class BlockComponent(
    val blockPosition: IntVector3 = IntVector3(),
    val tickInterval: Int,
    private val runner: (World, Block) -> Unit
) : Component {
    /** Executes a tick. */
    operator fun invoke(world: World, block: Block) = runner(world, block)
}
