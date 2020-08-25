package xyz.angm.terra3d.common.ecs.components

import com.badlogic.ashley.core.Component


/*
 * This file contains all components with little to no state, used simply as a flag.
 */

/** Flags an entity to NOT use the server-side physics engine, despite having position/vector/direction components.
 * Used for players; they update physics client-side. */
class NoPhysicsFlag : Component

/** Flags an entity to be removed from the engine; happens after the current update cycle. */
class RemoveFlag : Component

/** Ignores the entity containing it when receiving it over network. */
class IgnoreSyncFlag : Component