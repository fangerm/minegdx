/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 9:58 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.ecs.components.specific

import com.badlogic.gdx.math.Vector3
import xyz.angm.rox.Component
import xyz.angm.rox.Engine
import xyz.angm.terra3d.common.ecs.components.*
import xyz.angm.terra3d.common.items.Inventory

/** The maximum amount of hunger a player can have. */
const val MAX_HUNGER = 20

/** Component for all persistent player state.
 * @property name The (display)name of the player.
 * @property clientUUID The UUID of the client the player is from.
 * @property hunger The hunger of the player.
 * @property spawnPosition The spawn position returned to after death.
 * @property inventory The inventory of the player containing all their items.
 * @property isDead If the player is currently dead. */
class PlayerComponent : Component {

    lateinit var name: String
    var clientUUID: Int = 0
    var hunger = MAX_HUNGER
    val spawnPosition = defaultSpawnLocation.cpy()!!
    val inventory = PlayerInventory()
    var isDead = false

    /** An [Inventory] with some additional features.
     * @property hotbarPosition The position of the hotbar selector.
     * @property heldItem Convenience method for getting the item at the hotbar position. */
    class PlayerInventory : Inventory(36) {

        var hotbarPosition = 0
        val heldItem get() = this[hotbarPosition]

        /** Subtracts from the held item.
         * @see [Inventory.subtractFromSlot]*/
        fun subtractFromHeldItem(amount: Int) = subtractFromSlot(hotbarPosition, amount)

        /** Scroll the hotbar position. */
        fun scrollHotbarPosition(amount: Int) {
            hotbarPosition += amount
            if (hotbarPosition > 8) hotbarPosition -= 9
            if (hotbarPosition < 0) hotbarPosition += 9
        }

        /** Spawns item entities for all items at the given position
         * and clears the inventory. Used on player death. */
        fun dropAll(engine: Engine, position: Vector3) {
            for (item in items) {
                ItemComponent.create(engine, item ?: continue, position)
            }
            clear()
        }
    }

    companion object {
        /** The default player spawn location. */
        internal val defaultSpawnLocation = Vector3(10000f, 80f, 10000f)

        /** Create a new player entity. */
        fun create(engine: Engine, _name: String, uuid: Int) =
            engine.entity {
                with<PlayerComponent> {
                    name = _name
                    clientUUID = uuid
                }
                with<PositionComponent> { set(defaultSpawnLocation) }
                with<DirectionComponent>()
                with<VelocityComponent>()
                with<HealthComponent>()
                with<NoPhysicsFlag>()
                with<NetworkSyncComponent>()
            }
    }
}
