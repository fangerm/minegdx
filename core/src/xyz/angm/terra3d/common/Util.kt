package xyz.angm.terra3d.common

import ch.qos.logback.classic.Level
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Matrix4.*
import com.badlogic.gdx.math.Vector3
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

fun Vector3.toStringFloor() = "(${x.toInt()} | ${y.toInt()} | ${z.toInt()})"

/** Use this with constants in Matrix4 to allow things like world[M13] */
operator fun Matrix4.get(i: Int) = `val`[i]

/** Returns the distance between this and o's translation.
 * Does not use sqrt for better performance, result is an approximation. */
fun Matrix4.dist(o: Matrix4): Float {
    val a = o[M03] - this[M03]
    val b = o[M13] - this[M13]
    val c = o[M23] - this[M23]
    return a * a + b * b + c * c
}