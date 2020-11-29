/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/29/20, 10:11 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.IntSet
import com.charleskorn.kaml.Yaml
import org.nustaq.serialization.FSTConfiguration
import xyz.angm.rox.Component
import xyz.angm.rox.Entity
import xyz.angm.rox.FSTEntitySerializer
import xyz.angm.terra3d.common.ecs.components.*
import xyz.angm.terra3d.common.ecs.components.specific.DayTimeComponent
import xyz.angm.terra3d.common.ecs.components.specific.FallingBlockComponent
import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent
import xyz.angm.terra3d.common.ecs.components.specific.PlayerComponent
import xyz.angm.terra3d.common.ecs.ignoreSync
import xyz.angm.terra3d.common.ecs.localPlayer
import xyz.angm.terra3d.common.ecs.modelRender
import xyz.angm.terra3d.common.ecs.playerRender
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.networking.*
import xyz.angm.terra3d.common.quest.Quest
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.Chunk
import kotlin.reflect.KClass

/** A simple YAML serializer used for configuration files and some game data. */
val yaml = Yaml()

/** A FST serializer used for network communication and world storage. */
val fst = createFST(
    // Packets
    ChunkRequest::class, JoinPacket::class, InitPacket::class,
    ChunksLine::class, ChatMessagePacket::class,

    // Components
    Component::class, VectoredComponent::class,
    PositionComponent::class, VelocityComponent::class, DirectionComponent::class,
    HealthComponent::class, PlayerComponent::class, RemoveFlag::class,
    ItemComponent::class, FallingBlockComponent::class,
    NetworkSyncComponent::class, DayTimeComponent::class,
    NoPhysicsFlag::class,

    // Various
    IntVector3::class, Vector3::class, Quest::class, Entity::class,
    Chunk::class, Array<Chunk>::class, Block::class, Item::class,
    Inventory::class, PlayerComponent.PlayerInventory::class
)

private fun createFST(vararg classes: KClass<out Any>): FSTConfiguration {
    val fst = FSTConfiguration.createDefaultConfiguration()
    classes.forEach { fst.registerClass(it.java) }

    val ignore = IntSet()
    ignore.add(localPlayer.index)
    ignore.add(modelRender.index)
    ignore.add(playerRender.index)
    ignore.add(ignoreSync.index)
    fst.registerSerializer(Entity::class.java, FSTEntitySerializer(ignore), true)

    fst.registerSerializer(Chunk::class.java, Chunk.FSTChunkSerializer(), true)
    fst.registerSerializer(IntVector3::class.java, IntVector3.FSTVectorSerializer(), true)
    return fst
}
