package xyz.angm.terra3d.common

import ch.qos.logback.classic.Level
import com.badlogic.gdx.math.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import mu.KLogger
import mu.KotlinLogging

/** Global logger. Log level can be set to DEBUG using --debug as first VM argument, otherwise it's WARN. */
val log = KotlinLogging.logger { }

/** Extension property for easily getting and setting log level. */
var KLogger.level: Level
    get() = (underlyingLogger as ch.qos.logback.classic.Logger).level
    set(value) {
        (underlyingLogger as ch.qos.logback.classic.Logger).level = value
    }

/** Turns a string into a long, by multiplying the byte of every char with it. Used for generating a Random seed. */
fun String.convertToLong(): Long {
    var long = 1L
    this.chars().forEach { long *= it }
    return long
}

/** Schedules a closure to run at given intervals, using the provided scope.
 * Used as a replacement for java timers. */
fun schedule(initial: Long, delay: Long, scope: CoroutineScope, run: () -> Unit) {
    val ticker = ticker(delay, initial, scope.coroutineContext)
    scope.launch {
        while (true) {
            ticker.receive()
            run()
        }
    }
}

/** Axis indexing for vectors, used by some block renderers for dynamic oriented rendering. */
operator fun Vector3.get(index: Int) =
    when (index) {
        0 -> x
        1 -> y
        else -> z
    }

/** Axis indexing for vectors, used by some block renderers for dynamic oriented rendering. */
operator fun Vector3.set(index: Int, v: Float) =
    when (index) {
        0 -> x = v
        1 -> y = v
        else -> z = v
    }