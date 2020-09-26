package xyz.angm.terra3d.common.items.metadata

import com.badlogic.gdx.utils.IntMap
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/** This object holds metadata for all item types.
 * When an item is first created, it's metadata is
 * retrieved from here and copied. */
object DefaultMeta {

    private val metadata = IntMap<KClass<out IMetadata>>()

    init {
        fun add(name: String, t: KClass<out IMetadata>) = metadata.put(Item.Properties.fromIdentifier(name).type, t)

        // Add custom metadata here
        add("translocator", TranslocatorMetadata::class)
        add("configurator", ConfiguratorMetadata::class)
    }

    infix fun of(type: ItemType): IMetadata? = metadata[type]?.createInstance()
}