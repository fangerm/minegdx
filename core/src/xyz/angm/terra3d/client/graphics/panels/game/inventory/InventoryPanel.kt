package xyz.angm.terra3d.client.graphics.panels.game.inventory

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Align
import ktx.actors.onKeyDown
import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.actors.ItemTooltip
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.resources.configuration
import xyz.angm.terra3d.common.items.Item

/** Class for panels that need to handle items. Items should be added with ItemGroup. */
@Suppress("LeakingThis")
abstract class InventoryPanel(screen: GameScreen) : Panel(screen) {

    private var heldItem: Item? = null
    private var heldItemActor: ItemGroup.ItemActor? = null
    private val heldItemOffsetX = 2f
    private val heldItemOffsetY = -50f

    private val tooltip = ItemTooltip(this)
    private val tooltipOffsetX = 20f
    private val tooltipOffsetY = -5f

    private val holdingItem get() = heldItem != null

    init {
        this.addActor(tooltip)

        // This needs special handling since the usual input handler responsible
        // for this is unregistered while GUIs are open
        onKeyDown { keycode ->
            if (keycode == configuration.keybinds["openInventory"]) screen.popPanel()
        }
    }

    /** When a slot in an inventory is left clicked */
    open fun itemLeftClicked(actor: ItemGroup.ItemActor) {
        if (heldItem?.stacksWith(actor.item) == true) {
            if (actor.group.mutable) {
                fillItem(actor.item!!, heldItem!!)
                if (heldItem?.amount == 0) heldItem = null
            } else {
                fillItem(heldItem!!, actor.item!!)
                if (actor.item?.amount == 0) actor.item = null
            }
        } else swapHeldItemWithActorItem(actor)

        updateHeldItemActor(actor)
    }

    /** When a slot in an inventory is right clicked */
    open fun itemRightClicked(actor: ItemGroup.ItemActor) {
        if (!holdingItem && actor.item != null) {
            heldItem = actor.item!!.copy()
            heldItem!!.amount /= 2
            actor.group.inventory.subtractFromSlot(actor.slot, heldItem!!.amount)

        } else if (actor.group.mutable) {
            if (actor.item == null) {
                heldItem!!.amount--
                actor.item = heldItem!!.copy(amount = 1)
            } else if (actor.item!! stacksWith heldItem && actor.item!!.amount != actor.item!!.properties.stackSize) {
                heldItem!!.amount--
                actor.item!!.amount++
            }
        }

        if (heldItem?.amount == 0) heldItem = null
        updateHeldItemActor(actor)
    }

    private fun swapHeldItemWithActorItem(actor: ItemGroup.ItemActor) {
        if (!actor.group.mutable && holdingItem) return
        val tmp = heldItem
        heldItem = actor.item
        actor.item = tmp
    }

    /** Fills an item from the other one until stackSize. */
    private fun fillItem(toFill: Item, fillFrom: Item) {
        val stackSize = toFill.properties.stackSize
        toFill.amount += fillFrom.amount
        if (toFill.amount > stackSize) {
            fillFrom.amount = (toFill.amount - stackSize)
            toFill.amount = stackSize
        } else {
            fillFrom.amount = 0
        }
    }

    private fun updateHeldItemActor(toCopy: ItemGroup.ItemActor) {
        removeActor(heldItemActor)
        if (heldItem == null) heldItemActor = null
        else {
            heldItemActor = toCopy.lockedClone(heldItem)
            addActor(heldItemActor)
        }
    }

    /** When a slot in an inventory is shift clicked */
    abstract fun itemShiftClicked(actor: ItemGroup.ItemActor)

    /** When a slot is hovered */
    fun itemHovered(actor: ItemGroup.ItemActor) {
        if (!holdingItem) tooltip.update(actor.item)
    }

    /** When a slot is no longer hovered */
    fun itemLeft() = tooltip.update(item = null)

    /** See [com.badlogic.gdx.scenes.scene2d.Actor.act] */
    override fun act(delta: Float) {
        super.act(delta)
        heldItemActor?.setPosition(Gdx.input.x + heldItemOffsetX, (WORLD_HEIGHT - Gdx.input.y) + heldItemOffsetY)
        tooltip.setPosition(Gdx.input.x + tooltipOffsetX, (WORLD_HEIGHT - Gdx.input.y) + tooltipOffsetY, Align.topLeft)
    }
}
