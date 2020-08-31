package xyz.angm.terra3d.common.ecs

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import ktx.collections.GdxArray
import org.nustaq.serialization.FSTBasicObjectSerializer
import org.nustaq.serialization.FSTClazzInfo
import org.nustaq.serialization.FSTObjectInput
import org.nustaq.serialization.FSTObjectOutput
import xyz.angm.terra3d.client.ecs.components.LocalPlayerComponent
import xyz.angm.terra3d.client.ecs.components.render.ModelRenderComponent
import xyz.angm.terra3d.client.ecs.components.render.PlayerRenderComponent
import xyz.angm.terra3d.common.ecs.components.IgnoreSyncFlag
import xyz.angm.terra3d.common.ecs.components.NetworkSyncComponent
import java.io.Serializable
import kotlin.reflect.KClass

/** A data class used for (de)serializing entities.
 * See Serializer for use. */
class EntityData(
    private val networkBacking: NetworkSyncComponent? = null,
    private val componentsBacking: Array<Component>? = null
) : Serializable {

    val network get() = networkBacking!!
    val components get() = componentsBacking!!

    fun toEntity(): Entity {
        val e = Entity()
        components.forEach { e.add(it) }
        return e
    }

    /** Custom ECS entity serializer for EntityData.
     * Only serializes components to vastly improve speed and prevent internal state from being transferred. */
    class FSTEntitySerializer : FSTBasicObjectSerializer() {

        override fun writeObject(
            output: FSTObjectOutput,
            entity: Any,
            clzInfo: FSTClazzInfo,
            referencedBy: FSTClazzInfo.FSTFieldInfo,
            streamPosition: Int
        ) {
            entity as EntityData

            output.writeInt(entity.components.size)
            for (component in entity.components) {
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
            val componentAmount = input.readInt()
            var network: NetworkSyncComponent? = null
            val components = Array(componentAmount) {
                val component = input.readObject() as Component
                if (component is NetworkSyncComponent) network = component
                component
            }
            return EntityData(network, components)
        }
    }

    companion object {
        /** A set of components that do not get added to EntityData. */
        val ignore = HashSet<KClass<out Any>>()

        init {
            ignore.add(LocalPlayerComponent::class)
            ignore.add(ModelRenderComponent::class)
            ignore.add(PlayerRenderComponent::class)
            ignore.add(IgnoreSyncFlag::class)
        }

        fun from(entity: Entity): EntityData {
            var network: NetworkSyncComponent? = null
            val size = entity.components.count { !ignore.contains(it::class) }
            val array = GdxArray<Component>(false, size, Component::class.java)
            for (component in entity.components) {
                if (component is NetworkSyncComponent) network = component
                else if (ignore.contains(component::class)) continue
                array.add(component)
            }
            return EntityData(network!!, array.items)
        }

        fun from(entities: ImmutableArray<Entity>): Array<EntityData> {
            return Array(entities.size()) { from(entities[it]) }
        }

        fun toEntities(entities: Array<EntityData>): Array<Entity> {
            return Array(entities.size) { entities[it].toEntity() }
        }
    }

}