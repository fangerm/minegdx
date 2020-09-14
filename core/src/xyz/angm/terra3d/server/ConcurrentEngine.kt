package xyz.angm.terra3d.server

import com.badlogic.ashley.core.Engine
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** A simple wrapper around an Ashley engine that only
 * accesses from a single thread to prevent race conditions.
 * Use with Kotlin's invoke like for example this: `engine { update(delta) }` */
class ConcurrentEngine {

    private val engine = Engine()
    private val channel = Channel<Engine.() -> Unit>(5)
    private val worker: Job

    init {
        worker = GlobalScope.launch {
            while (true) channel.receive()(engine)
        }
    }

    operator fun invoke(fn: Engine.() -> Unit) = GlobalScope.launch { channel.send(fn) }

    internal fun close() = worker.cancel()
}