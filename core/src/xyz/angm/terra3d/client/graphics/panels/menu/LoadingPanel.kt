/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/19/20, 6:57 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.menu

import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisProgressBar
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.ResourceManager

/** Loading panel shown on boot. */
class LoadingPanel(private val screen: MenuScreen) : Panel(screen) {

    private val loadingLabel = VisLabel(I18N["loading-assets"])
    private val loadingPercentLabel = VisLabel("0%")
    private val loadingBar = VisProgressBar(0f, 1f, 0.01f, false)

    private var done = false

    init {
        add(loadingLabel).colspan(2).row()
        add(loadingPercentLabel).pad(10f)
        add(loadingBar).width(250f).pad(10f)
    }

    override fun act(delta: Float) {
        super.act(delta)
        val progress = ResourceManager.continueLoading()
        loadingBar.value = progress
        loadingPercentLabel.setText(String.format("%02d", (progress * 100).toInt()) + "%")

        if (progress == 1f && !done) {
            screen.doneLoading()
            done = true
        }
    }
}