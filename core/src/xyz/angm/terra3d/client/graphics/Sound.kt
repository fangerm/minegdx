package xyz.angm.terra3d.client.graphics

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import xyz.angm.terra3d.client.networking.Client
import xyz.angm.terra3d.client.resources.soundPlayer
import xyz.angm.terra3d.client.world.World
import xyz.angm.terra3d.common.networking.BlockUpdate

fun registerBlockChangeListener(client: Client, world: World) {
    client.addListenerPriority {
        if (it is BlockUpdate) {
            val sound = if (it.type == 0) world.getBlock(it.position)?.properties?.block?.destroySound
            else it.properties?.block?.placedSound
            soundPlayer.playSound3D(sound ?: return@addListenerPriority, it.position.toV3())
        }
    }
}

/** A function that will make the given table make a clicking noise when
 * clicked by the user. Used for buttons in the menu and similar. */
fun Table.click() {
    addCaptureListener {
        if ((it as? InputEvent)?.type == InputEvent.Type.touchDown)
            soundPlayer.playSound("random/wood_click")
        false
    }
}