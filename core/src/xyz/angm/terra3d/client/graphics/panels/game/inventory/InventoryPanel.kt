package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Align
import ktx.actors.onKeyDown
import xyz.angm.terra3d.client.graphics.actors.ItemActor
import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.actors.ItemTooltip
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.resources.configuration
import xyz.angm.terra3d.common.items.Item

private const val heldItemOffsetX = 2f
private const val heldItemOffsetY = -50f
private const val tooltipOffsetX = 20f
private const val tooltipOffsetY = -5f

/** Class for panels that need to handle items. Items should be added with ItemGroup. */
@Suppress("LeakingThis")
abstract class InventoryPanel(screen: GameScreen) : Panel(screen) {

    var heldItem: Item? = null
    private var heldItemActor = ItemActor(heldItem)
    private val tooltip = ItemTooltip(this)
    private val holdingItem get() = heldItem != null

    init {
        heldItemActor.isVisible = false
        addActor(heldItemActor)
        addActor(tooltip)

        // This needs special handling since the usual input handler responsible
        // for this is unregistered while GUIs are open
        onKeyDown { keycode ->
            if (keycode == configuration.keybinds["openInventory"]) screen.popPanel()
        }
    }

    /** When a slot in an inventory is left clicked */
    open fun itemLeftClicked(actor: ItemGroup.GroupedItemActor) {
        if (actor stacksWith heldItem) {
            if (actor.group.mutable) {
                // If they stack and the actor is mutable, fill the actor with the held item
                fillItem(actor.item, heldItem)
                maybeClearHeld()
            } else {
                // If it's not mutable, fill the held item instead
                fillItem(heldItem, actor.item)
                maybeClearItem(actor)
            }
        } else swapHeldItemWithActorItem(actor) // If the actor or held item are null, just swap

        updateHeldItemActor()
    }

    /** When a slot in an inventory is right clicked */
    open fun itemRightClicked(actor: ItemGroup.GroupedItemActor) {
        if (!holdingItem && !actor.empty) {
            // If there's no held item but an actor item, take half of it to hold
            heldItem = actor.item!!.copy()
            heldItem!!.amount /= 2
            actor.group.inventory.subtractFromSlot(actor.slot, heldItem!!.amount)
        } else if (actor.group.mutable) {
            // If the actor is mutable..
            if (actor.empty) {
                // and does not have an item, put 1 from the held item in it's place
                heldItem?.amount = (heldItem?.amount ?: 1) - 1
                actor.item = heldItem?.copy(amount = 1)
            } else if (actor.item!! stacksWith heldItem && !actor.full) {
                // and stacks with the held item, add 1 from the held item to it
                heldItem!!.amount--
                actor.item!!.amount++
            }
            maybeClearHeld()
        }

        updateHeldItemActor()
    }

    private fun swapHeldItemWithActorItem(actor: ItemGroup.GroupedItemActor) {
        if (!actor.group.mutable && holdingItem) return
        val tmp = heldItem
        heldItem = actor.item
        actor.item = tmp
    }

    /** Fills an item from the other one until stackSize. */
    private fun fillItem(toFill: Item?, fillFrom: Item?) {
        if (toFill == null || fillFrom == null) return
        val stackSize = toFill.properties.stackSize
        toFill.amount += fillFrom.amount
        if (toFill.amount > stackSize) {
            fillFrom.amount = (toFill.amount - stackSize)
            toFill.amount = stackSize
        } else {
            fillFrom.amount = 0
        }
    }

    private fun updateHeldItemActor() {
        heldItemActor.item = heldItem
        heldItemActor.isVisible = heldItem != null
        heldItemActor.zIndex = 999
    }

    private fun maybeClearHeld() {
        if (heldItem?.amount == 0) heldItem = null
    }

    private fun maybeClearItem(actor: ItemGroup.GroupedItemActor) {
        if (actor.item?.amount == 0) actor.item = null
    }

    /** When a slot in an inventory is shift clicked */
    open fun itemShiftClicked(actor: ItemGroup.GroupedItemActor) {}

    /** When a slot is hovered */
    fun itemHovered(actor: ItemGroup.GroupedItemActor) {
        if (!holdingItem) tooltip.update(actor.item)
    }

    /** When a slot is no longer hovered */
    fun itemLeft() = tooltip.update(item = null)

    override fun act(delta: Float) {
        super.act(delta)
        heldItemActor.setPosition(Gdx.input.x + heldItemOffsetX, (WORLD_HEIGHT - Gdx.input.y) + heldItemOffsetY)
        tooltip.setPosition(Gdx.input.x + tooltipOffsetX, (WORLD_HEIGHT - Gdx.input.y) + tooltipOffsetY, Align.topLeft)
    }
}
