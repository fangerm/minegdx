/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 10:06 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.ecs

import xyz.angm.rox.mapperFor
import xyz.angm.terra3d.client.ecs.components.LocalPlayerComponent
import xyz.angm.terra3d.client.ecs.components.render.ModelRenderComponent
import xyz.angm.terra3d.client.ecs.components.render.PlayerRenderComponent
import xyz.angm.terra3d.common.ecs.components.*
import xyz.angm.terra3d.common.ecs.components.specific.DayTimeComponent
import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent
import xyz.angm.terra3d.common.ecs.components.specific.PlayerComponent
import xyz.angm.terra3d.server.ecs.components.BlockComponent

/*
* This file contains mappers for all components.
* Reusing them allows for better performance and prevents code duplication.
*/

val position = mapperFor<PositionComponent>()
val direction = mapperFor<DirectionComponent>()
val velocity = mapperFor<VelocityComponent>()
val health = mapperFor<HealthComponent>()

val item = mapperFor<ItemComponent>()
val block = mapperFor<BlockComponent>()
val dayTime = mapperFor<DayTimeComponent>()

val playerM = mapperFor<PlayerComponent>()
val playerRender = mapperFor<PlayerRenderComponent>()
val localPlayer = mapperFor<LocalPlayerComponent>()

val network = mapperFor<NetworkSyncComponent>()
val ignoreSync = mapperFor<IgnoreSyncFlag>()
val remove = mapperFor<RemoveFlag>()

@Suppress("unused") // Unused but required to ensure NoPhysicsFlag is registered
val noPhysics = mapperFor<NoPhysicsFlag>()

val modelRender = mapperFor<ModelRenderComponent>()