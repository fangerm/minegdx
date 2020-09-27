/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/19/20, 2:20 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import ktx.collections.*
import org.nustaq.serialization.FSTBasicObjectSerializer
import org.nustaq.serialization.FSTClazzInfo
import org.nustaq.serialization.FSTObjectInput
import org.nustaq.serialization.FSTObjectOutput
import java.io.Serializable

/** A 3D vector using integers as values.
 * @constructor Applies supplied values. Default values are (0, 0, 0)
 * @param x The X coordinate
 * @param y The Y coordinate
 * @param z The Z coordinate */
@kotlinx.serialization.Serializable
open class IntVector3(var x: Int = 0, var y: Int = 0, var z: Int = 0) : Serializable {

    /** Constructs a vector from a libGDX float vector. Values are floored.
     * @param vector3 The float vector to be floored */
    constructor(vector3: Vector3) : this(MathUtils.floor(vector3.x), MathUtils.floor(vector3.y), MathUtils.floor(vector3.z))

    /** Constructs from an integer array that must have at least 3 values. */
    constructor(arr: IntArray) : this(arr[0], arr[1], arr[2])

    /** Creates a clone of given vector. */
    constructor(intVec: IntVector3) : this(intVec.x, intVec.y, intVec.z)

    /** @return A copy of itself; allows for calculations without affecting original. */
    fun cpy() = IntVector3(x, y, z)

    /** @return A float Vector3 with this vector's values */
    fun toV3() = Vector3(x.toFloat(), y.toFloat(), z.toFloat())

    /** Sets the specified vector3 to this vector.
     * @return The vector3 given, with the new coordinates applied */
    fun toV3(vector3: Vector3): Vector3 {
        vector3.x = x.toFloat()
        vector3.y = y.toFloat()
        vector3.z = z.toFloat()
        return vector3
    }

    /** @param min Minimum value.
     * @param max Maximum value (non-inclusive)
     * @return If all 3 directions are in bounds */
    fun isInBounds(min: Int, max: Int) = x >= min && y >= min && z >= min && x < max && y < max && z < max

    /** Sets itself to specified values.
     * @return Itself */
    fun set(v: IntVector3) = set(v.x, v.y, v.z)

    fun set(v: Vector3) = set(MathUtils.floor(v.x), MathUtils.floor(v.y), MathUtils.floor(v.z))

    fun set(x: Int, y: Int, z: Int): IntVector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    /** Adds specified values to itself.
     * @return Itself */
    fun add(v: IntVector3) = add(v.x, v.y, v.z)

    /** @see add */
    fun add(x: Int, y: Int, z: Int): IntVector3 {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    /** Subtracts specified values from itself.
     * @return Itself */
    fun minus(x: Int, y: Int, z: Int): IntVector3 {
        this.x -= x
        this.y -= y
        this.z -= z
        return this
    }

    /** @see minus */
    fun minus(other: IntVector3) = minus(other.x, other.y, other.z)

    /** Divides all axes by specified values.
     * @return Itself */
    fun div(v: IntVector3) = div(v.x, v.y, v.z)

    /** @see div */
    fun div(num: Int) = div(num, num, num)

    /** @see div */
    fun div(x: Int, y: Int, z: Int): IntVector3 {
        this.x /= x
        this.y /= y
        this.z /= z
        return this
    }

    /** Normalize this vector to multiples of chunk positions. */
    fun chunk(): IntVector3 {
        val mask = CHUNK_MASK xor Int.MAX_VALUE
        x = x and mask
        y = y and mask
        z = z and mask
        return this
    }

    /** @return If the distance between this and other on the XZ axes is within max. */
    fun within(other: IntVector3, max: Int): Boolean {
        val a = other.x - this.x
        val c = other.z - this.z
        return a > -max && a < max && c > -max && c < max
    }

    /** Makes this vector into a single scalar int for use with indexing or compression
     * @param mul The multiplier, determines max value of each axis */
    fun linearize(mul: Int) = x + (y * mul) + (z * mul * mul)

    /** Does the opposite to linearize, setting itself to `value`. */
    fun delinearize(value: Int, mul: Int) {
        x = value % mul
        y = (value % (mul * mul)) / mul
        z = value / (mul * mul)
    }

    /** String representation of all 3 axes */
    override fun toString() = "($x | $y | $z)"

    override fun equals(other: Any?) = other is IntVector3 && other.x == x && other.y == y && other.z == z

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    companion object {
        val ZERO = IntVector3()
    }

    /** Simple vector serializer to improve performance a bit compared to reflection. */
    class FSTVectorSerializer : FSTBasicObjectSerializer() {

        override fun writeObject(out: FSTObjectOutput, vec: Any, cInfo: FSTClazzInfo, fInfo: FSTClazzInfo.FSTFieldInfo, strPos: Int) {
            vec as IntVector3
            out.writeInt(vec.x)
            out.writeInt(vec.y)
            out.writeInt(vec.z)
        }

        override fun instantiate(oClass: Class<*>, input: FSTObjectInput, cInfo: FSTClazzInfo, fInfo: FSTClazzInfo.FSTFieldInfo, strPos: Int): Any {
            return IntVector3(input.readInt(), input.readInt(), input.readInt())
        }
    }
}