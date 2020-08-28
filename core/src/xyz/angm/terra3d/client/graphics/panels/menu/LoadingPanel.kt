package xyz.angm.terra3d.client.graphics.panels.menu

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.ResourceManager

/** Loading panel shown on boot. */
class LoadingPanel(private val screen: MenuScreen) : Panel(screen) {

    private val loadingLabel = Label(I18N["loading-assets"], skin)
    private val loadingPercentLabel = Label("0%", skin)
    private val loadingBar = ProgressBar(0f, 1f, 0.01f, false, skin)

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