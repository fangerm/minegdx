package xyz.angm.terra3d.server.world.generation

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import ktx.assets.file
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.server.world.World

/** A structure is a fixed set of blocks generated in the world. */
@Serializable
class Structure(private val blocks: Array<Array<Array<String?>>>, private val center: IntVector3) {

    /** Manages and hold all structures */
    companion object {
        private val structures =
            Json.decodeFromString(MapSerializer(String.serializer(), serializer()), file("structures.json").readString())

        /** Generate a structure.
         * @param structure Type of structure
         * @param position The position of the structure's center. */
        fun generate(structure: String, position: IntVector3) = structures[structure]?.queue(position.cpy())

        /** Update all structure's pending locations. */
        fun update(world: World) {
            structures.values.forEach { it.update(world) }
            world.flushBlockQueue() // Individual structures queue their blocks
        }
    }

    @Transient
    private val locationsPending = com.badlogic.gdx.utils.Array<IntVector3>()
    @Transient
    private val tmpIV = IntVector3()
    @Transient
    private val tmpIV2 = IntVector3()

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
        val position = tmpIV.set(pos).minus(center)
        for (y in 0 until blocks.size)
            for (x in 0 until blocks[y].size)
                for (z in 0 until blocks[y][x].size) {
                    if (!world.queueBlock(
                            tmpIV2.set(position).add(x, y, z),
                            Item.Properties.fromIdentifier(blocks[y][x][z] ?: continue).type
                        )
                    ) return false
                }
        return true
    }
}
