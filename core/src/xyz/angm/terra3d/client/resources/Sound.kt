/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 7:29 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.resources

import com.badlogic.gdx.math.Vector3
import xyz.angm.terra3d.common.ecs.components.VectoredComponent

/** Interface for playing sound. */
interface ISound {

    /** Initialize and get ready for playing sounds. */
    fun init()

    /** Play a sound to the player. */
    fun playSound(sound: String)

    /** Play a 3D sound at the specified location. Coordinate system is the world. */
    fun playSound3D(sound: String, location: Vector3)

    /** Same as [playSound3D] but loops the source until [stopPlaying] is called. */
    fun playLooping(sound: String, location: Vector3): Int

    /** Interrupts the given source. Source ID is obtained from [playLooping]. */
    fun stopPlaying(source: Int)

    /** Updates the position and direction of the listener for 3D sound, which is usually the player. */
    fun updateListenerPosition(position: VectoredComponent, direction: VectoredComponent)
}

/** A dummy sound implementation that deliberately does nothing.
 * Used by default when the launcher didn't specify an interface. */
private object DummySound : ISound {
    override fun init() {}
    override fun playSound(sound: String) {}
    override fun playSound3D(sound: String, location: Vector3) {}
    override fun playLooping(sound: String, location: Vector3) = 0
    override fun stopPlaying(source: Int) {}
    override fun updateListenerPosition(position: VectoredComponent, direction: VectoredComponent) {}
}

/** An interface for playing sound effects.
 * Defaults to dummy that does nothing, can only be set to proper interface once.
 * (The dummy is used instead of a 'lateinit' variable to prevent crashes in cases
 * where no sound is the intended behavior, like the server or during unit tests) */
var soundPlayer: ISound = DummySound