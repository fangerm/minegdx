/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.ecs.components.specific

import xyz.angm.rox.Component

/** This component is only used by the DayTime entity, see [xyz.angm.terra3d.common.ecs.systems.DayTimeSystem]. */
class DayTimeComponent : Component {
    /** The current day time, in range of 0-(PI*2). */
    var time = 0.5f
}