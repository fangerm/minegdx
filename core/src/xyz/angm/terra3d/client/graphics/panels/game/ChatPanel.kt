package xyz.angm.terra3d.client.graphics.panels.game

import com.badlogic.gdx.Input
import com.kotcrab.vis.ui.widget.VisTextField
import ktx.actors.onKeyDown
import ktx.ashley.get
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.networking.ChatMessagePacket

/** Panel for typing chat messages. */
class ChatPanel(private val screen: GameScreen) : Panel(screen) {

    private val inputField = VisTextField("", "chat-input")

    init {
        background = null

        addActor(inputField)
        inputField.setPosition(10f, 60f)
        inputField.width = 400f

        inputField.onKeyDown { if (it == Input.Keys.ENTER) onEnter(inputField.text) }
        focusedActor = inputField

        screen.gameplayPanel.displayChat()
    }

    private fun onEnter(message: String) {
        screen.client.send(ChatMessagePacket(formatMessage(message)))
        screen.popPanel()
    }

    private fun formatMessage(message: String) = "<[CYAN]${screen.player[playerM]!!.name}[WHITE]> $message"
}