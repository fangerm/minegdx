package xyz.angm.terra3d.common.ecs.components.specific

import com.badlogic.ashley.core.Component

/** This component is only used by the DayTime entity, see [xyz.angm.terra3d.common.ecs.systems.DayTimeSystem]. */
class DayTimeComponent : Component {
    /** The current day time, in range of 0-(PI*2). */
    var time = 0.5f
}