/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 8:36 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import xyz.angm.rox.Engine

/** A simple wrapper around an Ashley engine that only
 * accesses from a single thread to prevent race conditions.
 * Use with Kotlin's invoke like for example this: `engine { update(delta) }` */
class ConcurrentEngine(private val scope: CoroutineScope) {

    private val engine = Engine()
    private val channel = Channel<Engine.() -> Unit>(5)

    init {
        scope.launch {
            while (true) channel.receive()(engine)
        }
    }

    operator fun invoke(fn: Engine.() -> Unit) = scope.launch { channel.send(fn) }
}