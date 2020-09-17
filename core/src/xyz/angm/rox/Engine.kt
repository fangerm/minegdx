package xyz.angm.rox

import com.badlogic.gdx.utils.Bits
import ktx.collections.*
import xyz.angm.rox.ComponentMapper.Companion.getMapper
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class Engine {

    val entities = GdxArray<Entity>(false, 200)
    val entitySet = GdxSet<Entity>(200)
    val systems = GdxArray<EntitySystem>(true, 20)
    private val families = GdxArray<Family>(20)
    private val listeners = GdxArray<Pair<Family, EntityListener>>(5)

    fun update(delta: Float) {
        systems.forEach { it.update(delta) }
    }

    fun add(entity: Entity) {
        if (entitySet.contains(entity)) return
        entities.add(entity)
        entitySet.add(entity)
        updateFamilies(entity)
        listeners.forEach { if (entity partOf it.first) it.second.entityAdded(entity) }
    }

    fun add(system: EntitySystem, last: Boolean = true) {
        system.engine = this
        if (last) systems.add(system) else systems.insert(0, system)
    }

    fun add(family: Family, listener: EntityListener) {
        if (family.index < 0) registerFamily(family)
        listeners.add(Pair(family, listener))
    }

    fun remove(entity: Entity) {
        if (!entitySet.remove(entity)) return
        entities.removeValue(entity, true)
        families.forEach { it.entityRemoved(entity) }
        listeners.forEach { if (entity partOf it.first) it.second.entityRemoved(entity) }
    }

    operator fun get(family: Family): GdxArray<Entity> {
        if (family.index < 0) registerFamily(family)
        return family.entities
    }

    operator fun <T : EntitySystem> get(system: KClass<out T>): T = systems.find { it::class == system }!! as T

    fun entity(init: EntityBuilder.() -> Unit): Entity {
        val builder = EntityBuilder()
        init(builder)
        val entity = Entity(builder.components, builder.componentBits)
        add(entity)
        return entity
    }

    internal fun updateFamilies(entity: Entity) {
        for (family in families) {
            family.entityChanged(entity)
        }
    }

    private fun registerFamily(family: Family) {
        family.index = families.size
        families.add(family)
        entities.forEach(::updateFamilies)
        family.regenEntities(entities)
    }

    class EntityBuilder {

        var components = Bag(15)
        var componentBits = Bits()

        inline fun <reified T : Component> with(init: T.() -> Unit = {}) {
            val component = T::class.createInstance()
            add(component, init)
        }

        inline fun <reified T : Component> add(component: T, init: T.() -> Unit = {}) {
            init(component)
            val index = getMapper(T::class)
            components[index] = component
            componentBits.set(index)
        }
    }
}