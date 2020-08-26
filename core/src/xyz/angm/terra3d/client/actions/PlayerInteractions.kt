package xyz.angm.terra3d.client.actions

import com.badlogic.gdx.utils.ObjectMap
import ktx.ashley.get
import ktx.collections.*
import xyz.angm.terra3d.client.graphics.panels.game.inventory.ChestPanel
import xyz.angm.terra3d.client.graphics.panels.game.inventory.CraftingGridPanel
import xyz.angm.terra3d.client.graphics.panels.game.inventory.FurnacePanel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.common.ecs.components.specific.MAX_HUNGER
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.world.Block

/** Allows registering listeners for interactions between the player and items/blocks.
 * See [Event] for all events that can be listened to. */
object PlayerInteractions {

    private val listeners = ObjectMap<Pair<ItemType, Event>, (EventContext) -> Unit>()

    init {
        setListener("crafting_table", Event.BLOCK_CLICKED) { (screen) ->
            screen.pushPanel(CraftingGridPanel(screen))
        }
        setListener("crafting_table", Event.ITEM_CLICKED, getListener("crafting_table", Event.BLOCK_CLICKED)!!)


        setListener("furnace", Event.BLOCK_CLICKED) { ctx ->
            ctx.screen.pushPanel(FurnacePanel(ctx.screen, ctx.block!!))
        }

        setListener("furnace", Event.BLOCK_CLICKED) { ctx ->
            ctx.screen.pushPanel(ChestPanel(ctx.screen, ctx.block!!))
        }

        for (item in Item.Properties.allItems) {
            if (item.hunger != 0) {
                setListener(item.type, Event.ITEM_CLICKED) { ctx ->
                    if (ctx.screen.player[playerM]!!.hunger < MAX_HUNGER) {
                        ctx.screen.player[playerM]!!.hunger += item.hunger
                        ctx.screen.playerInventory.subtractFromHeldItem(1)
                    }
                }
            }
        }
    }

    /** Sets a listener.
     * @param item The type of item to listen upon
     * @param type The type of event to listen upon
     * @param listener The listener to execute */
    private fun setListener(item: String, type: Event, listener: (EventContext) -> Unit) {
        listeners[Pair(Item.Properties.fromIdentifier(item).type, type)] = listener
    }

    /** Sets a listener.
     * @param item The type of item to listen upon
     * @param type The type of event to listen upon
     * @param listener The listener to execute */
    private fun setListener(item: ItemType, type: Event, listener: (EventContext) -> Unit) {
        listeners[Pair(item, type)] = listener
    }

    /** Returns the listener registered. */
    fun getListener(item: Item, event: Event): ((EventContext) -> Unit)? = listeners[Pair(item.properties.type, event)]

    /** Returns the listener registered. */
    fun getListener(block: Block, event: Event): ((EventContext) -> Unit)? = listeners[Pair(block.properties!!.type, event)]

    /** Returns the listener registered. */
    private fun getListener(type: String, event: Event) = listeners[Pair(Item.Properties.fromIdentifier(type).type, event)]
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