package xyz.angm.terra3d.common.ecs.components.specific

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.math.Vector3
import ktx.ashley.entity
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
    }

    companion object {
        /** The default player spawn location. */
        internal val defaultSpawnLocation = Vector3(10000f, 100f, 10000f)

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
                with<SizeComponent> {
                    x = 0.4f
                    y = 1.75f
                    z = 0.4f
                }
                with<HealthComponent>()
                with<NoPhysicsFlag>()
                with<NetworkSyncComponent>()
            }
    }
}
