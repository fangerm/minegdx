/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/19/20, 2:20 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common

/** This file contains constants used by both server and client
 * that would not reasonably fit in any other file in the common package. */


/** World constants */

/** Size of 1 chunk in all 3 directions.
 * Must be a power of 2; [CHUNK_MASK] uses need adjustment if not. */
const val CHUNK_SIZE = 32
const val CHUNK_SHIFT = 5
const val CHUNK_MASK = (CHUNK_SIZE - 1) and Int.MAX_VALUE

/** World height in chunks, multiply with CHUNK_SIZE for world height in blocks */
const val WORLD_HEIGHT_IN_CHUNKS = 8

/** World buffer distance */
const val WORLD_BUFFER_DIST = 3

/** Ticks per second. */
const val TICK_RATE = 20L


/** Networking constants */

/** The port server and client use */
const val PORT = 25590

/** Maximum size of a netty packet, in bytes. */
const val MAX_NETTY_FRAME_SIZE = 1_024_000 // 1 MB

/** Size of the receive buffer. */
const val NETTY_BUFFER_SIZE = 8192