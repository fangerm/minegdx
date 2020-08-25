package xyz.angm.terra3d.common.ecs.components.specific

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.math.Vector3
import ktx.ashley.entity
import xyz.angm.terra3d.common.ecs.components.NetworkSyncComponent
import xyz.angm.terra3d.common.ecs.components.PositionComponent
import xyz.angm.terra3d.common.ecs.components.SizeComponent
import xyz.angm.terra3d.common.ecs.components.VelocityComponent
import xyz.angm.terra3d.common.items.Item

/** Component used for items dropped in the world, able to be picked up by players.
 * @property item The item 'carried' by the entity
 * @property pickupTimeout Time until an item can be picked up by a player. 0 means it can be picked up. Used to prevent the player picking up
 *                              an item they just dropped. */
class ItemComponent : Component {

    lateinit var item: Item
    var pickupTimeout = 0f

    companion object {
        fun create(engine: Engine, item: Item, position: Vector3, pickupTime: Float = 0f) =
            engine.entity {
                with<PositionComponent> { set(position) }
                with<VelocityComponent> {
                    y = -0.05f
                    gravity = false
                }
                with<SizeComponent> {
                    x = 0.4f
                    y = 0.4f
                    z = 0.4f
                }
                with<ItemComponent> {
                    this.item = item
                    pickupTimeout = pickupTime
                }
                with<NetworkSyncComponent>()
            }
    }
}