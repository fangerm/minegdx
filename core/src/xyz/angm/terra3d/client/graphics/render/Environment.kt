package xyz.angm.terra3d.client.graphics.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.PI
import com.badlogic.gdx.utils.Disposable
import ktx.assets.disposeSafely
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.client.resources.configuration
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/** The relative offset between sun/player in X direction */
private const val SUN_OFFSET_X = 1f

/** The relative offset between sun/player in Y direction */
private const val SUN_OFFSET_Y = 0.7f

/** The relative offset between sun/player in Z direction */
private const val SUN_OFFSET_Z = 0.6f

/** Multiplier used for the sun position. Multiply with the values
 * above to get position of the sun from the player. */
private const val SUN_MUL = 150

// These colors are used for the day/night cycle and the various lights in it.
// To get the color at a specific point in the cycle, the colors are linearly interpolated.
// Ambient: Color for the base/stray 'ambient' lighting
// Directional: Color for directional and shadow lighting
// Sky: Color used for GlClear, essentially sky color
private val DAYLIGHT_AMBIENT = Color(0.4f, 0.4f, 0.4f, 1f)
private val DAYLIGHT_DIRECTIONAL = Color(0.8f, 0.8f, 0.8f, 1f)
private val DAYLIGHT_SKY = Color(0.525f, 0.675f, 0.98f, 1f)
private val NIGHTLIGHT_AMBIENT = Color(0.05f, 0.05f, 0.1f, 1f)
private val NIGHTLIGHT_DIRECTIONAL = Color(0.08f, 0.08f, 0.12f, 1f)
private val NIGHTLIGHT_SKY = Color(0.03f, 0.03f, 0.08f, 1f)

/** This class is part of Renderer and responsible for handling
 * rendering and related graphics data for lighting effects and the day/night cycle. */
internal class Environment : Disposable {

    private val ambientC = Color(DAYLIGHT_AMBIENT)
    private val directionalC = Color(DAYLIGHT_DIRECTIONAL)
    private val skyC = Color(DAYLIGHT_SKY)

    val environment = Environment()
    private val ambientLight = ColorAttribute(ColorAttribute.AmbientLight)
    private val directionalLight = DirectionalLight()
    private val shadowLight = DirectionalShadowLight(configuration.video.shadowFBO, configuration.video.shadowFBO, 300f, 300f, -50f, 350f)
    private val shadowBatch = ModelBatch(DepthShaderProvider())
    private val sun = getCelestialModel("sun")
    private val moon = getCelestialModel("moon")

    init {
        environment.set(ambientLight)
        environment.add(directionalLight)
        environment.add(shadowLight)
        environment.shadowMap = shadowLight
    }

    /** Renders sky & shadows. Called before any other rendering. */
    fun preRender(renderer: Renderer, dayTime: Float) {
        updatePositions(renderer.cam, dayTime)

        shadowLight.begin()
        shadowBatch.begin(shadowLight.camera)
        renderer.renderWorld(shadowBatch, shadowLight.camera, null)
        shadowBatch.end()
        shadowLight.end()

        Gdx.gl.glClearColor(skyC.r, skyC.g, skyC.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
    }

    /** Renders the sun. Called during world rendering. */
    fun render(batch: ModelBatch) {
        batch.render(sun)
        batch.render(moon)
    }

    private fun updatePositions(cam: Camera, dayTime: Float) {
        val sinT = MathUtils.sin(dayTime)
        val cosT = MathUtils.cos(dayTime)

        ambientC.set(NIGHTLIGHT_AMBIENT)
        directionalC.set(NIGHTLIGHT_DIRECTIONAL)
        skyC.set(NIGHTLIGHT_SKY)

        if (dayTime < PI) {
            val cA = 1 - abs(cosT).pow(7)
            ambientC.lerp(DAYLIGHT_AMBIENT, cA)
            directionalC.lerp(DAYLIGHT_DIRECTIONAL, cA)
            skyC.lerp(DAYLIGHT_SKY, cA)
        }

        ambientLight.color.set(ambientC)
        directionalLight.set(
            directionalC,
            -SUN_OFFSET_X * -cosT,
            -SUN_OFFSET_Y * max(abs(sinT), 0.1f),
            -SUN_OFFSET_Z * cosT
        )
        shadowLight.camera.position.set(
            cam.position.x + SUN_OFFSET_X * SUN_MUL * -cosT,
            cam.position.y + (SUN_OFFSET_Y * SUN_MUL) * max(abs(sinT), 0.1f),
            cam.position.z + SUN_OFFSET_Z * SUN_MUL * cosT
        )
        shadowLight.setColor(directionalC)
        shadowLight.camera.lookAt(cam.position)
        shadowLight.camera.update()

        sun.transform.setTranslation(
            cam.position.x + SUN_OFFSET_X * SUN_MUL * -cosT,
            cam.position.y + (SUN_OFFSET_Y * SUN_MUL) * sinT,
            cam.position.z + SUN_OFFSET_Z * SUN_MUL * cosT
        )
        moon.transform.setTranslation(
            cam.position.x + SUN_OFFSET_X * SUN_MUL * cosT,
            cam.position.y + (SUN_OFFSET_Y * SUN_MUL) * -sinT,
            cam.position.z + SUN_OFFSET_Z * SUN_MUL * -cosT
        )
    }

    override fun dispose() {
        sun.model.disposeSafely() // Sometimes complains about buffers?
        moon.model.disposeSafely() // ^^^
        shadowBatch.dispose()
        shadowLight.dispose()
    }

    companion object {
        private fun getCelestialModel(name: String): ModelInstance {
            val material = Material(TextureAttribute.createDiffuse(ResourceManager.get<Texture>("textures/$name.png")), BlendingAttribute())
            val attributes = VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.TextureCoordinates.toLong()
            val builder = ModelBuilder()
            return ModelInstance(builder.createBox(5f, 5f, 5f, material, attributes))
        }
    }
}
