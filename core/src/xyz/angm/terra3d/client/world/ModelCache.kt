package xyz.angm.terra3d.client.world

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.Diffuse
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.IntMap
import ktx.ashley.get
import ktx.collections.*
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.ecs.components.specific.ItemComponent
import xyz.angm.terra3d.common.ecs.components.specific.PlayerComponent
import xyz.angm.terra3d.common.ecs.item
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.log
import kotlin.math.min

/** Caches block and entity models to prevent building a new model every time. */
class ModelCache(private val resourceManager: ResourceManager) {

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
    private val tmpV2 = Vector3()

    private var blockDamageModels = emptyArray<ModelInstance>()
    internal lateinit var activeDamageModel: ModelInstance
    private val itemModels = IntMap<Model>()
    private val playerModel: Model
    private val builder = ModelBuilder()

    init {
        playerModel = builder.createBox(0.4f, 1.75f, 0.4f, Material(ColorAttribute(Diffuse, 1f, 0f, 0f, 1f)), attributes)
    }

    fun init() = loadBlockDamageModels()

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
                ModelInstance(itemModels[entity[item]!!.item.type - 1] ?: addItemModelToCache(entity[item]!!.item.properties))

            entity.getComponent(PlayerComponent::class.java) != null ->
                ModelInstance(playerModel)

            else -> {
                log.error { "[CACHE] Unknown entity type; can't create model!" }
                log.error { "[CACHE] Returning empty model." }
                return ModelInstance(Model())
            }
        }
    }

    /** Call on position change, or block breakTime change.
     * @param transform Model world transform (block position)
     * @param timePercent The brokenness of the block in percent. */
    fun updateDamageModelPosition(transform: Vector3, timePercent: Float) {
        blockDamageModels.forEach { it.transform.setToTranslation(0f, -10000f, 0f) }
        if (timePercent == 0f) return // Block has no hit, nothing to do

        val modelIndex = min((timePercent / 10f).toInt(), 9)
        val modelInstance = blockDamageModels[modelIndex]
        modelInstance.transform
            .setToTranslation(transform.sub(0.005f, 0.005f, 0.005f))
            .scale(1.01f, 1.01f, 1.01f) // Ensure breakTime is drawn over the block
        activeDamageModel = modelInstance
    }

    private fun loadBlockDamageModels() {
        blockDamageModels = Array(10) { ModelInstance(createBlockModel(tex = "textures/blocks/destroy_stage_$it.png", blend = true)) }
        activeDamageModel = blockDamageModels[0]
    }

    // TODO: A bunch of this vector math seems really unnecessary
    private fun createBlockModel(tex: String, texSide: String? = null, texBottom: String? = null, blend: Boolean = false): Model {
        val material = Material(TextureAttribute.createDiffuse(resourceManager.get<Texture>(tex)))
        val materialSide =
            if (texSide != null) Material(TextureAttribute.createDiffuse(resourceManager.get<Texture>(texSide)))
            else material
        val materialBottom =
            if (texBottom != null) Material(TextureAttribute.createDiffuse(resourceManager.get<Texture>(texBottom)))
            else material

        if (blend) {
            material.set(BlendingAttribute())
            if (material != materialSide) materialSide.set(BlendingAttribute())
            if (material != materialBottom) materialBottom.set(BlendingAttribute())
        }

        builder.begin()
        var nor = tmpV1.set(corner000).lerp(corner101, 0.5f).sub(tmpV2.set(corner010).lerp(corner111, 0.5f)).nor()
        rect(corner010, corner011, corner111, corner110, nor.scl(-1f), material)
        rect(corner001, corner000, corner100, corner101, nor, materialBottom)

        nor = tmpV1.set(corner000).lerp(corner110, 0.5f).sub(tmpV2.set(corner001).lerp(corner111, 0.5f)).nor()
        rect(corner100, corner000, corner010, corner110, nor, materialSide)
        rect(corner001, corner101, corner111, corner011, nor.scl(-1f), materialSide)

        nor = tmpV1.set(corner000).lerp(corner011, 0.5f).sub(tmpV2.set(corner100).lerp(corner111, 0.5f)).nor()
        rect(corner000, corner001, corner011, corner010, nor, materialSide)
        rect(corner101, corner100, corner110, corner111, nor.scl(-1f), materialSide)

        return builder.end()
    }

    private fun rect(v0: Vector3, v1: Vector3, v2: Vector3, v3: Vector3, nor: Vector3, material: Material) {
        builder.part("rect", GL20.GL_TRIANGLES, attributes, material).rect(v0, v1, v2, v3, nor)
    }

    private fun createBlockModel(type: Item.Properties) = createBlockModel(type.texture, type.block?.texSide, type.block?.texBottom)

    private val itemCorner000 = Vector3(-0.2f, -0.2f, -0.2f)
    private val itemCorner010 = Vector3(-0.2f, 0.2f, -0.2f)
    private val itemCorner101 = Vector3(0.2f, -0.2f, 0.2f)
    private val itemCorner100 = Vector3(0.2f, -0.2f, -0.2f)
    private val itemCorner001 = Vector3(-0.2f, -0.2f, 0.2f)
    private val itemCorner111 = Vector3(0.2f, 0.2f, 0.2f)

    private fun addItemModelToCache(type: Item.Properties): Model {
        val model: Model

        if (type.isBlock) {
            model = createBlockModel(type)
            model.nodes.first()?.scale?.set(0.3f, 0.3f, 0.3f)
            model.nodes.first()?.translation?.set(-0.15f, 0.0f, -0.15f)
        } else {
            val material = Material(TextureAttribute.createDiffuse(resourceManager.get<Texture>(type.texture)))
            material.set(BlendingAttribute())

            builder.begin()
            val builderPart = builder.part("rect", GL20.GL_TRIANGLES, attributes, material)
            val nor = tmpV1.set(itemCorner000).lerp(itemCorner101, 0.5f).sub(tmpV2.set(itemCorner010).lerp(itemCorner111, 0.5f)).nor()
            builderPart.rect(itemCorner000, itemCorner001, itemCorner101, itemCorner100, nor.scl(-1f))
            model = builder.end()
        }
        itemModels[type.type - 1] = model
        return model
    }
}