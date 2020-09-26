package xyz.angm.rox

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Bits
import ktx.collections.*
import xyz.angm.rox.ComponentMapper.Companion.getMapper
import kotlin.reflect.KClass

/** A family is a set of entities with all share a set of
 * components. Families are used to filter entities.
 * Create a new family with [Family.allOf] or [Family.exclude].
 * You can also chain `exclude` after `allOf`. */
class Family private constructor() {

    internal var index = -1
    internal val include = Bits()
    internal var exclude: Int = -1
    internal var entities = GdxArray<Entity>(false, 5, Entity::class.java)

    /** Sets the component to exclude; an entity will not be considered
     * part of this family if it has this component.
     * This must be called during initialization and cannot be called
     * after a family was first used. */
    fun exclude(component: KClass<out Component>): Family {
        if (index != -1) throw IllegalArgumentException("Can only be called before using the family")
        exclude = getMapper(component)
        return this
    }

    internal fun regenEntities(newE: Array<Entity>) {
        entities.clear()
        newE.forEach {
            if (it.familyBits[index]) {
                entities.add(it)
            }
        }
    }

    internal fun entityRemoved(entity: Entity) {
        if (entity partOf this) entities.removeValue(entity, true)
    }

    internal fun entityChanged(entity: Entity) {
        val matched = entity.familyBits[index]
        val matches = this matches entity

        if (!matched && matches) {
            entity.familyBits.set(index)
            entities.add(entity)
        } else if (matched && !matches) {
            entity.familyBits.clear(index)
            entities.removeValue(entity, true)
        } // else: no change.
    }

    internal infix fun matches(entity: Entity) =
        entity.componentBits.containsAll(include) && (exclude == -1 || !entity.componentBits.get(exclude))

    companion object {
        /** Creates a new family that requires entities to contain all given components. */
        fun allOf(vararg components: KClass<out Component>): Family {
            val family = Family()
            for (component in components) {
                family.include.set(getMapper(component))
            }
            return family
        }

        /** Creates a new family that requires entities to not contain the given component. */
        fun exclude(component: KClass<out Component>): Family {
            val family = Family()
            family.exclude(component)
            return family
        }
    }
}