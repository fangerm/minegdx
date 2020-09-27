/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/20/20, 9:46 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import ktx.actors.alpha

import xyz.angm.terra3d.client.graphics.actors.Chat
import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.actors.ItemTooltip
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.ecs.health
import xyz.angm.terra3d.common.ecs.localPlayer
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.items.Item
import kotlin.random.Random

/** The HUD during gameplay. Contains everything that is 2D. */
class GameplayOverlay(private val screen: GameScreen) : Panel(screen) {

    private val hotbar = Image(ResourceManager.getTextureRegion("textures/gui/widgets.png", 0, 0, 364, 44))
    private val hotbarSelected = Image(ResourceManager.getTextureRegion("textures/gui/widgets.png", 0, 44, 48, 48))
    private val blockLabel = Label("", skin, "default-24pt")
    private val blockTooltip = ItemTooltip(this)
    private val debugLabel = Label("", skin, "monospace")
    private val onlinePlayers = Label("", skin, "default-24pt")
    private val chat = Chat(skin, screen.client)
    private val fluidOverlay = Image(ResourceManager.get<Texture>(Item.Properties.fromIdentifier("water").texture))
    var inFluid: Boolean
        get() = fluidOverlay.isVisible
        set(value) {
            fluidOverlay.isVisible = value
        }

    init {
        val icons = "textures/gui/icons.png"
        val hotbarItems = ItemGroup(null, screen.playerInventory, row = 1, column = 9)
        val crosshair = Image(ResourceManager.getTextureRegion(icons, 0, 0, 32, 32))
        val healthBar = IconGroup(
            ResourceManager.getTextureRegion(icons, 32, 0, 18, 18),
            ResourceManager.getTextureRegion(icons, 122, 0, 18, 18),
            ResourceManager.getTextureRegion(icons, 104, 0, 18, 18)
        ) { screen.player[health].health }
        val hungerBar = IconGroup(
            ResourceManager.getTextureRegion(icons, 32, 54, 18, 18),
            ResourceManager.getTextureRegion(icons, 122, 54, 18, 18),
            ResourceManager.getTextureRegion(icons, 104, 54, 18, 18)
        ) { screen.player[playerM].hunger }

        addActor(hotbar)
        addActor(hotbarSelected)
        addActor(blockLabel)
        addActor(blockTooltip)
        addActor(debugLabel)
        addActor(onlinePlayers)
        addActor(hotbarItems)
        addActor(crosshair)
        addActor(healthBar)
        addActor(hungerBar)
        addActor(chat)
        addActor(fluidOverlay)

        hotbar.setSize(364f, 44f)
        hotbarSelected.setSize(48f, 48f)
        crosshair.setSize(32f, 32f)
        fluidOverlay.setSize(WORLD_WIDTH, WORLD_HEIGHT)
        fluidOverlay.alpha = 0.2f

        hotbar.setPosition(WORLD_WIDTH / 2, hotbar.height / 2, Align.center)
        hotbarSelected.setPosition(0f, hotbar.height / 2, Align.center)
        blockTooltip.setPosition(0f, WORLD_HEIGHT, Align.topLeft)
        hotbarItems.setPosition(hotbar.x + 2f, hotbar.y + 2f)
        crosshair.setPosition(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, Align.center)
        debugLabel.setPosition(5f, WORLD_HEIGHT - 200, Align.topLeft)
        onlinePlayers.setPosition(WORLD_WIDTH - 400, WORLD_HEIGHT / 3)
        healthBar.setPosition(hotbar.x, hotbar.height + 6, Align.bottomLeft)
        hungerBar.setPosition(hotbar.x + hotbar.width + 16, hotbar.height + 6, Align.bottomRight)
        chat.setPosition(10f, 90f)
        fluidOverlay.setPosition(0f, 0f)

        debugLabel.isVisible = false
        onlinePlayers.isVisible = false
        fluidOverlay.isVisible = false
        updateHotbarSelector(screen.player[playerM].inventory.hotbarPosition)
        background = null
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (debugLabel.isVisible) debugLabel.setText(getDebugLabelString())
        if (onlinePlayers.isVisible) updateOnlinePlayers()

        val block = screen.world.getBlock(screen.player[localPlayer].blockLookingAt)
        blockTooltip.update(block)
        blockTooltip.setPosition(0f, WORLD_HEIGHT, Align.topLeft)
    }

    /** Update the hotbar selected sprite.
     * @param position It's new position */
    fun updateHotbarSelector(position: Int) {
        hotbarSelected.x = hotbar.x - 2 + (position * 40)
        blockLabel.clearActions()
        blockLabel.isVisible = true
        blockLabel.color.a = 1f
        blockLabel.setText(screen.playerInventory[position]?.properties?.name ?: "")
        blockLabel.addAction(Actions.sequence(
            Actions.fadeOut(3f, Interpolation.pow2),
            Actions.visible(false),
        ))
        blockLabel.setPosition(hotbarSelected.x, 120f, Align.center)
    }

    private fun updateOnlinePlayers() {
        val s = StringBuilder()
        s.append(I18N["players-online"])
        screen.onlinePlayers.forEach { s.append("\n$it") }
        onlinePlayers.setText(s)
    }

    /** Displays the chat, without it fading. */
    fun displayChat() = chat.update(fade = false)

    /** Toggle the debug menu/info. */
    fun toggleDebugInfo() {
        debugLabel.isVisible = !debugLabel.isVisible
    }

    /** Toggle the online players list. */
    fun toggleOnlinePlayers() {
        onlinePlayers.isVisible = !onlinePlayers.isVisible
    }

    private fun getDebugLabelString() =
        """
        FPS: ${Gdx.graphics.framesPerSecond}
        Time since last frame: ${(Gdx.graphics.deltaTime * 1000).format(1)}ms
        ${
            if (screen.bench.time.count == 0) "Profiling disabled in this build"
            else "Average time in render(): ${(screen.bench.time.average * 1000).format(1)}ms\n" +
                    "        Mean time in render(): ${(screen.bench.time.mean.mean * 1000).format(1)}ms"
        }

        Heap Size: ${Runtime.getRuntime().totalMemory()}
        Heap Free: ${Runtime.getRuntime().freeMemory()}

        OpenGL ${Gdx.graphics.glVersion.majorVersion}: ${Gdx.graphics.glVersion.rendererString}
        Display: ${Gdx.graphics.displayMode}

        Player position: ${screen.player[position].toStringFloor()} / ${screen.player[position]}
        Camera direction: ${screen.cam.direction}
        
        Chunks loaded: ${screen.world.chunksLoaded}
        Chunks waiting to be rendered: ${screen.world.waitingForRender}
        Entities loaded: ${screen.entitiesLoaded}
        ECS systems active: ${screen.systemsActive}
        """.trimIndent()

    private fun Float.format(digits: Int) = "%.${digits}f".format(this)

    /** Add a chat message and display the chat. */
    fun addChatMessage(message: String) = chat.addMessage(message)

    // A bar displayed as an array of sprites. Every sprite can be empty/half/full.
    // The getter is required, since primitive types (kotlin.Int compiles to JVM primitive int) get passed by value, not reference
    private class IconGroup(
        private val empty: TextureRegion, private val half: TextureRegion, private val full: TextureRegion,
        private val valueGetter: () -> Int
    ) : Actor() {

        private val iconSize = 18f
        private val pad = -2f
        private val wiggleRoom = 1f
        private val wiggleFreq = 1f / 15f // in seconds

        private val random = Random(System.currentTimeMillis())
        private val yOffsets = Array(10) { 0f }
        private var sinceOffsetUpdate = 0f

        init {
            width = iconSize * 10
            height = iconSize
        }

        override fun draw(batch: Batch, parentAlpha: Float) {
            var valueLeft = valueGetter()
            for (i in 0 until 10) {
                batch.draw(empty, x + (i * (iconSize + pad)), y + yOffsets[i], iconSize, iconSize)
                if (valueLeft >= 2) batch.draw(full, x + (i * (iconSize + pad)), y + yOffsets[i], iconSize, iconSize)
                else if (valueLeft == 1) batch.draw(half, x + (i * (iconSize + pad)), y + yOffsets[i], iconSize, iconSize)
                valueLeft -= 2
            }
        }

        override fun act(delta: Float) {
            sinceOffsetUpdate += delta
            if (sinceOffsetUpdate > wiggleFreq) {
                sinceOffsetUpdate = 0f
                for (i in 0 until 10) {
                    yOffsets[i] = if (valueGetter() < 5) ((random.nextFloat() * (wiggleRoom * 2)) - wiggleRoom) else 0f
                }
            }
        }
    }
}