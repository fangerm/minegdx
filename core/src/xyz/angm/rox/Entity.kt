/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/19/20, 12:59 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.rox

import com.badlogic.gdx.utils.Bits
import com.badlogic.gdx.utils.IntSet
import ktx.collections.*
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
 * Note that entities are bound to their ECS/Engine and should not be
 * used outside of it - they are pooled by default, and [Engine.remove]
 * will return an entity to the pool, meaning that it can no longer
 * be used.
 *
 * @property components The components in this entity. Do not modify directly! */
class Entity private constructor() : Serializable {

    // TODO This seems overkill...
    val components = Bag(15)
    val componentBits = Bits()
    internal val familyBits = Bits()

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

    companion object {

        private val free = GdxArray<Entity>(false, 20)

        @Synchronized
        fun get() = if (free.isEmpty) Entity() else free.pop()!!

        @Synchronized
        fun free(entity: Entity) {
            entity.components.clear()
            entity.componentBits.clear()
            entity.familyBits.clear()
            free.add(entity)
        }
    }
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
        val entity = Entity.get()
        val components = entity.components
        var lastI = input.readInt()
        while (lastI != -2342) {
            val component = input.readObject() as Component
            components[lastI] = component
            entity.componentBits.set(lastI)
            lastI = input.readInt()
        }
        return entity
    }
}
