package xyz.angm.terra3d.common.ecs.components

import xyz.angm.rox.Component
import xyz.angm.rox.Engine
import xyz.angm.rox.Entity

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
        fun flag(engine: Engine, entity: Entity) {
            entity.add(engine, RemoveFlag())
            entity[network].needsSync = true
        }
    }
}

/** Ignores the entity containing it when receiving it over network. */
class IgnoreSyncFlag : Component