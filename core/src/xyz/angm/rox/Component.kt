/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/19/20, 1:04 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

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