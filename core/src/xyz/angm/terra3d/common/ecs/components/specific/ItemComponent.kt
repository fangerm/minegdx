package xyz.angm.terra3d.common.ecs.components.specific

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.math.Vector3
import ktx.ashley.entity
import xyz.angm.terra3d.common.ecs.components.NetworkSyncComponent
import xyz.angm.terra3d.common.ecs.components.PositionComponent
import xyz.angm.terra3d.common.ecs.components.VelocityComponent
import xyz.angm.terra3d.common.items.Item
import kotlin.random.Random

/** Component used for items dropped in the world, able to be picked up by players.
 * @property item The item 'carried' by the entity
 * @property pickupTimeout Time until an item can be picked up by a player. 0 means it can be picked up. Used to prevent the player picking up
 *                              an item they just dropped. */
class ItemComponent : Component {

    lateinit var item: Item
    var pickupTimeout = 0f

    companion object {

        private const val XZ_SPEED = 5.0

        private val random = Random(System.currentTimeMillis())

        fun create(engine: Engine, item: Item, position: Vector3, pickupTime: Float = 0f) =
            engine.entity {
                with<PositionComponent> { set(position) }
                with<VelocityComponent> {
                    x = random.nextDouble(-XZ_SPEED, XZ_SPEED).toFloat()
                    y = random.nextDouble(1.0, 4.0).toFloat()
                    z = random.nextDouble(-XZ_SPEED, XZ_SPEED).toFloat()
                    accelerationRate = 0.8f
                }
                with<ItemComponent> {
                    this.item = item
                    pickupTimeout = pickupTime
                }
                with<NetworkSyncComponent>()
            }
    }
}