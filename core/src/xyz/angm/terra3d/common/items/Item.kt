package xyz.angm.terra3d.common.items

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.OrderedMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import ktx.assets.file
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.items.Item.Properties
import xyz.angm.terra3d.common.items.metadata.IMetadata
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.yaml
import xyz.angm.terra3d.server.ecs.systems.PhysicsSystem
import java.util.*

/** An alias for item types, which are simple ints.
 * Simply used for clarity. */
typealias ItemType = Int

/** Represents an Item both client- and server-side.
 * @param type Item type. Also see [Properties]
 * @param amount The amount of the item. Defaults to 1.
 * @param metadata The metadata. This contains information specific to this instance of the item.
 * @property texture The texture to draw the item with; needs to be set manually
 * @property properties The properties of this item type */
data class Item(
    var type: ItemType = 1,
    var amount: Int = 1,
    var metadata: IMetadata? = null
) : java.io.Serializable {

    @Transient
    private var textureBackingField: Texture? = null
    val texture: Texture
        get() {
            if (textureBackingField == null) textureBackingField = ResourceManager.get(properties.texture)
            return textureBackingField!!
        }

    val properties get() = Properties.fromType(type)!!

    /** Creates an item from the block. */
    constructor(block: Block) : this(block.type, 1, block.metadata)

    /** If the item stacks with the other item. */
    infix fun stacksWith(other: Item?) = other != null && type == other.type && metadata == other.metadata

    override fun hashCode() = Objects.hash(type, metadata)

    override fun equals(other: Any?) =
        other is Item?
                && other != null
                && type == other.type
                && metadata == other.metadata
                && amount == other.amount

    /** @return Type and amount, formatted */
    override fun toString() = "${amount}x ${properties.name}"

    /** Properties of an item type.
     * @property ident An internal identifier of the block used for getting locale names.
     * @property type The item type ID.
     * @property name The displayed name of the type; defaults to type with capitalized formatting
     * @property stackSize How many of this item can be in 1 stack
     * @property texture The texture of this type of item
     * @property block The block properties of this type, null if type is not a block
     * @property burnTime The amount of ticks an item burns for. Mainly used for determining fuel duration.
     * @property hunger How much hunger this restores, should it be food. */
    @Serializable
    class Properties {

        var ident = "" // Set during init
        var type: ItemType = 0
        val block: BlockProperties? = null
        var name = ""
        var texture = ""
        val tool: ToolProperties? = null
        val stackSize = if (tool != null) 1 else 999
        val burnTime = 0
        val hunger = 0

        val isBlock get() = (block != null)

        private fun init() {
            updateName()
            if (texture == "") texture = "textures/${if (isBlock) "blocks" else "items"}/${ident.toLowerCase()}.png"
        }

        private fun updateName() {
            name = getName(ident)
        }

        companion object {
            private val items = OrderedMap<String, Properties>(200)

            init {
                for (entry in yaml.decodeFromString(MapSerializer(String.serializer(), serializer()), file("items.yaml").readString())) {
                    entry.value.type = items.size + 1
                    entry.value.ident = entry.key
                    entry.value.init()
                    items.put(entry.key, entry.value)
                }
            }

            /** All items in the game */
            val allItems get() = items.values()!!

            /** Get properties by identifier. */
            fun fromIdentifier(type: String) = items[type]!!

            fun tryFromIdentifier(type: String): Item.Properties? = items[type]

            /** Get properties by type. */
            fun fromType(type: ItemType) = if (type == 0) null else items[items.orderedKeys()[type - 1]]

            /** Returns the type. Looks in I18N first, defaults to pretty printed identifier otherwise. */
            private fun getName(string: String): String {
                val i18n = I18N.tryGet("item-$string")
                if (i18n != null) return i18n

                val tmpString = string.toLowerCase().replace("_", " ")
                var newString = tmpString[0].toUpperCase().toString()
                for (i in 1 until tmpString.length) {
                    newString += if (tmpString[i - 1] == ' ') tmpString[i].toUpperCase()
                    else tmpString[i]
                }
                return newString
            }

            /** Reload all names; called on lang change by [I18N]. */
            fun reloadNames() {
                items.values().forEach { it.updateName() }
            }
        }

        /** Block-specific info of a type.
         * @property breakTime The amount of time the block has to be mined for to be destroyed with bare hands, in seconds.
         * @property prefTool The tool affecting mining time.
         * @property minToolLevel The minimum level a tool needs to be to be able to break the block.
         * @property drop An optional identifier of the item type dropped when mining this block (cobble for stone for example).

         * @property texSide Optional: Block side texture. [Properties.texture] will be used instead if null.
         * @property texBottom Optional: Block bottom texture. [Properties.texture] will be used instead if null.
         *
         * @property collider: The collider of this block used by the physics system.
         *
         * @property placedSound Sound played when the block is placed by a player.
         * @property hitSound Sound played when the block is hit by a player.
         * @property destroySound Sound played when a block is broken by a player.
         * @property walkSound Sound played when a player walks on a block. */
        @Serializable
        data class BlockProperties(
            val breakTime: Float = 1.5f,
            val prefTool: String? = null,
            val minToolLevel: Int = 0,
            val drop: String? = null,

            val texSide: String? = null,
            val texBottom: String? = null,

            val collider: PhysicsSystem.BlockCollider = PhysicsSystem.BlockCollider.FULL,
            val orientation: OrientationMode = OrientationMode.DISABLE,

            val placedSound: String = "step/stone1",
            val hitSound: String = "dig/stone1",
            val destroySound: String = "dig/stone1",
            val walkSound: String = "step/stone1"
        ) {
            /** Returns the actual time required to break a block, given the tool's properties. */
            fun getBreakTime(tool: ToolProperties?) = breakTime * (if (tool != null && tool.type == prefTool) tool.multiplier else 1f)

            /** All orientations this block can be placed. Any non-allowed ones get coerced. */
            @Serializable
            enum class OrientationMode {
                DISABLE, // This block is not orientable. Always forces UP.
                XZ, // This block can have any 'XZ' orientation (all but UP and DOWN)
                ALL; // This block can have any orientation

                /** Takes an orientation and adjusts it if needed. */
                fun get(o: Block.Orientation): Block.Orientation {
                    return when {
                        this == DISABLE -> Block.Orientation.UP
                        this == XZ && (o == Block.Orientation.UP || o == Block.Orientation.DOWN) -> Block.Orientation.NORTH
                        else -> o
                    }
                }
            }
        }

        /** Tool-specific info of a type.
         * @property type The type of tool the item is. Default is none
         * @property multiplier The amount a block's break time will be multiplied with when using this tool. Usually < 1.
         * @property durability The durability (amount of uses) of this tool.
         * @property level The level of the tool; used to ensure it can break a block. 0 is equal to the player hand. */
        @Serializable
        data class ToolProperties(
            val type: String,
            val multiplier: Float,
            val durability: Int,
            val level: Int = 0
        )
    }
}