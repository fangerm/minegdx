package xyz.angm.rox

import com.badlogic.gdx.utils.Bits
import com.badlogic.gdx.utils.IntSet
import org.nustaq.serialization.FSTBasicObjectSerializer
import org.nustaq.serialization.FSTClazzInfo
import org.nustaq.serialization.FSTObjectInput
import org.nustaq.serialization.FSTObjectOutput
import xyz.angm.rox.ComponentMapper.Companion.getMapper
import java.io.Serializable

/** An entity, the center of the rox ECS.
 * An entity contains a set of components that contain it's state,
 * which is then acted on by systems using families.
 *
 * For creating a new entity, use [Engine.entity]. Additionally,
 * you can serialize entities - see [FSTEntitySerializer].
 *
 * @property components The components in this entity. Do not modify directly! */
class Entity internal constructor(val components: Bag, internal val componentBits: Bits) : Serializable {

    // TODO This seems overkill...
    internal val familyBits = Bits()

    internal constructor(components: Bag) : this(components, Bits()) {
        for (i in 0 until components.size) {
            componentBits.set(getMapper((components[i] ?: continue)::class))
        }
    }

    /** This constructor is only to satisfy the need of [Serializable] and should
     * not be used for creating actual entities. */
    constructor() : this(Bag(0), Bits())

    /** Returns the component of the given type in this entity.
     * @throws NullPointerException If this entity does not contain that
     * component, see [Entity.c] for a null-safe version */
    operator fun <C : Component> get(mapper: ComponentMapper<C>) = components[mapper.index]!! as C

    /** Null-safe version of [Entity.get], returns null if this
     * type of component is not part of the entity, returns component if it does. */
    fun <C : Component> c(mapper: ComponentMapper<C>): C? = components[mapper.index] as C?

    /** @return If this entity contains given type of component. */
    infix fun <C : Component> has(mapper: ComponentMapper<C>) = c(mapper) != null

    /** Add the given component to this entity.
     * @param engine The engine responsible for this entity */
    fun add(engine: Engine, component: Component) {
        addInternal(component)
        engine.updateFamilies(this)
    }

    /** Add all components from `other`, replacing any components
     * of the same type that already exist.
     * @param engine The engine responsible for this entity */
    fun addAll(engine: Engine, other: Entity) {
        for (i in 0 until other.components.size) {
            if (other.components[i] != null) {
                addInternal(other.components[i]!!)
            }
        }
        engine.updateFamilies(this)
    }

    private fun addInternal(component: Component): Boolean {
        val index = getMapper(component::class)
        components[index] = component
        return componentBits.getAndSet(index)
    }

    /** Remove the given component fron the entity if present.
     * @param engine The engine responsible for this entity */
    fun remove(engine: Engine, mapper: ComponentMapper<out Component>) {
        components[mapper.index] = null
        componentBits.clear(mapper.index)
        engine.updateFamilies(this)
    }

    /** @return If this entity is part of the given family. */
    infix fun partOf(family: Family) = familyBits[family.index]
}

/** A simple entity serializer for the FST framework.
 * @property ignore Component types that should not be serialized.
 * Use [ComponentMapper.index] for the values. */
class FSTEntitySerializer(private val ignore: IntSet) : FSTBasicObjectSerializer() {

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
        output.writeInt(-2342)
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
        while (lastI != -2342) {
            val component = input.readObject() as Component
            components[lastI] = component
            lastI = input.readInt()
        }
        return Entity(components)
    }
}
