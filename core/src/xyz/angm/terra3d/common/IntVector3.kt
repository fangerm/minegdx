package xyz.angm.terra3d.common

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import xyz.angm.terra3d.common.ecs.components.VectoredComponent
import java.io.Serializable

/** A 3D vector using integers as values.
 * @constructor Applies supplied values. Default values are (0, 0, 0)
 * @param x The X coordinate
 * @param y The Y coordinate
 * @param z The Z coordinate */
@kotlinx.serialization.Serializable
data class IntVector3(var x: Int = 0, var y: Int = 0, var z: Int = 0) : Serializable {

    /** Constructs a vector from a libGDX float vector. Values are floored.
     * @param vector3 The float vector to be floored */
    constructor(vector3: Vector3) : this(MathUtils.floor(vector3.x), MathUtils.floor(vector3.y), MathUtils.floor(vector3.z))

    /** Constructs a vector from a vectored component. Values are floored.
     * @param vC The VectoredComponent. */
    constructor(vC: VectoredComponent) : this(MathUtils.floor(vC.x), MathUtils.floor(vC.y), MathUtils.floor(vC.z))

    /** Constructs a vector from the translation of a libGDX Matrix4. */
    constructor(matrix4: Matrix4) : this(matrix4.getTranslation(tmpV))

    /** Constructs from an integer array that must have at least 3 values. */
    constructor(arr: IntArray) : this(arr[0], arr[1], arr[2])

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
    fun isInBounds(min: Int, max: Int) = isInBounds(x, y, z, min, max)

    /** Sets itself to specified values.
     * @return Itself */
    fun set(v: IntVector3) = set(v.x, v.y, v.z)

    /** @see set */
    fun set(v: Vector3) = set(MathUtils.floor(v.x), MathUtils.floor(v.y), MathUtils.floor(v.z))

    /** @see set */
    fun set(v: VectoredComponent) = set(MathUtils.floor(v.x), MathUtils.floor(v.y), MathUtils.floor(v.z))

    /** @see set */
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

    /** Multiplies all axes.
     * @param num Value to multiply by.
     * @return Itself */
    fun mul(num: Int): IntVector3 {
        x *= num
        y *= num
        z *= num
        return this
    }

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

    /** Normalize this vector to a multiple of the specified number. */
    fun norm(num: Int) = this.div(num).mul(num)

    /** String representation of all 3 axes */
    override fun toString() = "($x | $y | $z)"

    companion object {
        private val tmpV = Vector3()

        /** @return If all 3 directions are in bounds */
        fun isInBounds(x: Int, y: Int, z: Int, min: Int, max: Int) = x >= min && y >= min && z >= min && x < max && y < max && z < max
    }
}