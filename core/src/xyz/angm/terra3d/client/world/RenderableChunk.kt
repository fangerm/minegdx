package xyz.angm.terra3d.client.world

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.graphics.g3d.model.NodePart
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.world.Chunk

/** A chunk capable of rendering itself. Constructed from a regular chunk sent by the server. */
internal class RenderableChunk(serverChunk: Chunk) : Chunk(fromChunk = serverChunk), Disposable {

    private val positionCentered = position.toV3().add(CHUNK_SIZE / 2f, CHUNK_SIZE / 2f, CHUNK_SIZE / 2f)

    @Transient
    private var model = ModelCache()

    /** If this chunk has a mesh and needs to be rendered.
     * This can still be false even after [RenderableChunk.mesh()]
     * if the chunk is just air. */
    private var hasMesh = false

    /** If this chunk is queued for meshing. */
    internal var isQueued = false

    /** Renders itself. */
    fun render(modelBatch: ModelBatch, environment: Environment) = modelBatch.render(model, environment)

    override fun dispose() = model.dispose()

    /** Called when the chunk is in the rendering queue. Will create the model.
     * This is a greedy meshing implementation. It's abridged from https://eddieabbondanz.io/post/voxel/greedy-mesh/. */
    internal fun mesh(world: World) {
        hasMesh = false
        model.begin()
        Builder.begin()

        // Render each face separately
        for (face in 0 until 6) {
            val isBackFace = face > 2 // If this face is a 'back' face, needing it's position & OGL calls adjusted
            val direction = face % 3 // The axis of the face.
            val workAxis1 = (direction + 1) % 3 // The first other axis
            val workAxis2 = (direction + 2) % 3 // The second other axis

            // For each plane in the given direction...
            startPos[direction] = -1
            while (++startPos[direction] < CHUNK_SIZE) {

                resetMerged() // Reset the faces merged

                startPos[workAxis1] = -1
                while (++startPos[workAxis1] < CHUNK_SIZE) {

                    startPos[workAxis2] = -1
                    while (++startPos[workAxis2] < CHUNK_SIZE) {

                        val block = getFromAIV3(startPos)
                        // Skip this block if it's been merged already, is air, or isn't visible
                        if (isMerged(startPos[workAxis1], startPos[workAxis2]) || block == 0 || !faceVisible(world, startPos, direction, isBackFace))
                            continue

                        quadSize.reset() // Making a new quad, reset

                        // Figure out width & save
                        currPos.set(startPos)
                        while (currPos[workAxis2] < CHUNK_SIZE
                            && canMerge(world, direction, isBackFace)
                            && !isMerged(currPos[workAxis1], currPos[workAxis2])
                        ) currPos[workAxis2]++
                        quadSize[workAxis2] = currPos[workAxis2] - startPos[workAxis2]

                        // Figure out height & save
                        currPos.set(startPos)
                        while (currPos[workAxis1] < CHUNK_SIZE
                            && canMerge(world, direction, isBackFace)
                            && !isMerged(currPos[workAxis1], currPos[workAxis2])
                        ) {
                            currPos[workAxis2] = startPos[workAxis2]
                            while (currPos[workAxis2] < CHUNK_SIZE
                                && canMerge(world, direction, isBackFace)
                                && !isMerged(currPos[workAxis1], currPos[workAxis2])
                            ) currPos[workAxis2]++

                            if (currPos[workAxis2] - startPos[workAxis2] < quadSize[workAxis2]) break
                            else currPos[workAxis2] = startPos[workAxis2]

                            currPos[workAxis1]++
                        }
                        quadSize[workAxis1] = currPos[workAxis1] - startPos[workAxis1]

                        // Finally render the quad
                        tmpAIV.set(startPos)
                        tmpAIV[direction] += if (isBackFace) 0 else 1
                        tmpAIV.apply(corner1)

                        m.reset()[workAxis1] = quadSize[workAxis1]
                        n.reset()[workAxis2] = quadSize[workAxis2]

                        m.apply(corner2).add(corner1) // corner2 = c1 + m
                        m.apply(corner3).add(corner1).add(n.apply(tmpV3)) // corner3 = c1 + m + n
                        n.apply(corner4).add(corner1) // corner4 = c1 + n

                        // Normal is always orthogonal to the quad
                        tmpAIV.reset()[direction] += 1
                        tmpAIV.apply(normal)

                        val tex = Item.Properties.fromType(block)!!.texture
                        val texture = when (face) {
                            1 -> tex // Top face
                            4 -> Item.Properties.fromType(block)!!.block?.texBottom ?: tex // Bottom face
                            else -> Item.Properties.fromType(block)!!.block?.texSide ?: tex // Side face
                        }
                        Builder.drawRect(texture, quadSize[workAxis1].toFloat(), quadSize[workAxis2].toFloat(), isBackFace, direction == 0)
                        hasMesh = true

                        for (f in 0 until quadSize[workAxis1]) {
                            for (g in 0 until quadSize[workAxis2]) {
                                markMerged(startPos[workAxis1] + f, startPos[workAxis2] + g)
                            }
                        }
                    }
                }
            }
        }

        val modelInst = ModelInstance(Builder.end())
        modelInst.transform.setToTranslation(position.toV3(tmpV3))
        model.add(modelInst)
        model.end()
        isQueued = false
    }

    /** Is this face visible and needs to be rendered? */
    private fun faceVisible(world: World, pos: ArrIV3, axis: Int, backFace: Boolean): Boolean {
        tmpAIV.set(pos)[axis] += if (backFace) -1 else 1
        return if (tmpAIV[axis] !in 0 until CHUNK_SIZE)
            ((position.y + tmpAIV[1]) > -1) && !world.blockExists(tmpIV.set(position).add(tmpAIV[0], tmpAIV[1], tmpAIV[2]), true)
        else getFromAIV3(tmpAIV) == 0
    }

    /** Takes a face and returns if the block at currPos can be merged
     * with the current mesh. */
    private fun canMerge(world: World, direction: Int, backFace: Boolean): Boolean {
        val blockA = getFromAIV3(startPos)
        val blockB = getFromAIV3(currPos)
        return blockA == blockB && blockB != 0 && faceVisible(world, currPos, direction, backFace)
    }

    private fun getFromAIV3(pos: ArrIV3) = this[pos[0], pos[1], pos[2]]

    /** Returns if the chunk is meshed and visible to the given camera. */
    fun shouldRender(cam: Camera) = hasMesh && cam.frustum.boundsInFrustum(positionCentered, dimensions)

    override fun hashCode() = position.hashCode()
    override fun equals(other: Any?) = other is RenderableChunk && other.position == position

    private companion object {

        private val dimensions = Vector3(CHUNK_SIZE.toFloat(), CHUNK_SIZE.toFloat(), CHUNK_SIZE.toFloat())

        private const val attributes = VertexAttributes.Usage.Position.toLong() or
                VertexAttributes.Usage.Normal.toLong() or VertexAttributes.Usage.TextureCoordinates.toLong()

        private val corner1 = Vector3()
        private val corner2 = Vector3()
        private val corner3 = Vector3()
        private val corner4 = Vector3()
        private val normal = Vector3()

        private val tmpAIV = ArrIV3()
        private val tmpIV = IntVector3()
        private val tmpV3 = Vector3()
        private val m = ArrIV3()
        private val n = ArrIV3()

        private val startPos = ArrIV3()
        private val currPos = ArrIV3()
        private val quadSize = ArrIV3()

        // Holds the faces on the currently meshing plane that are already meshed.
        // Flattened 2D array.
        private val merged = BooleanArray(CHUNK_SIZE * CHUNK_SIZE)

        private fun resetMerged() = merged.fill(false)
        private fun isMerged(x: Int, y: Int) = merged[(x * CHUNK_SIZE) + y]
        private fun markMerged(x: Int, y: Int) {
            merged[(x * CHUNK_SIZE) + y] = true
        }

        /** An array-backed integer vector used by the greedy meshing algorithm.
         * It's required as the it indexes the axes. */
        private class ArrIV3 {

            val values = IntArray(3)

            operator fun get(i: Int) = values[i]
            operator fun set(i: Int, v: Int) = values.set(i, v)

            fun set(o: ArrIV3): ArrIV3 {
                values[0] = o[0]
                values[1] = o[1]
                values[2] = o[2]
                return this
            }

            /** Applies itself to the given vector. */
            fun apply(v: Vector3): Vector3 {
                v.x = values[0].toFloat()
                v.y = values[1].toFloat()
                v.z = values[2].toFloat()
                return v
            }

            fun reset(): ArrIV3 {
                values[0] = 0
                values[1] = 0
                values[2] = 0
                return this
            }
        }

        object Builder {

            private val regions = ObjectMap<String, TextureRegion>()
            private val builder = MeshBuilder()
            private var model = Model()
            private var node = Node()

            fun begin() {
                model = Model()
                node = Node()
                model.nodes.add(node)
                builder.begin(attributes)
                node.id = "chunkNode"
            }

            fun end(): Model {
                builder.end()
                ModelBuilder.rebuildReferences(model)
                return model
            }

            /** Draw a rectangle, using the positions inside corner1-4. */
            fun drawRect(texture: String, width: Float, height: Float, backFace: Boolean, xFace: Boolean) {
                val region = getRegion(texture)
                // X faces need -90deg rotation, so account for that by inverting w/h
                if (xFace) region.setRegion(0f, 0f, height, width)
                else region.setRegion(0f, 0f, width, height)

                val meshPart = builder.part("c", GL20.GL_TRIANGLES)
                node.parts.add(NodePart(meshPart, Material(TextureAttribute.createDiffuse(region))))

                // Back faces need their corners to be CCW, regular faces CW.
                // This is per OpenGL spec to ensure OGL correctly assumes front faces.
                // This is what the corners are like, assuming a regular 0 at bottom-left coordinate system:
                // 2 3
                // 1 4
                // Regular faces are CW and use order 1234, back faces use 2143 for CCW.
                //
                // Additionally, faces that are orthogonal to the X axis need
                // their points rotated by -90 degrees.
                if (xFace) {
                    if (backFace) builder.rect(corner1, corner4, corner3, corner2, normal)
                    else builder.rect(corner4, corner1, corner2, corner3, normal)
                } else {
                    if (backFace) builder.rect(corner2, corner1, corner4, corner3, normal)
                    else builder.rect(corner1, corner2, corner3, corner4, normal)
                }
            }

            private fun getRegion(texture: String): TextureRegion {
                if (regions[texture] != null) return regions[texture]
                val tex = ResourceManager.get<Texture>(texture)
                tex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
                return TextureRegion(tex)
            }
        }
    }
}
