/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.ecs.components

import xyz.angm.rox.Component

/** A component for all entities with a health.
 * @property maxHealth The maximum health the entity can have.
 * @property health The current health of the entity. */
class HealthComponent(
    private val maxHealth: Int = 100,
    var health: Int = maxHealth
) : Component {

    /** Restores health back to max. */
    fun restore() {
        health = maxHealth
    }
}