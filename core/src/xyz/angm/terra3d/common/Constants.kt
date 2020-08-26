package xyz.angm.terra3d.common

/** This file contains constants used by both server and client
 * that would not reasonably fit in any other file in the common package. */


/** World constants */

/** Size of 1 chunk in all 3 directions */
const val CHUNK_SIZE = 16

/** World height in chunks, multiply with CHUNK_SIZE for world height in blocks */
const val WORLD_HEIGHT_IN_CHUNKS = CHUNK_SIZE

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