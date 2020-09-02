package xyz.angm.terra3d.client.resources

import com.badlogic.gdx.math.Vector3
import xyz.angm.terra3d.common.ecs.components.VectoredComponent

/** Interface for playing sound. */
interface SoundInterface {

    /** Initialize and get ready for playing sounds. */
    fun init()

    /** Play a sound to the player. */
    fun playSound(sound: String)

    /** Play a 3D sound at the specified location. Coordinate system is the world. */
    fun playSound3D(sound: String, location: Vector3)

    /** Updates the position and direction of the listener for 3D sound, which is usually the player. */
    fun updateListenerPosition(position: VectoredComponent, direction: VectoredComponent)
}

/** A dummy sound interface that deliberately does nothing.
 * Used by default when the launcher didn't specify an interface. */
private object DummySoundInterface : SoundInterface {
    override fun init() {}
    override fun playSound(sound: String) {}
    override fun playSound3D(sound: String, location: Vector3) {}
    override fun updateListenerPosition(position: VectoredComponent, direction: VectoredComponent) {}
}

/** An interface for playing sound effects.
 * Defaults to dummy that does nothing, can only be set to proper interface once.
 * (The dummy is used instead of a 'lateinit' variable to prevent crashes in cases
 * where no sound is the intended behavior, like the server or during unit tests) */
var soundPlayer: SoundInterface = DummySoundInterface