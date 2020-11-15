/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 9:49 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.actions

import com.badlogic.gdx.utils.ObjectMap
import ktx.collections.*
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.panels.game.inventory.ChestPanel
import xyz.angm.terra3d.client.graphics.panels.game.inventory.FurnacePanel
import xyz.angm.terra3d.client.graphics.panels.game.inventory.GeneratorPanel
import xyz.angm.terra3d.client.graphics.panels.game.inventory.MinerPanel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.soundPlayer
import xyz.angm.terra3d.common.ecs.components.specific.MAX_HUNGER
import xyz.angm.terra3d.common.ecs.direction
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.items.metadata.blocks.ConfiguratorMetadata
import xyz.angm.terra3d.common.items.metadata.blocks.TranslocatorMetadata
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.NOTHING

/** Allows registering listeners for interactions between the player and items/blocks.
 * See [Event] for all events that can be listened to. */
object PlayerInteractions {

    private val listeners = ObjectMap<Pair<ItemType, Event>, (EventContext) -> Unit>()

    init {
        fun add(item: ItemType, type: Event, listener: (EventContext) -> Unit) {
            listeners[Pair(item, type)] = listener
        }

        fun add(item: String, type: Event, listener: (EventContext) -> Unit) =
            add(Item.Properties.fromIdentifier(item).type, type, listener)

        fun panel(item: String, panel: (EventContext) -> Panel) =
            add(item, Event.BLOCK_CLICKED) { ctx ->
                ctx.screen.pushPanel(panel(ctx))
            }

        panel("furnace") { ctx -> FurnacePanel(ctx.screen, ctx.block!!) }
        panel("chest") { ctx -> ChestPanel(ctx.screen, ctx.block!!) }
        panel("generator") { ctx -> GeneratorPanel(ctx.screen, ctx.block!!) }
        panel("stone_miner") { ctx -> MinerPanel(ctx.screen, ctx.block!!) }
        panel("iron_miner") { ctx -> MinerPanel(ctx.screen, ctx.block!!) }
        panel("gold_miner") { ctx -> MinerPanel(ctx.screen, ctx.block!!) }
        panel("diamond_miner") { ctx -> MinerPanel(ctx.screen, ctx.block!!) }

        for (item in Item.Properties.allItems) {
            if (item.hunger != 0) {
                add(item.type, Event.ITEM_CLICKED) { ctx ->
                    if (ctx.screen.player[playerM].hunger < MAX_HUNGER) {
                        ctx.screen.player[playerM].hunger += item.hunger
                        ctx.screen.playerInventory.subtractFromHeldItem(1)
                        soundPlayer.playSound("random/eat1")
                    }
                }
            }
        }

        add("configurator", Event.ITEM_CLICKED) { ctx ->
            val confM = ctx.item!!.metadata!! as ConfiguratorMetadata
            val newTransM = ctx.block?.metadata as? TranslocatorMetadata
            if (newTransM == null) {
                ctx.screen.msg("[ORANGE]${I18N["configurator.not-translocator"]}")
                return@add
            }

            if (confM.linking) {
                confM.linking = false
                if (ctx.block.position == confM.position) {
                    ctx.screen.msg("[ORANGE]${I18N["configurator.no-same"]}")
                    return@add
                }
                val oldTranslocator = ctx.screen.world.getBlock(confM.position)
                val oldTransM = oldTranslocator?.metadata as? TranslocatorMetadata
                if (oldTransM == null) {
                    ctx.screen.msg("[ORANGE]${I18N["configurator.block-changed"]}")
                    return@add
                } else if (oldTranslocator.type != ctx.block.type) {
                    ctx.screen.msg("[ORANGE]${I18N["configurator.different-type"]}")
                    return@add
                }

                oldTransM.other = ctx.block.position.cpy()
                oldTransM.push = true
                newTransM.other = confM.position.cpy()
                newTransM.push = false

                // Set them to ensure the new metadata is applied
                ctx.screen.world.setBlock(oldTranslocator)
                ctx.screen.world.setBlock(ctx.block)

                ctx.screen.msg("[GREEN]${I18N["configurator.linked"]}")
            } else {
                confM.linking = true
                confM.position.set(ctx.block.position)
                ctx.screen.msg("[GREEN]$confM")
            }
        }

        // This dirty hack allows swapping slab types; relies on slabs
        // being adjacent in the item id table
        add("slab_oak_lower", Event.ITEM_CLICKED) { ctx ->
            ctx.item!!.type++
            ctx.screen.gameplayPanel.updateHotbarSelector(ctx.screen.playerInventory.hotbarPosition)
        }
        add("slab_oak_upper", Event.ITEM_CLICKED) { ctx ->
            ctx.item!!.type--
            ctx.screen.gameplayPanel.updateHotbarSelector(ctx.screen.playerInventory.hotbarPosition)
        }

        add("bucket", Event.ITEM_CLICKED) { ctx ->
            val fluid = ctx.screen.world.getBlockRaycast(ctx.screen.player[position], ctx.screen.player[direction], prev = false, fluids = true)
            val fBlock = ctx.screen.world.getBlock(fluid ?: return@add)
            val properties = fBlock?.properties?.block ?: return@add
            if (!properties.fluid || properties.fluidReach != fBlock.fluidLevel) return@add
            ctx.screen.world.setBlock(Block(NOTHING, fBlock.position))
            ctx.screen.playerInventory.heldItem!!.type = Item.Properties.fromIdentifier("water_bucket").type
        }

        add("water_bucket", Event.ITEM_CLICKED) { ctx ->
            val fluid = ctx.screen.world.getBlockRaycast(ctx.screen.player[position], ctx.screen.player[direction], prev = true) ?: return@add
            ctx.screen.world.setBlock(Block(Item.Properties.fromIdentifier("water").type, fluid))
            ctx.screen.playerInventory.heldItem!!.type = Item.Properties.fromIdentifier("bucket").type
        }
    }

    /** Returns the listener registered. */
    fun get(item: Item, event: Event): ((EventContext) -> Unit)? = listeners[Pair(item.properties.type, event)]

    /** Returns the listener registered. */
    fun get(block: Block, event: Event): ((EventContext) -> Unit)? = listeners[Pair(block.properties!!.type, event)]
}

/** All events that can be listened to. */
enum class Event {
    /** After a placed block was right-clicked. */
    BLOCK_CLICKED,

    /** After an item in the player's hand was clicked. */
    ITEM_CLICKED
}


/** Context for an event.
 * @property screen The game screen active.
 * @property item The item the player was holding during the event.
 * @property block The block the player interacted with; if applicable. */
data class EventContext(
    val screen: GameScreen,
    val item: Item? = null,
    val block: Block? = null
)