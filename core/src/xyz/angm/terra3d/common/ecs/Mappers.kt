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
val noPhysics = mapperFor<NoPhysicsFlag>()

val modelRender = mapperFor<ModelRenderComponent>()