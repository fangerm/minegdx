package xyz.angm.terra3d.common

import com.badlogic.gdx.math.Vector3
import com.charleskorn.kaml.Yaml
import org.nustaq.serialization.FSTConfiguration
import xyz.angm.rox.Component
import xyz.angm.rox.Entity
import xyz.angm.rox.FSTEntitySerializer
import xyz.angm.terra3d.client.ecs.components.LocalPlayerComponent
import xyz.angm.terra3d.common.ecs.components.*
import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent
import xyz.angm.terra3d.common.ecs.components.specific.PlayerComponent
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.networking.*
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
    Component::class,
    LocalPlayerComponent::class, PlayerComponent::class,
    HealthComponent::class, ItemComponent::class,
    NetworkSyncComponent::class,
    PositionComponent::class, VelocityComponent::class, DirectionComponent::class,
    NoPhysicsFlag::class, RemoveFlag::class,

    // Various
    IntVector3::class, Vector3::class,
    Chunk::class, Block::class, Item::class,
    Inventory::class, PlayerComponent.PlayerInventory::class
)

private fun createFST(vararg classes: KClass<out Any>): FSTConfiguration {
    val fst = FSTConfiguration.createDefaultConfiguration()
    classes.forEach { fst.registerClass(it.java) }

    fst.registerSerializer(Entity::class.java, FSTEntitySerializer(), true)
    fst.registerSerializer(Chunk::class.java, Chunk.FSTChunkSerializer(), true)
    fst.registerSerializer(IntVector3::class.java, IntVector3.FSTVectorSerializer(), true)
    return fst
}
