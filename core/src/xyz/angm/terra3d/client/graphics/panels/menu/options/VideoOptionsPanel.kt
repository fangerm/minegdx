/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.menu.options

import ktx.actors.onClick
import ktx.actors.plusAssign
import ktx.scene2d.scene2d
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextTooltip
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.configuration

/** The highest level of shadow quality allowed. Higher than 3 leads to a too
 * big shadow FBO (32768x32768) leading to crashes. */
private const val MAX_QUALITY = 3

/** Video options menu. */
class VideoOptionsPanel(screen: MenuScreen) : Panel(screen) {

    init {
        reload(screen)
    }

    fun reload(screen: MenuScreen) {
        clearChildren()
        this += scene2d.visTable {
            visTextButton("${I18N["options.video.blend"]}: ${I18N[configuration.video.blend.toString()]}") {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick {
                    configuration.video.blend = !configuration.video.blend
                    reload(screen)
                }
                visTextTooltip(I18N["options.video.blendTooltip"])
            }

            visTextButton("${I18N["options.video.shadowQuality"]}: ${configuration.video.shadowQuality}") {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()
                onClick {
                    configuration.video.shadowQuality++
                    if (configuration.video.shadowQuality > MAX_QUALITY) configuration.video.shadowQuality = 0
                    reload(screen)
                }
                visTextTooltip(I18N["options.video.shadowQualityTooltip"])
            }

            backButton(screen)

            setFillParent(true)
        }
    }
}