/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server.networking

/** A wrapper for a connection. Subclass it and add other connection info if needed (eg. a socket)
 * @property ip The IP of the client connected
 * @property id The id of the client. IDs should be unique.*/
open class Connection(val ip: String, val id: Int)