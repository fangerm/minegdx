/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 10/1/20, 9:50 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.rox

import xyz.angm.rox.ComponentMapper.Companion.getMapper
import xyz.angm.rox.systems.EntitySystem
import xyz.angm.rox.util.RoxArray
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/** The Engine, which is core of every rox ECS instance. Register
 * all your entities, systems and listeners in an Engine instance.
 *
 * Warning: The arrays used for entities and systems do not allow
 * nested iteration. If you need it, you need to create
 * your own [RoxArray.RoxIterator] unless you want to
 * run into some very nasty bugs.
 *
 * @property entities All entities currently in the engine.
 * @property systems All systems currently registered.
 * @property updating If the ECS is currently inside [Engine.update]. */
class Engine {

    val entities = RoxArray<Entity>(false, 200)
    val systems = RoxArray<EntitySystem>(true, 20)
    private val families = RoxArray<Family>(20)
    private val listeners = RoxArray<EntityListener>(5)
    private val builder = EntityBuilder()

    var updating = false
        private set
    private val pendingAdd = RoxArray<Entity>(false, 5)
    private val pendingRemove = RoxArray<Entity>(false, 5)

    /** Calls [EntitySystem.update] on all systems to advance
     * the ECS by one step as well as other upkeep.
     * The ECS is considered inside an 'update cycle' during this call.
     * Should call this once per game loop.
     * @param delta Time in seconds since last call. */
    fun update(delta: Float) {
        updating = true

        systems.forEach { it.update(delta) }
        pendingAdd.forEach { addInternal(it) }
        pendingAdd.clear()
        pendingRemove.forEach { removeInternal(it) }
        pendingRemove.clear()

        updating = false
    }

    /** Add the given entity to the ECS.
     * Will delay until the end of an update cycle if currently in one. */
    fun add(entity: Entity) {
        if (updating) pendingAdd.add(entity)
        else addInternal(entity)
    }

    /** Add an entity system. Should not be called during an
     * update cycle; register all systems on init if possible.
     * Note that this method does *not* guard against adding the same
     * system multiple times; adding a system twice will make it
     * update twice each cycle. */
    fun add(system: EntitySystem) {
        system.engine = this
        systems.add(system)
        systems.sort()
    }

    /** Add an entity listener to listen to entity changes.
     * Same as with systems: Ideally do not add during an update cycle.
     * Also does not guard against adding twice, see note about adding systems. */
    fun add(listener: EntityListener) {
        if (listener.family.index < 0) registerFamily(listener.family)
        listeners.add(listener)
    }

    /** Remove the entity from the system.
     * Delays until end of update cycle if inside of one.
     * Note that any reference to this entity will become invalid after this call. */
    fun remove(entity: Entity) {
        if (updating) pendingRemove.add(entity)
        else removeInternal(entity)
    }

    /** Returns all entities registered that are part of the given family. */
    operator fun get(family: Family): RoxArray<Entity> {
        if (family.index < 0) registerFamily(family)
        return family.entities
    }

    /** Returns the given system if present. */
    operator fun <T : EntitySystem> get(system: KClass<out T>) = systems.find { it::class == system } as T?

    /** Creates a new entity and registers it as part of the ECS.
     * This is a custom DSL to make entity creation easier.
     * @see [EntityBuilder] */
    fun entity(init: EntityBuilder.() -> Unit): Entity {
        init(builder)
        val entity = builder.get()
        add(entity)
        return entity
    }

    /** Update family bits on the given entity, should be part of this ECS. */
    internal fun updateFamilies(entity: Entity) {
        families.forEach { it.entityChanged(entity) }
    }

    /** Register a new family if it isn't known */
    private fun registerFamily(family: Family) {
        // See if a family with the same parameters is already registered
        val existing = families.find {
            it.include == family.include && it.exclude == family.exclude
        }

        if (existing == null) {
            // If not, add the new family to the list
            family.index = families.size
            families.add(family)
            entities.forEach(::updateFamilies)
            family.regenEntities(entities)
        } else {
            // If there is, simply make the 'new' family
            // a clone of the preexisting one
            family.index = existing.index
            family.entities = existing.entities
        }
    }

    private fun addInternal(entity: Entity) {
        entities.add(entity)
        updateFamilies(entity)
        listeners.forEach { if (entity partOf it.family) it.entityAdded(entity) }
    }

    private fun removeInternal(entity: Entity) {
        entities.remove(entity)
        families.forEach { it.entityRemoved(entity) }
        listeners.forEach { if (entity partOf it.family) it.entityRemoved(entity) }
        Entity.free(entity)
    }

    /** This class is used for the entity builder DSL. */
    class EntityBuilder {

        var entity = Entity.get()

        /** Add a component of the given class to the entity.
         * Class must have an empty constructor for this method to work.
         * Can add a code block to initialize the component. */
        inline fun <reified T : Component> with(init: T.() -> Unit = {}) {
            val component = T::class.createInstance()
            add(component, init)
        }

        /** Add the given specific component to the entity.
         * Can add a code block to initialize the component. */
        inline fun <reified T : Component> add(component: T, init: T.() -> Unit = {}) {
            init(component)
            val index = getMapper(T::class)
            entity.components[index] = component
            entity.componentBits.set(index)
        }

        internal fun get(): Entity {
            val e = entity
            entity = Entity.get()
            return e
        }
    }
}