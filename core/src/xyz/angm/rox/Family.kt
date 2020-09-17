package xyz.angm.rox

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Bits
import ktx.collections.*
import xyz.angm.rox.ComponentMapper.Companion.getMapper
import kotlin.reflect.KClass

class Family private constructor() {

    internal var index = -1
    private val include = Bits()
    private var exclude: Int = -1
    internal val entities = GdxArray<Entity>(false, 5, Entity::class.java)

    fun exclude(component: KClass<out Component>): Family {
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
        fun allOf(vararg components: KClass<out Component>): Family {
            val family = Family()
            for (component in components) {
                family.include.set(getMapper(component))
            }
            return family
        }

        fun exclude(component: KClass<out Component>): Family {
            val family = Family()
            family.exclude(component)
            return family
        }
    }
}