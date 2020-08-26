package xyz.angm.terra3d.client.graphics.panels.menu

import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.*
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.common.world.WorldSaveManager

/** Singleplayer world selection. */
class SingleplayerWorldSelectionPanel(screen: MenuScreen) : Panel(screen) {

    init {
        reload(screen)
    }

    internal fun reload(screen: MenuScreen) {
        clearChildren()
        this + table {
            focusedActor = scrollPane {
                table {
                    WorldSaveManager.getWorlds().forEach { save ->
                        button {
                            background = skin.getDrawable("black-transparent")

                            label(save.name) { it.pad(5f).colspan(2).expandX().left().row() }

                            label("Seed: ${save.seed}", style = "italic-16pt") { it.pad(5f, 5f, 10f, 5f).left() }

                            textButton("Delete", style = "server-delete") {
                                it.right().row()
                                onClick {
                                    WorldSaveManager.deleteWorld(save.location)
                                    reload(screen)
                                }
                            }

                            onClick {
                                screen.localServer(save)
                            }

                            it.width(700f).pad(20f, 0f, 20f, 0f).row()
                        }
                    }
                }
                it.pad(50f, 0f, 50f, 0f).expand().row()
            }

            textButton("Create World") {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f)
                onClick { screen.pushPanel(SingleplayerWorldCreatePanel(screen, this@SingleplayerWorldSelectionPanel)) }
            }

            setFillParent(true)
        }
    }
}