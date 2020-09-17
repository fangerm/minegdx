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