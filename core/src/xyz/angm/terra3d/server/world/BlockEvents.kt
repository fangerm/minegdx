/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/16/20, 7:01 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server.world

import com.badlogic.gdx.utils.ObjectMap
import ktx.collections.*
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.items.metadata.EnergyStorageMeta
import xyz.angm.terra3d.common.items.metadata.IMetadata
import xyz.angm.terra3d.common.items.metadata.InventoryMetadata
import xyz.angm.terra3d.common.items.metadata.blocks.*
import xyz.angm.terra3d.common.recipes.FurnaceRecipes
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
        /** Sets the listener executed when a certain event happens.
         * @param item The type of block to act upon */
        fun listener(item: String, type: Event, listener: (World, Block) -> Unit) {
            listeners[Pair(Item.Properties.fromIdentifier(item).type, type)] = listener
        }

        /** Register a block entity for a block type. Every block of the specified type will then have a copy of the entity added to them.
         * @param type The type of block to register an entity for */
        fun blockEntity(type: String, entity: BlockComponent) {
            blockEntities[Item.Properties.fromIdentifier(type).type] = entity
        }

        /** Simple listener that attaches metadata if none is attached; runs on block placement*/
        fun attachMeta(type: String, inst: () -> IMetadata) {
            listener(type, Event.BLOCK_PLACED) { world, blockPlaced ->
                blockPlaced.metadata = blockPlaced.metadata ?: inst()
                world.metadataChanged(blockPlaced)
            }
        }

        // ##### Furnace #####
        attachMeta("furnace") { FurnaceMetadata() }

        listener("furnace", Event.BLOCK_DESTROYED) { _, removed ->
            val meta = removed.metadata as FurnaceMetadata
            meta.progress = 0
            meta.burnTime = 0
        }

        blockEntity("furnace", BlockComponent(tickInterval = 20) { world, block ->
            val meta = block.metadata as FurnaceMetadata
            if (meta.burnTime > 10) meta.burnTime -= 10
            else {
                if (meta.fuel[0] == null || meta.fuel[0]?.properties?.burnTime == 0) return@BlockComponent // Not a valid fuel
                meta.burnTime += meta.fuel[0]!!.properties.burnTime
                meta.fuel.subtractFromSlot(0, 1)
            }

            val recipeResult = FurnaceRecipes[meta.baking[0]?.type ?: return@BlockComponent] ?: return@BlockComponent
            if (meta.baking[0]!!.amount < recipeResult.inAmount) return@BlockComponent

            meta.progress += 10
            if (meta.progress >= 100) {
                when {
                    meta.result[0] == null -> meta.result[0] = Item(recipeResult.output, recipeResult.outAmount)
                    meta.result[0]!!.type == recipeResult.output -> meta.result[0]!!.amount += recipeResult.outAmount
                    else -> return@BlockComponent
                }
                meta.baking[0]!!.amount -= recipeResult.inAmount
                meta.progress = 0
            }

            world.metadataChanged(block)
        })

        // ##### Generator ####
        attachMeta("generator") { GeneratorMetadata() }

        blockEntity("generator", BlockComponent(tickInterval = 20) { world, block ->
            val meta = block.metadata as GeneratorMetadata

            val pos = block.orientation.applyIV(block.position.cpy())
            val target = world.getBlock(pos)
            val targetM = target?.metadata as? EnergyStorageMeta
            if (targetM?.isFull() == false) {
                targetM.receive(meta.energyStored)
                meta.energyStored = 0
                world.metadataChanged(target)
                world.metadataChanged(block)
            }

            if (meta.fuel[0] == null || meta.fuel[0]?.properties?.burnTime == 0) return@BlockComponent // Not a valid fuel
            meta.energyStored += meta.fuel[0]!!.properties.burnTime / 10
            meta.fuel.subtractFromSlot(0, 1)
            world.metadataChanged(block)
        })

        // ##### Chest #####
        attachMeta("chest") { ChestMetadata() }

        // #### Miners ####
        for (miner in listOf("stone_miner", "iron_miner", "gold_miner", "diamond_miner")) {
            attachMeta(miner) { MinerMetadata() }
        }
        val component = BlockComponent(tickInterval = 100) { world, block ->
            val meta = block.metadata as MinerMetadata
            if (meta.energy < 50) return@BlockComponent
            meta.energy -= 50

            val ore = world.getBlock(block.position.cpy().minus(0, 1, 0)) // Ore below
            if (!ORES.contains(ore?.properties?.ident ?: "")) return@BlockComponent
            meta.inventory += Item(ore ?: return@BlockComponent)
            world.metadataChanged(block)
        }
        blockEntity("stone_miner", component)
        blockEntity("iron_miner", component.copy(tickInterval = 50))
        blockEntity("gold_miner", component.copy(tickInterval = 20))
        blockEntity("diamond_miner", component.copy(tickInterval = 5))

        // #### Translocator ####
        listener("translocator", Event.BLOCK_DESTROYED) { world, removed ->
            val meta = removed.metadata as TranslocatorMetadata

            if (meta.other != null) { // Remove other position on both if it was linked
                val other = world.getBlock(meta.other!!)!!
                (other.metadata as TranslocatorMetadata).other = null
                world.metadataChanged(other)

                meta.other = null
            }
        }

        blockEntity("translocator", BlockComponent(tickInterval = 20) { world, block ->
            val meta = block.metadata as? TranslocatorMetadata ?: return@BlockComponent
            if (!meta.push || meta.other == null) return@BlockComponent

            val pullInventoryPosition = block.orientation.applyIVInv(block.position)
            val pullBlock = world.getBlock(pullInventoryPosition)
            val pullInv = pullBlock?.metadata as? InventoryMetadata ?: return@BlockComponent

            val other = world.getBlock(meta.other!!) ?: return@BlockComponent
            val pushInventoryPosition = other.orientation.applyIVInv(other.position)
            val pushBlock = world.getBlock(pushInventoryPosition)
            val pushInv = pushBlock?.metadata as? InventoryMetadata ?: return@BlockComponent

            val item = pullInv.pull.takeFirst() ?: return@BlockComponent
            val remaining = pushInv.push.add(item)
            if (remaining != 0) {
                item.amount = remaining
                pullInv.pull += item
            }

            world.metadataChanged(pullBlock)
            world.metadataChanged(pushBlock)
        })
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
