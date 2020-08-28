/*
 * Developed by Felix Ang. (felix.ang@pm.me).
 * Last modified on 6/21/19 6:57 PM.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.panels.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import ktx.ashley.get
import xyz.angm.terra3d.client.graphics.actors.Chat
import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.actors.ItemTooltip
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.ecs.health
import xyz.angm.terra3d.common.ecs.localPlayer
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.ecs.position
import kotlin.random.Random

/** The HUD during gameplay. Contains everything that is 2D. */
class GameplayOverlay(private val screen: GameScreen) : Panel(screen) {

    private val hotbar = Image(ResourceManager.getTextureRegion("textures/gui/widgets.png", 0, 0, 364, 44))
    private val hotbarSelected = Image(ResourceManager.getTextureRegion("textures/gui/widgets.png", 0, 44, 48, 48))
    private val blockTooltip = ItemTooltip(this)
    private val debugLabel = Label("", skin["monospace", Label.LabelStyle::class.java])
    private val chat = Chat(skin, screen.client)

    init {
        val icons = "textures/gui/icons.png"
        val hotbarItems = ItemGroup(null, screen.playerInventory, rows = 1, columns = 9, itemOffsetX = 40)
        val crosshair = Image(ResourceManager.getTextureRegion(icons, 0, 0, 32, 32))
        val healthBar = IconGroup(
            ResourceManager.getTextureRegion(icons, 32, 0, 18, 18),
            ResourceManager.getTextureRegion(icons, 122, 0, 18, 18),
            ResourceManager.getTextureRegion(icons, 104, 0, 18, 18)
        ) { screen.player[health]!!.health }
        val hungerBar = IconGroup(
            ResourceManager.getTextureRegion(icons, 32, 54, 18, 18),
            ResourceManager.getTextureRegion(icons, 122, 54, 18, 18),
            ResourceManager.getTextureRegion(icons, 104, 54, 18, 18)
        ) { screen.player[playerM]!!.hunger }

        addActor(hotbar)
        addActor(hotbarSelected)
        addActor(blockTooltip)
        addActor(debugLabel)
        addActor(hotbarItems)
        addActor(crosshair)
        addActor(healthBar)
        addActor(hungerBar)
        addActor(chat)

        hotbar.setSize(364f, 44f)
        hotbarSelected.setSize(48f, 48f)
        crosshair.setSize(32f, 32f)

        hotbar.setPosition(WORLD_WIDTH / 2, hotbar.height / 2, Align.center)
        hotbarSelected.setPosition(0f, hotbar.height / 2, Align.center)
        blockTooltip.setPosition(0f, WORLD_HEIGHT, Align.topLeft)
        hotbarItems.setPosition(hotbar.x + 6, hotbar.y + 6)
        crosshair.setPosition(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, Align.center)
        debugLabel.setPosition(5f, WORLD_HEIGHT - 175, Align.topLeft)
        healthBar.setPosition(hotbar.x, hotbar.height + 6, Align.bottomLeft)
        hungerBar.setPosition(hotbar.x + hotbar.width + 16, hotbar.height + 6, Align.bottomRight)
        chat.setPosition(10f, 90f)

        debugLabel.isVisible = false
        updateHotbarSelector(0)
        background = null
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (debugLabel.isVisible) debugLabel.setText(getDebugLabelString())
        val block = screen.world.getBlock(screen.player[localPlayer]!!.blockLookingAt)
        blockTooltip.update(block)
        blockTooltip.setPosition(0f, WORLD_HEIGHT, Align.topLeft)
    }

    /** Update the hotbar selected sprite.
     * @param position It's new position */
    fun updateHotbarSelector(position: Int) {
        hotbarSelected.x = hotbar.x - 2 + (position * 40)
    }

    /** Displays the chat, without it fading. */
    fun displayChat() = chat.update(fade = false)

    /** Toggle the debug menu/info. */
    fun toggleDebugInfo() {
        debugLabel.isVisible = !debugLabel.isVisible
    }

    private fun getDebugLabelString() =
        """
        FPS: ${Gdx.graphics.framesPerSecond}
        Time since last frame: ${(Gdx.graphics.deltaTime * 1000).toInt()}ms
        Average time in render(): ${(screen.bench.time.average * 1000).toInt()}ms
        Mean time in render(): ${(screen.bench.time.mean.mean * 1000).toInt()}ms

        OpenGL ${Gdx.graphics.glVersion.majorVersion}: ${Gdx.graphics.glVersion.rendererString}
        Display: ${Gdx.graphics.displayMode}

        Player position: ${screen.player[position]!!.toStringFloor()} / ${screen.player[position]!!}
        Camera direction: ${screen.cam.direction}
        
        Chunks loaded: ${screen.world.chunksLoaded}
        Chunks waiting to be rendered: ${screen.world.waitingForRender}
        Entities loaded: ${screen.entitiesLoaded}
        ECS systems active: ${screen.systemsActive}
        """.trimIndent()

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