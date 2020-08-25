package xyz.angm.terra3d.client.graphics

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import ktx.assets.file
import ktx.scene2d.Scene2DSkin
import ktx.style.*
import xyz.angm.terra3d.client.resources.ResourceManager

/** The skin used for all UI objects in the client. */
object Skin {

    /** The height of text buttons in most menus. */
    const val textButtonHeight = 48f

    /** The width of text buttons in most menus. */
    const val textButtonWidth = 400f

    private val fontSizes = listOf(16, 24, 32, 48)
    private val colors5 = mapOf(
        Pair("white", Color.WHITE),
        Pair("light-grey", Color.LIGHT_GRAY),
        Pair("black-transparent", Color(0f, 0f, 0f, 0.5f)),
        Pair("red-transparent", Color(0.3f, 0f, 0f, 0.5f)),
        Pair("black", Color.BLACK),
        Pair("dark-grey", Color.DARK_GRAY),
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
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.shadowColor = Color(0.4f, 0.4f, 0.4f, 0.8f)

        Scene2DSkin.defaultSkin = skin {
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
            add("logo-editor", ResourceManager.get("textures/gui/title/terra3d-editor.png"))

            textButton {
                font = it["default-32pt"]
                up = it["button-default"]
                over = it["button-hover"]
            }

            textButton("server-delete") {
                font = it["default-16pt"]
                over = it["dark-grey"]
                checked = it["black"]
            }

            getAll(BitmapFont::class.java).forEach { skinFont ->
                label(skinFont.key) { font = skinFont.value }
            }

            label("pack-loading") {
                font = it["default-48pt"]
                fontColor = Color.ORANGE
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

            textField("chat-input") {
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

            list {
                font = it["default-32pt"]
                fontColorSelected = Color.WHITE
                fontColorUnselected = Color.LIGHT_GRAY
                background = it["black"]
                selection = it["light-grey"]
            }

            scrollPane {}

            selectBox {
                font = it["default-32pt"]
                fontColor = Color.WHITE
                background = it["black"]
                backgroundOver = it["dark-grey"]
                listStyle = it["default"]
                scrollStyle = it["default"]
            }

            checkBox {
                checkboxOff = it["red"]
                checkboxOn = it["green"]
                font = it["default-32pt"]
            }
        }

        regularGenerator.dispose()
        italicGenerator.dispose()
    }
}