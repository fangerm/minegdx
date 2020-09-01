package xyz.angm.terra3d.common.ecs.components.specific

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import ktx.ashley.entity
import xyz.angm.terra3d.common.ecs.components.NetworkSyncComponent
import xyz.angm.terra3d.common.ecs.components.VelocityComponent
import xyz.angm.terra3d.common.ecs.components.WorldComponent
import xyz.angm.terra3d.common.items.Item

/** Component used for items dropped in the world, able to be picked up by players.
 * @property item The item 'carried' by the entity
 * @property pickupTimeout Time until an item can be picked up by a player. 0 means it can be picked up. Used to prevent the player picking up
 *                              an item they just dropped. */
class ItemComponent : Component {

    lateinit var item: Item
    var pickupTimeout = 0f

    companion object {
        private val tmpM = Matrix4()

        fun create(engine: Engine, item: Item, position: Matrix4, pickupTime: Float = 0f) =
            engine.entity {
                with<WorldComponent> { set(position) }
                with<VelocityComponent> { } // TODO: Give items an initial velocity
                with<ItemComponent> {
                    this.item = item
                    pickupTimeout = pickupTime
                }
                with<NetworkSyncComponent>()
            }

        fun create(engine: Engine, item: Item, position: Vector3, pickupTime: Float = 0f) =
            create(engine, item, tmpM.setToTranslation(position), pickupTime)
    }
}