package xyz.angm.rox

import com.badlogic.gdx.utils.Bits
import org.nustaq.serialization.FSTBasicObjectSerializer
import org.nustaq.serialization.FSTClazzInfo
import org.nustaq.serialization.FSTObjectInput
import org.nustaq.serialization.FSTObjectOutput
import xyz.angm.rox.ComponentMapper.Companion.getMapper
import xyz.angm.terra3d.common.ecs.ignoreSync
import xyz.angm.terra3d.common.ecs.localPlayer
import xyz.angm.terra3d.common.ecs.modelRender
import xyz.angm.terra3d.common.ecs.playerRender
import java.io.Serializable

class Entity internal constructor(val components: Bag, internal val componentBits: Bits) : Serializable {

    // TODO This seems overkill...
    internal val familyBits = Bits()

    internal constructor(components: Bag) : this(components, Bits()) {
        for (i in 0 until components.size) {
            componentBits.set(getMapper((components[i] ?: continue)::class))
        }
    }

    constructor() : this(Bag(0), Bits())

    operator fun <C : Component> get(mapper: ComponentMapper<C>) = components[mapper.index]!! as C

    fun <C : Component> c(mapper: ComponentMapper<C>): C? = components[mapper.index] as C?

    infix fun <C : Component> has(mapper: ComponentMapper<C>) = c(mapper) != null

    fun add(engine: Engine, component: Component) {
        addInternal(component)
        engine.updateFamilies(this)
    }

    fun addAll(engine: Engine, other: Entity) {
        var recalculateBits = false
        for (i in 0 until other.components.size) {
            if (other.components[i] != null) {
                recalculateBits = recalculateBits || addInternal(other.components[i]!!)
            }
        }
        if (recalculateBits) engine.updateFamilies(this)
    }

    private fun addInternal(component: Component): Boolean {
        val index = getMapper(component::class)
        components[index] = component
        return componentBits.getAndSet(index)
    }

    fun remove(engine: Engine, mapper: ComponentMapper<out Component>) {
        components[mapper.index] = null
        componentBits.clear(mapper.index)
        engine.updateFamilies(this)
    }

    infix fun partOf(family: Family) = familyBits[family.index]

    companion object {
        fun new(init: Entity.() -> Unit): Entity {
            val entity = Entity(Bag(10))
            init(entity)
            return entity
        }
    }
}

class FSTEntitySerializer : FSTBasicObjectSerializer() {

    override fun writeObject(
        output: FSTObjectOutput,
        entity: Any,
        clzInfo: FSTClazzInfo,
        referencedBy: FSTClazzInfo.FSTFieldInfo,
        streamPosition: Int
    ) {
        entity as Entity

        for (i in 0 until entity.components.size) {
            if (ignore.contains(i) || entity.components[i] == null) continue
            else {
                output.writeInt(i)
                output.writeObject(entity.components[i])
            }
        }
        output.writeInt(-1)
    }

    override fun instantiate(
        objectClass: Class<*>,
        input: FSTObjectInput,
        serializationInfo: FSTClazzInfo,
        referencee: FSTClazzInfo.FSTFieldInfo,
        streamPosition: Int
    ): Any {
        val components = Bag(16)
        var lastI = input.readInt()
        while (lastI != -1) {
            val component = input.readObject() as Component
            components[lastI] = component
            lastI = input.readInt()
        }
        return Entity(components)
    }

    companion object {
        /** A set of components that do not get added to EntityData. */
        val ignore = HashSet<Int>()

        init {
            ignore.add(localPlayer.index)
            ignore.add(modelRender.index)
            ignore.add(playerRender.index)
            ignore.add(ignoreSync.index)
        }
    }
}
