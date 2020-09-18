package xyz.angm.rox

import java.io.Serializable

/** A component, which is part of an entity.
 *
 * To create a new component, simply implement this class.
 * Additionally, you will need to register it with [ComponentMapper].
 *
 * All component types should *not* actually contain any logic
 * or behavior, they are purely data bags for components.
 * Implement your logic in systems instead. */
interface Component : Serializable