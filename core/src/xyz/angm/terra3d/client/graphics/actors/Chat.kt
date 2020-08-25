package xyz.angm.terra3d.client.graphics.actors

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import ktx.actors.alpha
import xyz.angm.terra3d.client.networking.Client
import xyz.angm.terra3d.common.networking.ChatMessagePacket
import kotlin.math.max

/** Displays the chat.
 * @param skin The skin to use
 * @param client The client to receive messages with */
class Chat(skin: Skin, client: Client) : Table(skin) {

    private val chatMessages = Array<String>(true, 20)

    init {
        client.addListener { packet ->
            if (packet is ChatMessagePacket) addMessage(packet.message)
        }
        background = skin.getDrawable("black-transparent")
        left()
        pad(5f)
    }

    /** Update chat messages.
     * @param fade If the actor should fade after 15 seconds */
    fun update(fade: Boolean = true) {
        isVisible = !chatMessages.isEmpty
        clearActions()
        clearChildren()
        alpha = 1f
        chatMessages.forEach {
            add(Label(it, skin, "default-24pt")).left().row()
        }
        height = prefHeight
        width = max(400f, prefWidth)
        if (fade) addAction(Actions.fadeOut(15f, Interpolation.pow5))
    }

    /** Adds a new message to the chat. */
    fun addMessage(message: String) {
        chatMessages.add(message)
        if (chatMessages.size > 20) chatMessages.removeIndex(0)
        update()
    }
}