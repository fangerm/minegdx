package xyz.angm.rox

import com.badlogic.gdx.utils.Bits
import ktx.collections.*
import xyz.angm.rox.ComponentMapper.Companion.getMapper
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/** The Engine, which is core of every rox ECS instance. Register
 * all your entities, systems and listeners in an Engine instance.
 *
 * Warning: Do not modify any of the arrays exposed as public API or
 * returned by methods. They are intended to be read-only, but cannot
 * be for performance reasons. Expect unintended behavior if you ignore this.
 *
 * @property entities All entities currently in the engine.
 * @property systems All systems currently registered.
 * @property updating If the ECS is currently inside [Engine.update]. */
class Engine {

    val entities = GdxArray<Entity>(false, 200)
    val systems = GdxArray<EntitySystem>(true, 20)
    private val families = GdxArray<Family>(20)
    private val listeners = GdxArray<EntityListener>(5)

    var updating = false
        private set
    private val pendingAdd = GdxArray<Entity>(false, 5)
    private val pendingRemove = GdxArray<Entity>(false, 5)

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
        systems.sort(EntitySystem.SystemComparator)
    }

    /** Add an entity listener to listen to entity changes.
     * Same as with systems: Ideally do not add during an update cycle.
     * Also does not guard against adding twice, see note about adding systems. */
    fun add(listener: EntityListener) {
        if (listener.family.index < 0) registerFamily(listener.family)
        listeners.add(listener)
    }

    /** Remove the entity from the system.
     * Delays until end of update cycle if inside of one. */
    fun remove(entity: Entity) {
        if (updating) pendingRemove.add(entity)
        else removeInternal(entity)
    }

    /** Returns all entities registered that are part of the given family. */
    operator fun get(family: Family): GdxArray<Entity> {
        if (family.index < 0) registerFamily(family)
        return family.entities
    }

    /** Returns the given system if present. */
    operator fun <T : EntitySystem> get(system: KClass<out T>) = systems.find { it::class == system } as T?

    /** Creates a new entity and registers it as part of the ECS.
     * This is a custom DSL to make entity creation easier.
     * @see [EntityBuilder] */
    fun entity(init: EntityBuilder.() -> Unit): Entity {
        val builder = EntityBuilder()
        init(builder)
        val entity = Entity(builder.components, builder.componentBits)
        add(entity)
        return entity
    }

    /** Update family bits on the given entity, should be part of this ECS. */
    internal fun updateFamilies(entity: Entity) {
        families.forEach { it.entityChanged(entity) }
    }

    /** Register a new family if it isn't known */
    private fun registerFamily(family: Family) {
        family.index = families.size
        families.add(family)
        entities.forEach(::updateFamilies)
        family.regenEntities(entities)
    }

    private fun addInternal(entity: Entity) {
        entities.add(entity)
        updateFamilies(entity)
        listeners.forEach { if (entity partOf it.family) it.entityAdded(entity) }
    }

    private fun removeInternal(entity: Entity) {
        entities.removeValue(entity, true)
        families.forEach { it.entityRemoved(entity) }
        listeners.forEach { if (entity partOf it.family) it.entityRemoved(entity) }
    }

    /** This class is used for the entity builder DSL. */
    class EntityBuilder {

        var components = Bag(15)
        var componentBits = Bits()

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
            components[index] = component
            componentBits.set(index)
        }
    }
}