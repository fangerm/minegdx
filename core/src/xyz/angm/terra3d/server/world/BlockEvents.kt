package xyz.angm.terra3d.server.world

import com.badlogic.gdx.utils.ObjectMap
import ktx.collections.*
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.items.metadata.ChestMetadata
import xyz.angm.terra3d.common.items.metadata.FurnaceMetadata
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.server.ecs.components.BlockComponent

/** A manager for block Events.
 *
 * A block listener is executed on the server whenever a block is placed or destroyed, and allows
 * executing custom code when those events happen.
 *
 * A block entity is code executed by a block at an interval. See [xyz.angm.terra3d.server.ecs.components.BlockComponent] */
object BlockEvents {

    private val listeners = ObjectMap<Pair<ItemType, Event>, (World, Block) -> Unit>()
    private val blockEntities = ObjectMap<ItemType, BlockComponent>()

    // Adds all listeners and block entities on init.
    init {
        // ##### Furnace #####
        setListener("furnace", Event.BLOCK_PLACED) { _, blockPlaced ->
            blockPlaced.metadata = FurnaceMetadata()
        }
        addBlockEntity("furnace", BlockComponent(tickInterval = 20) { world, block ->
            val meta = block.metadata as FurnaceMetadata
            if (meta.burnTime > 10) meta.burnTime -= -10
            else {
                if (meta.fuel == null || meta.fuel?.properties?.burnTime == 0) return@BlockComponent // Not a valid fuel
                meta.burnTime += meta.fuel!!.properties.burnTime
                meta.fuel!!.amount--
            }

            //val recipeResult = FurnaceRecipe.matchAll((meta.baking ?: return@BlockComponent)) ?: return@BlockComponent

            meta.progress += 10
            if (meta.progress >= 100) {
                // meta.result = recipeResult
                meta.progress = 0
            }

            world.metadataChanged(block)
        })

        // ##### Chest #####
        setListener("chest", Event.BLOCK_PLACED) { world, blockPlaced ->
            blockPlaced.metadata = ChestMetadata()
            world.metadataChanged(blockPlaced)
        }

        // #### Miners ####
        val component = BlockComponent(tickInterval = 100) { world, block ->
            val ore = world.getBlock(block.position.minus(0, 1, 0)) // Ore below
            if (!ORES.contains(ore?.properties?.ident ?: "")) return@BlockComponent
            val chest = world.getBlock(block.position.add(0, 2, 0)) // Chest above
            val meta = chest?.metadata as? ChestMetadata ?: return@BlockComponent
            meta.inventory += Item(ore ?: return@BlockComponent)
            world.metadataChanged(chest)
        }
        addBlockEntity("stone_miner", component)
        addBlockEntity("iron_miner", component.copy(tickInterval = 50))
        addBlockEntity("gold_miner", component.copy(tickInterval = 20))
        addBlockEntity("diamond_miner", component.copy(tickInterval = 5))
    }

    /** Sets the listener executed when a certain event happens.
     * @param item The type of block to act upon */
    private fun setListener(item: String, type: Event, listener: (World, Block) -> Unit) {
        listeners[Pair(Item.Properties.fromIdentifier(item).type, type)] = listener
    }

    /** Register a block entity for a block type. Every block of the specified type will then have a copy of the entity added to them.
     * @param type The type of block to register an entity for */
    private fun addBlockEntity(type: String, entity: BlockComponent) {
        blockEntities[Item.Properties.fromIdentifier(type).type] = entity
    }

    /** Returns the appropriate listener for the event. */
    fun getListener(block: Block, event: Event): ((World, Block) -> Unit)? = listeners[Pair(block.properties!!.type, event)]

    /** Returns the block entity for the given block. Position is set correctly automatically. */
    fun getBlockEntity(block: Block) = blockEntities[block.type]?.copy(blockPosition = block.position)

    private val ORES = arrayOf("coal_ore", "iron_ore", "gold_ore", "diamond_ore")
}

/** All events that can be listened to. */
enum class Event {
    /** After a block was placed. */
    BLOCK_PLACED,

    /** Before a block was destroyed. */
    BLOCK_DESTROYED
}
