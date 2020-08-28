package xyz.angm.terra3d.client.graphics.panels.menu.options

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.file.FileChooser
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter
import com.kotcrab.vis.ui.widget.file.FileTypeFilter
import ktx.actors.centerPosition
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.*
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.MenuScreen
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.client.resources.configuration

/** Panel for selecting the resource pack to use. */
class ResourcePackPanel(private val screen: MenuScreen) : Panel(screen) {

    private var selectedPack = ResourceManager.pack
    private val buttons = ButtonGroup<KButton>()

    init {
        buttons.setMinCheckCount(1)
        buttons.setMaxCheckCount(1)
        buttons.uncheckAll()

        this + table {
            focusedActor = scrollPane {
                table {
                    ResourceManager.availablePacks.forEach { pack ->
                        buttons.add(button {
                            background = skin.getDrawable("black-transparent")

                            label(pack.name) { it.pad(5f).colspan(2).expandX().left().row() }

                            label(pack.description, style = "italic-16pt") { it.pad(5f, 5f, 10f, 5f).left() }

                            onClick { selectedPack = pack }

                            isChecked = (pack == selectedPack)

                            it.width(700f).pad(20f, 0f, 20f, 0f).row()
                        })
                    }
                }
                it.colspan(4).pad(50f, 0f, 50f, 0f).expand().row()
            }

            backButton(this, screen)

            textButton(I18N["apply"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f)
                onClick {
                    if (ResourceManager.pack != selectedPack) {
                        ResourceManager.pack = selectedPack
                        configuration.resourcePack = selectedPack
                        configuration.save()
                        screen.reload()
                    } else screen.popPanel()
                }
            }

            textButton(I18N["res.import"]) {
                it.height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f)
                onClick { importDialog() }
            }

            setFillParent(true)
        }
    }

    private fun importDialog() {
        FileChooser.setDefaultPrefsName("xyz.angm.terra3d.client.ui.panels")
        val filter = FileTypeFilter(false)
        filter.addRule("Zipped packs", "zip")

        val chooser = FileChooser(FileChooser.Mode.OPEN)
        chooser.selectionMode = FileChooser.SelectionMode.FILES_AND_DIRECTORIES
        chooser.setFileTypeFilter(filter)
        chooser.setListener(object : FileChooserAdapter() {
            override fun selected(files: Array<FileHandle>) {
                removeActor(chooser)
                Gdx.input.inputProcessor = null

                val loadingThread = Thread {
                    ResourceManager.importMinecraftPack(files.first())
                    Gdx.app.postRunnable {
                        Gdx.input.inputProcessor = stage
                        screen.popPanel()
                        screen.pushPanel(ResourcePackPanel(screen))
                    }
                }
                loadingThread.start()

                val messageLabel = Label(I18N["wait"], skin, "pack-loading")
                messageLabel.width = WORLD_WIDTH
                messageLabel.height = WORLD_HEIGHT
                messageLabel.setAlignment(Align.center)
                addActor(messageLabel)
                messageLabel.centerPosition()
            }
        })
        addActor(chooser)
    }
}