package xyz.angm.terra3d.client.ecs.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.MathUtils
import ktx.ashley.get
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.resources.configuration
import xyz.angm.terra3d.common.ecs.localPlayer
import xyz.angm.terra3d.common.ecs.playerM

/** Mouse sensitivity, will be multiplied with [configuration.sensitivity]. */
private const val SENSITIVITY = 0.1f

/** Amount of time the game will wait until registering a right click again when holding the button. */
const val RIGHT_CLICK_COOLDOWN = 0.3f

private const val DEG_TO_RAD = MathUtils.degreesToRadians

/** Used for handling input while in-game.
 * @param screen The game screen */
class PlayerInputHandler(private val screen: GameScreen) : InputAdapter() {

    private var lastX = 0
    private var lastY = 0
    private var yaw = 0f
    private var pitch = 0f
    private var roll = 0f

    private var firstMouseInput = true
    private var active = true
    private var rightClickCooldown = 0f

    internal fun update(delta: Float) {
        if (!active) return

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            screen.playerInputSystem.leftHeld(delta)
        } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && rightClickCooldown < 0f) {
            screen.playerInputSystem.rightClick()
            rightClickCooldown = RIGHT_CLICK_COOLDOWN
        }

        // Check if roll != 0 and slightly reduce it and update if so
        // (Intentionally not a comparison to account for float inaccuracy)
        if (roll > 0.0000f) {
            updateCamera()
            roll -= roll * 10f * delta
        }

        rightClickCooldown -= delta
    }

    /** Call on player hurt, causes camera to tilt shortly */
    fun playerHurt() {
        roll = 0.2f
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == Input.Buttons.LEFT) screen.playerInputSystem.leftUp()
        return true
    }

    /** Searches and executes the action bound to the key */
    override fun keyDown(keycode: Int): Boolean {
        configuration.keybinds[keycode]?.keyDown?.invoke(screen)
        return true
    }

    /** Searches and executes the action bound to the key */
    override fun keyUp(keycode: Int): Boolean {
        configuration.keybinds[keycode]?.keyUp?.invoke(screen)
        return true
    }

    /** Should be called before registering as input handler. */
    fun beforeRegister() {
        configuration.keybinds.forEach { if (Gdx.input.isKeyPressed(it) && !Gdx.input.isKeyJustPressed(it)) keyDown(it) }
        active = true
    }

    /** Should be called before unregistering as input handler. */
    fun beforeUnregister() {
        configuration.keybinds.forEach { if (Gdx.input.isKeyPressed(it)) keyUp(it) }
        firstMouseInput = true
        active = false
    }

    /** Moves the camera according to the mouse. */
    override fun mouseMoved(xPos: Int, yPos: Int): Boolean {
        // Math code is copied from https://learnopengl.com/Getting-started/Camera, with some changes to fit libGDX
        if (firstMouseInput) {
            lastX = xPos
            lastY = yPos
            firstMouseInput = false
        }

        val xOffset = (xPos - lastX).toFloat() * SENSITIVITY * configuration.sensitivity
        val yOffset = (lastY - yPos).toFloat() * SENSITIVITY * configuration.sensitivity
        lastX = xPos
        lastY = yPos

        yaw += xOffset
        pitch += yOffset

        // Prevent overshooting pitch axis
        if (pitch > 89.0f) pitch = 89.0f
        if (pitch < -89.0f) pitch = -89.0f

        screen.cam.direction.set(
            MathUtils.cos(yaw * DEG_TO_RAD) * MathUtils.cos(pitch * DEG_TO_RAD),
            MathUtils.sin(pitch * DEG_TO_RAD),
            MathUtils.sin(yaw * DEG_TO_RAD) * MathUtils.cos(pitch * DEG_TO_RAD)
        ).nor()
        updateCamera()
        screen.player[localPlayer]!!.transform.setFromEulerAngles(yaw, pitch, 0f)
        return true
    }

    /** Called when camera needs update (mouse was moved or roll is not 0). */
    private fun updateCamera() {
        screen.cam.up.set(roll, 1f, roll)
        screen.cam.update()
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = mouseMoved(screenX, screenY)

    /** Scrolls players hotbar position. */
    override fun scrolled(amount: Int): Boolean {
        screen.player[playerM]!!.inventory.scrollHotbarPosition(amount)
        screen.gameplayPanel.updateHotbarSelector(screen.player[playerM]!!.inventory.hotbarPosition)
        return true
    }
}