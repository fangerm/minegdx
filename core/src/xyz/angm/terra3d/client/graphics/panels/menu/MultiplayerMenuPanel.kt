package xyz.angm.terra3d.client.graphics.panels.menu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ktx.actors.onClick
import ktx.actors.onClickEvent
import ktx.actors.plusAssign
import ktx.scene2d.button
import ktx.scene2d.scene2d
import ktx.scene2d.scrollPane
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.networking.Client
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.configuration
import xyz.angm.terra3d.common.networking.ServerInfo
import java.io.IOException

/** Multiplayer server selection. */
class MultiplayerMenuPanel(screen: MenuScreen) : Panel(screen) {

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        reload(screen)
    }

    internal fun reload(screen: MenuScreen) {
        clearChildren()
        this += scene2d.visTable {
            focusedActor = scrollPane {
                visTable {
                    configuration.servers.forEach { server ->
                        button {
                            background = skin.getDrawable("black-transparent")

                            visLabel(server.key) { it.pad(5f).expandX().left() }
                            val players = visLabel("?? / ?? Online") { it.pad(5f).right().row() }
                            val motd = visLabel("Loading...", style = "italic-16pt") { it.pad(5f, 5f, 10f, 5f).left() }

                            val deleteBtn = visTextButton(I18N["multi.delete"], style = "server-delete") {
                                it.right().row()
                                onClick {
                                    screen.pushPanel(ConfirmationPanel(screen) {
                                        if (it) {
                                            configuration.removeServer(server.key)
                                            reload(screen)
                                            this@MultiplayerMenuPanel.isVisible = true // Regrab focus lost by reload
                                        }
                                        screen.popPanel()
                                    })
                                }
                            }

                            onClickEvent { event ->
                                if (event.target.parent == deleteBtn) return@onClickEvent
                                screen.connectToServer(server.value)
                            }

                            scope.launch {
                                try {
                                    var client: Client? = null
                                    client = Client(server.value) {
                                        if (it is ServerInfo) {
                                            players.setText("${it.onlinePlayers} / ${it.maxPlayers} Online")
                                            motd.setText("MOTD: ${it.motd}")
                                        }
                                        client!!.close()
                                    }
                                } catch (e: IOException) {
                                    motd.setText(I18N["multi.offline"])
                                }
                            }

                            it.width(700f).pad(20f, 0f, 20f, 0f).row()
                        }
                    }
                }
                it.colspan(3).pad(50f, 0f, 50f, 0f).expand().row()
            }

            backButton(this, screen)

            visTextButton(I18N["multi.add"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f)
                onClick { screen.pushPanel(MultiplayerServerAddPanel(screen, this@MultiplayerMenuPanel)) }
            }

            visTextButton(I18N["multi.direct"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f)
                onClick {
                    screen.pushPanel(GetUserInputPanel(screen, "Enter Server IP:", "Connect") {
                        if (it != null) screen.connectToServer(it)
                        else screen.popPanel()
                    })
                }
            }

            setFillParent(true)
        }
    }

    override fun dispose() = scope.cancel()
}