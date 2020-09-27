/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.world.generation

import com.badlogic.gdx.utils.OrderedMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import ktx.assets.file
import ktx.collections.*
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.world.WorldInterface
import xyz.angm.terra3d.common.yaml

internal class Structures {

    private val structures = OrderedMap<Structure, GdxArray<IntVector3>>()

    /** Generate a structure.
     * @param structureName Name of structure
     * @param position The position of the structure's center. */
    fun generate(structureName: String, position: IntVector3) {
        val structure = Structure.structures[structureName]
        var arr = structures.get(structure)
        if (arr == null) {
            arr = GdxArray(16)
            structures[structure] = arr
        }

        arr.add(position.cpy())
    }

    /** Update all structure's pending locations. */
    fun update(world: WorldInterface) = structures.forEach { it.key.update(world, it.value) }
}

/** A structure is a fixed set of blocks generated in the world. */
private class Structure private constructor(private val calls: Array<DrawCall>) {

    /** Update the structure's pending locations
     * @param world The world to apply to
     * @param pending All locations pending. */
    fun update(world: WorldInterface, pending: GdxArray<IntVector3>) {
        pending.removeAll {
            apply(world, it)
        }
    }

    private fun apply(world: WorldInterface, pos: IntVector3): Boolean {
        var success = true
        for (call in calls) {
            success = success && call.draw(world, tmpIV.set(pos))
        }
        return success
    }

    override fun equals(other: Any?) = this === other
    override fun hashCode() = calls.contentHashCode()

    /** Manages and holds all structures */
    companion object {
        internal val structures = loadStructures()

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
}

private val tmpIV = IntVector3() // Vector used by draw calls

private sealed class DrawCall(val start: IntVector3, val block: ItemType) {
    abstract fun draw(world: WorldInterface, pos: IntVector3): Boolean
}

private class BoxCall(start: IntVector3, block: ItemType, val end: IntVector3) : DrawCall(start, block) {
    override fun draw(world: WorldInterface, pos: IntVector3): Boolean {
        var success = true
        for (y in start.y..end.y)
            for (x in start.x..end.x)
                for (z in start.z..end.z)
                    success = success && world.setBlockRaw(tmpIV.set(pos).add(x, y, z), block)
        return success
    }
}

private class PointCall(start: IntVector3, block: ItemType) : DrawCall(start, block) {
    override fun draw(world: WorldInterface, pos: IntVector3) = world.setBlockRaw(pos.add(start), block)
}
