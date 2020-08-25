package xyz.angm.terra3d.client.world

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.graphics.g3d.model.NodePart
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.world.Chunk

/** A chunk capable of rendering itself. Constructed from a regular chunk sent by the server. */
class RenderableChunk(serverChunk: Chunk) : Chunk(fromChunk = serverChunk), Disposable {

    private val dimensions = Vector3(CHUNK_SIZE.toFloat(), CHUNK_SIZE.toFloat(), CHUNK_SIZE.toFloat())
    private val positionCentered = position.toV3().add(CHUNK_SIZE / 2f, CHUNK_SIZE / 2f, CHUNK_SIZE / 2f)
    @Transient
    private var model = ModelInstance(Model())

    /** Renders itself. */
    fun render(modelBatch: ModelBatch, environment: Environment) = modelBatch.render(model, environment)

    override fun dispose() = model.model.dispose()

    /** Called when the chunk is in the rendering queue. Will create the model.
     * TODO: Optimize meshing alg */
    fun mesh() {
        model.model.dispose()
        Builder.begin()

        for (x in 0..CHUNK_SIZE)
            for (y in 0..CHUNK_SIZE)
                for (z in 0..CHUNK_SIZE) {
                    if (!isBlockVisible(x, y, z)) continue
                    val block = getBlock(tmpIV.set(x, y, z)) ?: continue
                    setCurrentBlockPosition(x, y, z)

                    val tex = block.properties!!.texture
                    val texSide = block.properties!!.block?.texSide ?: tex
                    val texBottom = block.properties!!.block?.texBottom ?: tex

                    var meshBuilder = Builder.getMeshBuilder(tex)

                    var nor = tmpV1.set(corner000).lerp(corner101, 0.5f).sub(tmpV2.set(corner010).lerp(corner111, 0.5f)).nor()
                    if (y == CHUNK_SIZE - 1 || !blockExists(x, y + 1, z)) {
                        meshBuilder.rect(corner010, corner011, corner111, corner110, nor.scl(-1f))
                    }

                    if (y == 0 || !blockExists(x, y - 1, z)) {
                        meshBuilder = Builder.getMeshBuilder(texBottom)
                        meshBuilder.rect(corner001, corner000, corner100, corner101, nor)
                    }

                    meshBuilder = Builder.getMeshBuilder(texSide)
                    nor = tmpV1.set(corner000).lerp(corner110, 0.5f).sub(tmpV2.set(corner001).lerp(corner111, 0.5f)).nor()
                    if (z == 0 || !blockExists(x, y, z - 1)) meshBuilder.rect(corner100, corner000, corner010, corner110, nor)
                    if (z == CHUNK_SIZE - 1 || !blockExists(x, y, z + 1)) meshBuilder.rect(corner001, corner101, corner111, corner011, nor.scl(-1f))

                    nor = tmpV1.set(corner000).lerp(corner011, 0.5f).sub(tmpV2.set(corner100).lerp(corner111, 0.5f)).nor()
                    if (x == 0 || !blockExists(x - 1, y, z)) meshBuilder.rect(corner000, corner001, corner011, corner010, nor)
                    if (x == CHUNK_SIZE - 1 || !blockExists(x + 1, y, z)) meshBuilder.rect(corner101, corner100, corner110, corner111, nor.scl(-1f))
                }

        model = ModelInstance(Builder.end())
        model.transform.setToTranslation(position.toV3(tmpV3))
    }

    private fun isBlockVisible(x: Int, y: Int, z: Int): Boolean {
        return !IntVector3.isInBounds(x, y, z, 1, CHUNK_SIZE - 1) ||
                !(blockExists(x - 1, y, z) &&
                        blockExists(x + 1, y, z) &&
                        blockExists(x, y - 1, z) &&
                        blockExists(x, y + 1, z) &&
                        blockExists(x, y, z - 1) &&
                        blockExists(x, y, z + 1))
    }

    /** Returns if the chunk is visible to the given camera. */
    fun isVisible(cam: Camera) = cam.frustum.boundsInFrustum(positionCentered, dimensions)

    private companion object {

        private const val attributes = VertexAttributes.Usage.Position.toLong() or
                VertexAttributes.Usage.Normal.toLong() or VertexAttributes.Usage.TextureCoordinates.toLong()

        private val corner000 = Vector3(0f, 0f, 0f)
        private val corner010 = Vector3(0f, 1f, 0f)
        private val corner100 = Vector3(1f, 0f, 0f)
        private val corner110 = Vector3(1f, 1f, 0f)
        private val corner001 = Vector3(0f, 0f, 1f)
        private val corner011 = Vector3(0f, 1f, 1f)
        private val corner101 = Vector3(1f, 0f, 1f)
        private val corner111 = Vector3(1f, 1f, 1f)
        private val tmpIV = IntVector3()
        private val tmpV1 = Vector3()
        private val tmpV2 = Vector3()
        private val tmpV3 = Vector3()

        private fun setCurrentBlockPosition(x: Int, y: Int, z: Int) {
            corner000.set(x + 0f, y + 0f, z + 0f)
            corner010.set(x + 0f, y + 1f, z + 0f)
            corner100.set(x + 1f, y + 0f, z + 0f)
            corner110.set(x + 1f, y + 1f, z + 0f)
            corner001.set(x + 0f, y + 0f, z + 1f)
            corner011.set(x + 0f, y + 1f, z + 1f)
            corner101.set(x + 1f, y + 0f, z + 1f)
            corner111.set(x + 1f, y + 1f, z + 1f)
        }

        object Builder {

            private val materials = ObjectMap<String, Material>()
            private val meshBuilders = ObjectMap<String, MeshBuilder>()
            private var model = Model()
            private var node = Node()

            fun begin() {
                model = Model()
                node = Node()
                model.nodes.add(node)
                node.id = "chunkNode"
            }

            fun end(): Model {
                meshBuilders.values().forEach { it.end() }
                meshBuilders.clear()
                ModelBuilder.rebuildReferences(model)
                return model
            }

            fun getMeshBuilder(texture: String): MeshPartBuilder {
                val stored = meshBuilders[texture]
                if (stored != null) return stored

                val builder = MeshBuilder()
                builder.begin(attributes)

                val meshPart = builder.part("c", GL20.GL_TRIANGLES)
                node.parts.add(NodePart(meshPart, getMaterial(texture)))

                meshBuilders.put(texture, builder)

                return builder
            }

            private fun getMaterial(texture: String): Material {
                return materials[texture] ?: {
                    val material = Material(TextureAttribute.createDiffuse(ResourceManager.get<Texture>(texture)))
                    materials.put(texture, material)
                    material
                }()
            }
        }
    }
}
