package xyz.angm.terra3d.client.graphics.render

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
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
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.client.resources.configuration
import kotlin.math.abs
import kotlin.math.max

/** The relative offset between sun/player in X direction */
private const val SUN_OFFSET_X = 1f

/** The relative offset between sun/player in Y direction */
private const val SUN_OFFSET_Y = 0.7f

/** The relative offset between sun/player in Z direction */
private const val SUN_OFFSET_Z = 0.6f

/** Multiplier used for the sun position. Multiply with the values
 * above to get position of the sun from the player. */
private const val SUN_MUL = 150

/** The movement speed of the sun. */
private const val SUN_SPEED = 0.1f

/** This class is part of Renderer and responsible for handling
 * rendering and related graphics data for lighting effects and the day/night cycle. */
internal class Environment : Disposable {

    val environment = Environment()
    private val shadowLight = DirectionalShadowLight(configuration.video.shadowFBO, configuration.video.shadowFBO, 300f, 300f, -50f, 350f)
    private val shadowBatch = ModelBatch(DepthShaderProvider())
    private val sun = getSunModel()
    private var sunTime = 0f

    init {
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -SUN_OFFSET_X, -SUN_OFFSET_Y, -SUN_OFFSET_Z))
        shadowLight.set(0.8f, 0.8f, 0.8f, -SUN_OFFSET_X, -SUN_OFFSET_Y, -SUN_OFFSET_Z)
        environment.add(shadowLight)
        environment.shadowMap = shadowLight
    }

    /** Renders shadows. Called before any other rendering. */
    fun preRender(renderer: Renderer, delta: Float) {
        sunTime += delta * SUN_SPEED
        val cam = renderer.cam
        val sinT = MathUtils.sin(sunTime)
        val cosT = MathUtils.cos(sunTime)

        shadowLight.camera.position.set(
            cam.position.x + SUN_OFFSET_X * SUN_MUL * -cosT,
            cam.position.y + (SUN_OFFSET_Y * SUN_MUL) * max(abs(sinT), 0.1f),
            cam.position.z + SUN_OFFSET_Z * SUN_MUL * cosT
        )
        shadowLight.camera.lookAt(cam.position)
        shadowLight.camera.update()
        sun.transform.setToLookAt(cam.position, Vector3.Y)
        sun.transform.setTranslation(shadowLight.camera.position)

        shadowLight.begin()
        shadowBatch.begin(shadowLight.camera)
        renderer.renderWorld(shadowBatch, shadowLight.camera, null)
        shadowBatch.end()
        shadowLight.end()
    }

    /** Renders the sun. Called during world rendering. */
    fun render(batch: ModelBatch) = batch.render(sun)

    override fun dispose() {
        sun.model.dispose()
        shadowBatch.dispose()
        shadowLight.dispose()
    }

    companion object {
        private fun getSunModel(): ModelInstance {
            val material = Material(TextureAttribute.createDiffuse(ResourceManager.get<Texture>("textures/sun.png")), BlendingAttribute())
            val attributes = VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.TextureCoordinates.toLong()
            val builder = ModelBuilder()
            return ModelInstance(builder.createBox(5f, 5f, 5f, material, attributes))
        }
    }
}
