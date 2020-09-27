/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.menu

import com.badlogic.gdx.Input
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.actors.plusAssign
import ktx.scene2d.scene2d
import ktx.scene2d.textField
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.world.WorldSaveManager

/** Panel for creating a new world/save. */
class SingleplayerWorldCreatePanel(screen: MenuScreen, parent: SingleplayerWorldSelectionPanel) : Panel(screen) {


    init {
        this += scene2d.visTable {
            visLabel(I18N["single-add.name"]) { it.pad(20f).row() }
            val nameField = textField { it.width(400f).pad(20f).row() }
            focusedActor = nameField

            visLabel(I18N["single-add.seed"]) { it.pad(20f).row() }
            val seedField = textField(System.currentTimeMillis().toString()) { it.width(400f).pad(20f).padBottom(40f).row() }

            visTextButton(I18N["single-add.button"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick {
                    WorldSaveManager.addWorld(nameField.text, seedField.text)
                    parent.reload(screen)
                    screen.popPanel()
                }
            }

            backButton(screen)
            row()

            onKeyDown { keycode ->
                when (keycode) {
                    Input.Keys.ESCAPE -> screen.popPanel()
                    Input.Keys.ENTER -> {
                        WorldSaveManager.addWorld(nameField.text, seedField.text)
                        parent.reload(screen)
                        screen.popPanel()
                    }
                }
            }

            setFillParent(true)
        }
        clearListeners()
    }
}