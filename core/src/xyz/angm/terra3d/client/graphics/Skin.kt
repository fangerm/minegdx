package xyz.angm.terra3d.client.graphics

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisTextButton
import ktx.assets.file
import ktx.scene2d.Scene2DSkin
import ktx.style.*
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.client.resources.soundPlayer

/** The skin used for all UI objects in the client. */
object Skin {

    /** The height of text buttons in most menus. */
    const val textButtonHeight = 48f

    /** The width of text buttons in most menus. */
    const val textButtonWidth = 400f

    private val fontSizes = listOf(48, 32, 24, 16)
    private val colors5 = mapOf(
        Pair("white", Color.WHITE),
        Pair("light-grey", Color.LIGHT_GRAY),
        Pair("black-transparent", Color(0f, 0f, 0f, 0.5f)),
        Pair("red-transparent", Color(0.3f, 0f, 0f, 0.5f)),
        Pair("black", Color.BLACK),
        Pair("dark-grey", Color.DARK_GRAY),
        Pair("transparent", Color(0f, 0f, 0f, 0f)),
        Pair("dark-green", Color(0.3f, 0.4f, 0.3f, 1f))
    )
    private val colors32 = mapOf(
        Pair("item-selector", Color(1f, 1f, 1f, 0.5f)),
        Pair("red", Color.RED),
        Pair("green", Color.GREEN)
    )

    /** Reload the skin. Only needs to be called on init or when the resource pack changes. */
    fun reload() {
        val regularGenerator = FreeTypeFontGenerator(file(ResourceManager.getFullPath("font/regular.ttf")))
        val italicGenerator = FreeTypeFontGenerator(file(ResourceManager.getFullPath("font/italic.ttf")))
        val monospaceGen = FreeTypeFontGenerator(file(ResourceManager.getFullPath("font/monospace.ttf")))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.shadowColor = Color(0.4f, 0.4f, 0.4f, 0.8f)

        val it = VisUI.getSkin()
        Scene2DSkin.defaultSkin = VisUI.getSkin().apply {
            fontSizes.forEach { size ->
                parameter.size = size
                parameter.shadowOffsetX = size / 10
                parameter.shadowOffsetY = size / 10

                val regular = regularGenerator.generateFont(parameter)
                val italic = italicGenerator.generateFont(parameter)
                regular.data.markupEnabled = true
                italic.data.markupEnabled = true

                add("default-${size}pt", regular)
                add("italic-${size}pt", italic)
            }
            add("default", it.get<BitmapFont>("default-32pt"))
            add("monospace", monospaceGen.generateFont(parameter))

            colors5.forEach { color ->
                val pixmap = Pixmap(5, 5, Pixmap.Format.RGBA8888)
                pixmap.setColor(color.value)
                pixmap.fill()
                add(color.key, Texture(pixmap))
            }
            colors32.forEach { color ->
                val pixmap = Pixmap(32, 32, Pixmap.Format.RGBA8888)
                pixmap.setColor(color.value)
                pixmap.fill()
                add(color.key, Texture(pixmap))
            }

            val guiWidgets = "textures/gui/widgets.png"
            add("button-default", ResourceManager.getTextureRegion(guiWidgets, 0, 132, 400, 40))
            add("button-hover", ResourceManager.getTextureRegion(guiWidgets, 0, 172, 400, 40))
            add("button-disabled", ResourceManager.getTextureRegion(guiWidgets, 0, 92, 400, 40))
            add("logo", ResourceManager.get("textures/gui/title/terra3d.png"))


            add("vis-default", it.get<VisTextButton.VisTextButtonStyle>())
            visTextButton {
                font = it["default-32pt"]
                up = it["button-default"]
                over = it["button-hover"]
            }

            visTextButton("server-delete") {
                font = it["default-16pt"]
                over = it["dark-grey"]
                checked = it["black"]
            }

            getAll(BitmapFont::class.java).forEach { skinFont ->
                label(skinFont.key) { font = skinFont.value }
            }

            label("debug") {
                font = it["monospace"]
                fontColor = Color.WHITE
                background = it["black-transparent"]
            }

            progressBar("default-horizontal") {
                background = it["light-grey"]
                knobBefore = it["white"]
            }

            textField {
                font = it["default-32pt"]
                fontColor = Color.WHITE
                background = it["dark-grey"]
                cursor = it["white"]
                selection = it["dark-grey"]
            }

            visTextField("chat-input") {
                font = it["default-24pt"]
                fontColor = Color.WHITE
                background = it["black-transparent"]
                cursor = it["white"]
                selection = it["dark-grey"]
            }

            button {
                up = it["black"]
                over = it["dark-grey"]
                checked = it["dark-green"]
            }

            button("craft") {
                up = it["dark-grey"]
                over = it["transparent"]
                checked = it["dark-green"]
            }

            scrollPane {}
        }

        regularGenerator.dispose()
        italicGenerator.dispose()
        monospaceGen.dispose()
    }
}


/** A function that will make the given table make a clicking noise when
 * clicked by the user. Used for buttons in the menu and similar. */
fun Table.click() {
    addCaptureListener {
        if ((it as? InputEvent)?.type == InputEvent.Type.touchDown)
            soundPlayer.playSound("random/wood_click")
        false
    }
}
