package xyz.angm.terra3d.client.world

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.Diffuse
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.PixmapTextureData
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ScreenUtils
import ktx.ashley.get
import ktx.collections.*
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent
import xyz.angm.terra3d.common.ecs.components.specific.PlayerComponent
import xyz.angm.terra3d.common.ecs.item
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.log
import kotlin.math.min

/** Resolution of the inventory image for blocks.
 * 32 is the size of ItemActor, bigger size is simply to make it smoother. */
private const val BLOCK_TEX_RES = 128

/** Caches block and entity models to prevent building a new model every time. */
class ModelCache {

    private val attributes = VertexAttributes.Usage.Position.toLong() or
            VertexAttributes.Usage.Normal.toLong() or VertexAttributes.Usage.TextureCoordinates.toLong()
    private val corner000 = Vector3(0f, 0f, 0f)
    private val corner010 = Vector3(0f, 1f, 0f)
    private val corner100 = Vector3(1f, 0f, 0f)
    private val corner110 = Vector3(1f, 1f, 0f)
    private val corner001 = Vector3(0f, 0f, 1f)
    private val corner011 = Vector3(0f, 1f, 1f)
    private val corner101 = Vector3(1f, 0f, 1f)
    private val corner111 = Vector3(1f, 1f, 1f)
    private val tmpV1 = Vector3()

    private var blockDamageModels = emptyArray<ModelInstance>()
    internal lateinit var activeDamageModel: ModelInstance
    private val itemModels = IntMap<Model>()
    private val itemImages = IntMap<Texture>() // Images for blocks in inventories
    private val playerModel: Model
    private val builder = ModelBuilder()

    init {
        playerModel = builder.createBox(0.4f, 1.75f, 0.4f, Material(ColorAttribute(Diffuse, 1f, 0f, 0f, 1f)), attributes)
    }

    fun init() {
        loadBlockDamageModels()
        generateItemImages()
    }

    /** Clears all caches. Needs to be called when resource pack changes. */
    fun clear() {
        blockDamageModels.forEach { it.model.dispose() }
        itemModels.values().forEach { it.dispose() }
        blockDamageModels = emptyArray()
        itemModels.clear()
    }

    /** Get a entity model instance.
     * @param entity Entity to get the model for
     * @return The entities ModelInstance. */
    fun getEntityModelInstance(entity: Entity): ModelInstance {
        return when {
            entity.getComponent(ItemComponent::class.java) != null ->
                ModelInstance(getItemModel(entity[item]!!.item.type))

            entity.getComponent(PlayerComponent::class.java) != null ->
                ModelInstance(playerModel)

            else -> {
                log.error { "[CACHE] Unknown entity type; can't create model!" }
                log.error { "[CACHE] Returning empty model." }
                return ModelInstance(Model())
            }
        }
    }

    fun getItemModel(type: ItemType) = itemModels[type - 1] ?: addItemModelToCache(Item.Properties.fromType(type)!!)

    /** Takes a block type and returns the texture to use for it when displaying in inventories. */
    fun itemImage(type: ItemType) = itemImages[type]!!

    /** Call on position change, or block breakTime change.
     * @param transform Model world transform (block position)
     * @param timePercent The brokenness of the block in percent. */
    fun updateDamageModelPosition(transform: Vector3, timePercent: Float) {
        blockDamageModels.forEach { it.transform.setToTranslation(0f, -10000f, 0f) }
        if (timePercent == 0f) return // Block has no hit, nothing to do

        val modelIndex = min((timePercent / 10f).toInt(), 9)
        val modelInstance = blockDamageModels[modelIndex]
        modelInstance.transform
            .setToTranslation(transform.sub(0.015f, 0.015f, 0.015f))
            .scale(1.03f, 1.03f, 1.03f) // Ensure breakTime is drawn over the block
        activeDamageModel = modelInstance
    }

    private fun loadBlockDamageModels() {
        blockDamageModels = Array(10) { ModelInstance(createBlockModel(tex = "textures/blocks/destroy_stage_$it.png", blend = true)) }
        activeDamageModel = blockDamageModels[0]
    }

    private fun createBlockModel(
        tex: String,
        texSide: String? = null,
        texBottom: String? = null,
        texFront: String? = null,
        blend: Boolean = false
    ): Model {
        val material = Material(TextureAttribute.createDiffuse(ResourceManager.get<Texture>(tex)))
        val materialSide =
            if (texSide != null) Material(TextureAttribute.createDiffuse(ResourceManager.get<Texture>(texSide)))
            else material
        val materialBottom =
            if (texBottom != null) Material(TextureAttribute.createDiffuse(ResourceManager.get<Texture>(texBottom)))
            else material
        val materialFront =
            if (texFront != null) Material(TextureAttribute.createDiffuse(ResourceManager.get<Texture>(texFront)))
            else materialSide

        if (blend) {
            material.set(BlendingAttribute())
            if (material != materialSide) materialSide.set(BlendingAttribute())
            if (material != materialBottom) materialBottom.set(BlendingAttribute())
        }

        builder.begin()
        var nor = tmpV1.set(corner010)
        rect(corner010, corner011, corner111, corner110, nor.scl(-1f), material)
        rect(corner001, corner000, corner100, corner101, nor.scl(-1f), materialBottom)

        nor = tmpV1.set(corner001)
        rect(corner100, corner000, corner010, corner110, nor.scl(-1f), materialFront)
        rect(corner001, corner101, corner111, corner011, nor.scl(-1f), materialSide)

        nor = tmpV1.set(corner100)
        rect(corner000, corner001, corner011, corner010, nor.scl(-1f), materialSide)
        rect(corner101, corner100, corner110, corner111, nor.scl(-1f), materialSide)

        return builder.end()
    }

    private fun rect(v0: Vector3, v1: Vector3, v2: Vector3, v3: Vector3, nor: Vector3, material: Material) {
        builder.part("rect", GL20.GL_TRIANGLES, attributes, material).rect(v0, v1, v2, v3, nor)
    }

    private fun addItemModelToCache(type: Item.Properties): Model {
        val model: Model

        if (type.isBlock) {
            model = createBlockModel(type.texture, type.block?.texSide, type.block?.texBottom, type.block?.texFront, type.block?.isBlend ?: false)
            model.nodes.first()?.scale?.set(0.25f, 0.25f, 0.25f)
            model.nodes.first()?.translation?.set(-0.125f, 0.0f, -0.125f)
        } else {
            val material = Material(TextureAttribute.createDiffuse(ResourceManager.get<Texture>(type.texture)))
            material.set(BlendingAttribute())
            model = builder.createRect(
                -0.2f, 0.01f, -0.2f,
                -0.2f, 0.01f, 0.2f,
                0.2f, 0.01f, 0.2f,
                0.2f, 0.01f, -0.2f,
                0f, 1f, 0f,
                material, attributes
            )
        }
        itemModels[type.type - 1] = model
        return model
    }

    /** Generates item images of blocks.
     * Works by creating a model instance of the block and rendering a side view of it
     * into a framebuffer, from which the texture is then extracted from. */
    private fun generateItemImages() {
        val fbo = FrameBuffer(Pixmap.Format.RGBA8888, BLOCK_TEX_RES, BLOCK_TEX_RES, false)
        val camera = OrthographicCamera(BLOCK_TEX_RES.toFloat(), BLOCK_TEX_RES.toFloat())
        val batch = ModelBatch()
        val environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, 0.8f, -0.2f))

        // Modify these and mess up render positions, i dare you
        camera.far = 8f
        camera.zoom = 0.027f
        camera.position.set(-3f, 4f, -3f)
        camera.up.set(0f, -1f, 0f) // Frame buffers are inverted by default, invert the cam to counter
        camera.lookAt(Vector3.Y)
        camera.update()

        fbo.begin()
        for (item in Item.Properties.allItems) {
            if (item.isBlock) generateItemImage(item.type, camera, batch, environment)
        }
        fbo.end()
    }

    private fun generateItemImage(type: ItemType, camera: Camera, batch: ModelBatch, env: Environment) {
        val block = getItemModel(type)
        val inst = ModelInstance(block)
        inst.transform.setToScaling(8f, 8f, 8f)

        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        batch.begin(camera)
        batch.render(inst, env)
        batch.end()

        val pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, BLOCK_TEX_RES, BLOCK_TEX_RES)
        val data = PixmapTextureData(pixmap, null, false, false)
        val texture = Texture(data)
        pixmap.dispose()
        itemImages[type] = texture
    }
}