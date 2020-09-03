package xyz.angm.terra3d.client.graphics.panels.menu

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
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.configuration

/** Multiplayer server selection. */
class MultiplayerMenuPanel(screen: MenuScreen) : Panel(screen) {

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

                            visLabel(server.key) { it.pad(5f).colspan(2).expandX().left().row() }
                            visLabel("IP: ${server.value}", style = "italic-16pt") { it.pad(5f, 5f, 10f, 5f).left() }

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
}