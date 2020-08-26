package xyz.angm.terra3d.server.world.generation

import com.badlogic.gdx.utils.OrderedMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import ktx.assets.file
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.yaml
import xyz.angm.terra3d.server.world.World

/** A structure is a fixed set of blocks generated in the world. */
class Structure private constructor(private val calls: Array<DrawCall>) {

    /** Manages and hold all structures */
    companion object {
        private val structures = loadStructures()

        /** Generate a structure.
         * @param structure Type of structure
         * @param position The position of the structure's center. */
        fun generate(structure: String, position: IntVector3) = structures[structure]?.queue(position.cpy())

        /** Update all structure's pending locations. */
        fun update(world: World) {
            structures.values().forEach { it.update(world) }
            world.flushBlockQueue() // Individual structures queue their blocks
        }

        private fun loadStructures(): OrderedMap<String, Structure> {
            val raw = yaml.decodeFromString(
                MapSerializer(String.serializer(), ListSerializer(MapSerializer(String.serializer(), SerializedDrawCall.serializer()))),
                file("structures.yaml").readString()
            )

            val structs = OrderedMap<String, Structure>(raw.size)
            for (struct in raw) {
                val name = struct.key
                val calls = com.badlogic.gdx.utils.Array<DrawCall>(true, struct.value.size, DrawCall::class.java)
                for (call in struct.value) {
                    val v = call.values.first()
                    calls.add(
                        when (call.keys.first()) {
                            "box" -> BoxCall(IntVector3(v.start), Item.Properties.fromIdentifier(v.block).type, IntVector3(v.end!!))
                            else -> PointCall(IntVector3(v.start), Item.Properties.fromIdentifier(v.block).type)
                        }
                    )
                }
                structs.put(name, Structure(calls.items))
            }

            return structs
        }

        @Serializable
        private class SerializedDrawCall(val start: IntArray, val block: String) {
            val end: IntArray? = null
        }

        private val tmpIV = IntVector3() // Vector passed into draw calls
    }

    private val locationsPending = com.badlogic.gdx.utils.Array<IntVector3>()

    /** Update the structure's pending locations
     * @param world The world to apply to */
    fun update(world: World) {
        locationsPending.removeAll {
            apply(world, it)
        }
    }

    /** Queue a position for the structure to apply at */
    fun queue(position: IntVector3) = locationsPending.add(position)

    private fun apply(world: World, pos: IntVector3): Boolean {
        for (call in calls) {
            if (!call.draw(world, tmpIV.set(pos))) return false
        }
        return true
    }
}

private val tmpIV = IntVector3() // Vector used by draw calls

private sealed class DrawCall(val start: IntVector3, val block: ItemType) {
    abstract fun draw(world: World, pos: IntVector3): Boolean
}

private class BoxCall(start: IntVector3, block: ItemType, val end: IntVector3) : DrawCall(start, block) {
    override fun draw(world: World, pos: IntVector3): Boolean {
        for (y in start.y..end.y)
            for (x in start.x..end.x)
                for (z in start.z..end.z)
                    if (!world.queueBlock(tmpIV.set(pos).add(x, y, z), block)) return false
        return true
    }
}

private class PointCall(start: IntVector3, block: ItemType) : DrawCall(start, block) {
    override fun draw(world: World, pos: IntVector3) = world.queueBlock(pos.add(start), block)
}
