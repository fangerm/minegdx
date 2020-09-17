package xyz.angm.rox

import com.badlogic.gdx.utils.ObjectIntMap
import kotlin.reflect.KClass

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

inline fun <reified A : Component> mapperFor() = ComponentMapper(A::class, ComponentMapper.index++)