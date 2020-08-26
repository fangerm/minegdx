package xyz.angm.terra3d.client.graphics.panels.menu

import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.*
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.resources.configuration

/** Multiplayer server selection. */
class MultiplayerMenuPanel(screen: MenuScreen) : Panel(screen) {

    init {
        reload(screen)
    }

    internal fun reload(screen: MenuScreen) {
        clearChildren()
        this + table {
            focusedActor = scrollPane {
                table {
                    configuration.servers.forEach { server ->
                        button {
                            background = skin.getDrawable("black-transparent")

                            label(server.key) { it.pad(5f).colspan(2).expandX().left().row() }

                            label("IP: ${server.value}", style = "italic-16pt") { it.pad(5f, 5f, 10f, 5f).left() }

                            textButton("Delete", style = "server-delete") {
                                it.right().row()
                                onClick {
                                    configuration.removeServer(server.key)
                                    screen.popPanel()
                                    screen.pushPanel(MultiplayerMenuPanel(screen))
                                }
                            }

                            onClick { screen.connectToServer(server.value) }

                            it.width(700f).pad(20f, 0f, 20f, 0f).row()
                        }
                    }
                }
                it.colspan(2).pad(50f, 0f, 50f, 0f).expand().row()
            }

            textButton("Add Server") {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f)
                onClick { screen.pushPanel(MultiplayerServerAddPanel(screen, this@MultiplayerMenuPanel)) }
            }

            textButton("Direct Connect") {
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
}