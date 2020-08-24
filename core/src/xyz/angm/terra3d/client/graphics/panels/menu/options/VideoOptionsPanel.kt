package xyz.angm.terra3d.client.graphics.panels.menu.options

import ktx.actors.plus
import ktx.scene2d.label
import ktx.scene2d.table
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen

/** Panel for adjusting video and render related options. */
class VideoOptionsPanel(screen: MenuScreen) : Panel(screen) {

    init {
        this + table {
            setFillParent(true)
            label("Coming soon!") {
                it.pad(20f)
            }
        }
    }
}