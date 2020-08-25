package xyz.angm.terra3d.server.ecs.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.utils.ObjectMap
import ktx.ashley.allOf
import ktx.ashley.get
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.block
import xyz.angm.terra3d.server.ecs.components.BlockComponent
import xyz.angm.terra3d.server.world.World

/** A system that handles and ticks any block entities.
 * See [xyz.angm.terra3d.server.ecs.components.BlockComponent] for more info about block entities. */
class BlockEntitySystem(private val world: World) : IteratingSystem(allOf(BlockComponent::class).get()) {

    private var tickCount = 0
    private val blockEntities = ObjectMap<IntVector3, Entity>()

    /** Increase tick count every update. */
    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        tickCount++
    }

    /** Tick any entity that need to be. */
    override fun processEntity(entity: Entity, delta: Float) {
        val blockC = entity[block]!!
        if (shouldTick(blockC)) blockC(world, world.getBlock(blockC.blockPosition) ?: return)
    }

    private fun shouldTick(block: BlockComponent) = tickCount % block.tickInterval == 0

    /** Creates a new block entity. All entities should be created with this helper. */
    fun createBlockEntity(engine: Engine, component: BlockComponent) {
        val entity = engine.createEntity()
        entity.add(component)
        engine.addEntity(entity)
        blockEntities.put(component.blockPosition, entity)
    }

    /** Removes a block entity at the given position. All block entities should be removed with this helper. */
    fun removeBlockEntity(engine: Engine, position: IntVector3) {
        val entity = blockEntities[position] ?: return
        blockEntities.remove(position)
        engine.removeEntity(entity)
    }
}