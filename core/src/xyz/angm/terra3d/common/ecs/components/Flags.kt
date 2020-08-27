package xyz.angm.terra3d.common.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.get
import xyz.angm.terra3d.common.ecs.network


/*
 * This file contains all components with no state, used simply as a flag.
 */

/** Flags an entity to NOT use the server-side physics engine, despite having position/vector/direction components.
 * Used for players; they update physics client-side. */
class NoPhysicsFlag : Component

/** Flags an entity to be removed from the engine; happens after the current update cycle. */
class RemoveFlag private constructor() : Component {
    companion object {
        /** Mark an entity to be scheduled for removal.
         * Will also ensure it syncs if needed. */
        fun flag(entity: Entity) {
            entity.add(RemoveFlag())
            entity[network]?.needsSync = true
        }
    }
}

/** Ignores the entity containing it when receiving it over network. */
class IgnoreSyncFlag : Component