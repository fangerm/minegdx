/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 7:07 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.ecs.components.specific

import xyz.angm.rox.Component
import xyz.angm.rox.Engine
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.components.NetworkSyncComponent
import xyz.angm.terra3d.common.ecs.components.PositionComponent
import xyz.angm.terra3d.common.ecs.components.VelocityComponent
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType

/** Component used for blocks that can and are falling.
 * Created when block below one of them is removed, destroyed
 * and replaced by a simple block once hitting the ground.
 * Note that this does not support blocks with metadata.
 * @property block The block 'carried' by the entity */
class FallingBlockComponent : Component {

    var block = 0

    companion object {
        fun create(engine: Engine, block: ItemType, position: IntVector3) {
            engine.entity {
                with<PositionComponent> { position.toV3(this) }
                with<VelocityComponent> {
                    gravity = 1f
                    y = -5f
                    accelerationRate = 1f
                }
                with<FallingBlockComponent> {
                    this.block = block
                }
                with<NetworkSyncComponent>()
            }
        }

        fun maybeFall(engine: Engine, block: ItemType, position: IntVector3, onFall: () -> Unit) {
            if (Item.Properties.fromType(block)!!.block!!.gravity) {
                create(engine, block, position)
                onFall()
            }
        }
    }
}