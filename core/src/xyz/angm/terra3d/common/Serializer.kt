package xyz.angm.terra3d.common

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.charleskorn.kaml.Yaml
import org.nustaq.serialization.*
import xyz.angm.terra3d.client.ecs.components.LocalPlayerComponent
import xyz.angm.terra3d.client.ecs.components.render.ModelRenderComponent
import xyz.angm.terra3d.client.ecs.components.render.PlayerRenderComponent
import xyz.angm.terra3d.common.ecs.components.*
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
    PlayerBlockInteractionPacket::class, ChunkRequest::class, JoinPacket::class,
    ChunksUpdate::class, ChatMessagePacket::class,

    // Components
    Component::class,
    LocalPlayerComponent::class,
    HealthComponent::class,
    NetworkSyncComponent::class,
    PositionComponent::class, VelocityComponent::class, DirectionComponent::class, SizeComponent::class,
    NoPhysicsFlag::class, RemoveFlag::class,

    // Various
    IntVector3::class, Vector3::class,
    Chunk::class, Block::class, Item::class,
    Inventory::class
)

private fun createFST(vararg classes: KClass<out Any>): FSTConfiguration {
    val fst = FSTConfiguration.createDefaultConfiguration()
    classes.forEach { fst.registerClass(it.java) }

    fst.registerSerializer(Entity::class.java, FSTEntitySerializer(), true)
    return fst
}

/** Custom ECS entity serializer.
 * Only serializes components to vastly improve speed and prevent internal state from being transferred. */
private class FSTEntitySerializer : FSTBasicObjectSerializer() {

    override fun writeObject(
        output: FSTObjectOutput,
        entity: Any,
        clzInfo: FSTClazzInfo,
        referencedBy: FSTClazzInfo.FSTFieldInfo,
        streamPosition: Int
    ) {
        entity as Entity

        // Renderable components are not sent, and the size of the components needs to reflect that
        var size = entity.components.size()
        entity.components.forEach { if (noSerialize.contains(it::class)) size-- }
        output.writeInt(size)

        for (component in entity.components) {
            if (noSerialize.contains(component::class)) continue
            output.writeObject(component)
        }
    }

    override fun instantiate(
        objectClass: Class<*>,
        input: FSTObjectInput,
        serializationInfo: FSTClazzInfo,
        referencee: FSTClazzInfo.FSTFieldInfo,
        streamPosition: Int
    ): Any {
        val entity = Entity()
        val componentAmount = input.readInt()
        for (i in 0 until componentAmount) {
            val component = input.readObject() as Component
            entity.add(component)
        }
        return entity
    }

    companion object {
        /** A set of components that do not get serialized. */
        val noSerialize = HashSet<KClass<out Any>>()

        init {
            noSerialize.add(LocalPlayerComponent::class)
            noSerialize.add(ModelRenderComponent::class)
            noSerialize.add(PlayerRenderComponent::class)
            noSerialize.add(IgnoreSyncFlag::class)
        }
    }
}
