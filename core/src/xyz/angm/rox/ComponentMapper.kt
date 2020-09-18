package xyz.angm.rox

import com.badlogic.gdx.utils.ObjectIntMap
import kotlin.reflect.KClass

/** A component mapper is used for accessing components inside an entity.
 * Component retrieval this way is always O(1), as it is a simple array access.
 * For each component you make, use [mapperFor] for getting a mapper. */
class ComponentMapper<C : Component>(cls: KClass<C>, internal val index: Int) {

    init {
        mappers.put(cls, index)
    }

    companion object {
        var index = 0
        val mappers = ObjectIntMap<KClass<out Component>>(30)
        fun getMapper(component: KClass<out Component>) = mappers[component, -42]
    }
}

/** Create a new mapper for the given component class.
 * The result of this call should be stored in for example a global
 * variable for better performance. */
inline fun <reified A : Component> mapperFor(): ComponentMapper<A> {
    val stored = ComponentMapper.getMapper(A::class)
    return ComponentMapper(A::class, if (stored != -42) stored else ComponentMapper.index++)
}