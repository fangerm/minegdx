/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/29/20, 3:20 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import xyz.angm.terra3d.client.networking.Client
import xyz.angm.terra3d.client.resources.soundPlayer
import xyz.angm.terra3d.client.world.World
import xyz.angm.terra3d.common.networking.BlockUpdate

fun registerBlockChangeListener(client: Client, world: World) {
    client.addListenerPriority {
        // Fluids do not produce noise here
        if (it is BlockUpdate && it.properties?.block?.fluid == false) {
            val previous = world.getBlock(it.position)
            val sound = when {
                it.type == 0 -> previous?.properties?.block?.destroySound
                previous == null -> it.properties?.block?.placedSound
                else -> return@addListenerPriority // Block type did not change, probably just metadata change
            }
            soundPlayer.playSound3D(sound ?: return@addListenerPriority, it.position.toV3())
        }
    }
}

/** A function that will make the given table make a clicking noise when
 * clicked by the user. Used for buttons in the menu and similar. */
fun Table.click() {
    addCaptureListener {
        if ((it as? InputEvent)?.type == InputEvent.Type.touchDown)
            soundPlayer.playSound("random/menu_click")
        false
    }
}