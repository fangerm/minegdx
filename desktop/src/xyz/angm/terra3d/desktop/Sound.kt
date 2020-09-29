/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 10:06 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.desktop


import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.audio.OggInputStream
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ObjectIntMap
import com.badlogic.gdx.utils.StreamUtils
import org.lwjgl.openal.AL10.*
import xyz.angm.terra3d.client.resources.ISound
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.ecs.components.DirectionComponent
import xyz.angm.terra3d.common.ecs.components.PositionComponent
import xyz.angm.terra3d.common.ecs.components.VectoredComponent
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A sound interface playing sounds using LWJGL's OpenAL wrapper. */
object Sound : ISound {

    private val sounds = ObjectIntMap<String>()
    private val listenerPosition = Vector3()

    override fun init() {
        alListener3f(AL_POSITION, 0f, 0f, 0f)
        alListener3f(AL_VELOCITY, 0f, 0f, 0f)
        updateListenerPosition(PositionComponent(), DirectionComponent())
    }

    override fun updateListenerPosition(position: VectoredComponent, direction: VectoredComponent) {
        listenerPosition.set(position)
        alListener3f(AL_POSITION, position.x, position.y, position.z)
        alListener3f(AL_ORIENTATION, direction.x, direction.y, direction.z)
    }

    override fun playSound3D(sound: String, location: Vector3) {
        val source = genSource(sound)
        alSource3f(source, AL_POSITION, location.x, location.y, location.z)
        alSourcePlay(source)
    }

    override fun playSound(sound: String) = playSound3D(sound, listenerPosition)

    override fun playLooping(sound: String, location: Vector3): Int {
        val source = genSource(sound)
        alSourcei(source, AL_LOOPING, 1)
        alSource3f(source, AL_POSITION, location.x, location.y, location.z)
        alSourcePlay(source)
        return source
    }

    override fun stopPlaying(source: Int) {
        // Don't actually stop the sound; just disable looping
        // so it stops on it's own instead of unnatural cutoff
        alSourcei(source, AL_LOOPING, 0)
    }

    private fun genSource(sound: String): Int {
        val source = alGenSources()
        alSourcei(source, AL_BUFFER, getSound(sound))
        return source
    }

    private fun getSound(sound: String): Int {
        val soundIndex = sounds[sound, -1]
        return if (soundIndex == -1) loadSound(sound) else soundIndex
    }

    private fun loadSound(sound: String): Int {
        val bufferID = alGenBuffers()
        constructSound(sound, bufferID)
        sounds.put(sound, bufferID)
        return bufferID
    }

    /** Both this and setupSound are abridged from [com.badlogic.gdx.backends.lwjgl.audio.Ogg]. */
    private fun constructSound(sound: String, bufferID: Int) {
        var input: OggInputStream? = null
        try {
            input = OggInputStream(Gdx.files.internal(ResourceManager.getFullPath("sounds/$sound.ogg")).read())
            val output = ByteArrayOutputStream(4096)
            val buffer = ByteArray(2048)
            while (!input.atEnd()) {
                val length = input.read(buffer)
                if (length == -1) break
                output.write(buffer, 0, length)
            }
            setupSound(bufferID, output.toByteArray(), input.channels, input.sampleRate)
        } finally {
            StreamUtils.closeQuietly(input)
        }
    }

    /** @see constructSound */
    private fun setupSound(bufferID: Int, pcm: ByteArray, channels: Int, sampleRate: Int) {
        val bytes = pcm.size - pcm.size % if (channels > 1) 4 else 2
        val buffer = ByteBuffer.allocateDirect(bytes)
        buffer.order(ByteOrder.nativeOrder())
        buffer.put(pcm, 0, bytes)
        buffer.flip()

        alBufferData(bufferID, if (channels > 1) AL_FORMAT_STEREO16 else AL_FORMAT_MONO16, buffer.asShortBuffer(), sampleRate)
    }
}