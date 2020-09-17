package xyz.angm.terra3d.client.world

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.graphics.g3d.model.NodePart
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import ktx.collections.*
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.client.resources.configuration
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.world.ALL
import xyz.angm.terra3d.common.world.Block
import xyz.angm.terra3d.common.world.Chunk
import xyz.angm.terra3d.common.world.TYPE


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
    fun render(modelBatch: ModelBatch, environment: Environment?) = modelBatch.render(model, environment)

    override fun dispose() = model.dispose()

    /** Called when the chunk is in the rendering queue. Will create the model.
     * This is a greedy meshing implementation. It's abridged from https://eddieabbondanz.io/post/voxel/greedy-mesh/. */
    internal fun mesh(world: World) {
        hasMesh = false
        model.begin()
        Builder.begin()

        // Render each face separately
        for (face in 0 until 6) {
            // If this face is a 'back' face, needing it's position & OGL calls adjusted.
            // This is an integer/modifier used when needing to move 1 block forward from the face.
            val backFaceM = if (face > 2) -1 else 1
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

                        val block = getFromAIV3(startPos) and TYPE
                        // Skip this block if it's been merged already, is air, or isn't visible
                        if (isMerged(startPos[workAxis1], startPos[workAxis2]) || block == 0 || !faceVisible(world, startPos, direction, backFaceM))
                            continue

                        setColor(world, direction, backFaceM) // Set the blocks ambient color to be used by the vertex

                        quadSize.reset() // Making a new quad, reset

                        // Figure out width & save
                        currPos.set(startPos)
                        while (currPos[workAxis2] < CHUNK_SIZE
                            && canMerge(world, direction, backFaceM)
                            && !isMerged(currPos[workAxis1], currPos[workAxis2])
                        ) currPos[workAxis2]++
                        quadSize[workAxis2] = currPos[workAxis2] - startPos[workAxis2]

                        // Figure out height & save
                        currPos.set(startPos)
                        while (currPos[workAxis1] < CHUNK_SIZE
                            && canMerge(world, direction, backFaceM)
                            && !isMerged(currPos[workAxis1], currPos[workAxis2])
                        ) {
                            currPos[workAxis2] = startPos[workAxis2]
                            while (currPos[workAxis2] < CHUNK_SIZE
                                && canMerge(world, direction, backFaceM)
                                && !isMerged(currPos[workAxis1], currPos[workAxis2])
                            ) currPos[workAxis2]++

                            if (currPos[workAxis2] - startPos[workAxis2] < quadSize[workAxis2]) break
                            else currPos[workAxis2] = startPos[workAxis2]

                            currPos[workAxis1]++
                        }
                        quadSize[workAxis1] = currPos[workAxis1] - startPos[workAxis1]

                        // Finally render the quad
                        tmpAIV.set(startPos)
                        tmpAIV[direction] += (backFaceM + 1) / 2 // if it's -1, make it 0
                        tmpAIV.apply(corner1)

                        m.reset()[workAxis1] = quadSize[workAxis1]
                        n.reset()[workAxis2] = quadSize[workAxis2]

                        m.apply(corner2).add(corner1) // corner2 = c1 + m
                        m.apply(corner3).add(corner1).add(n.apply(tmpV3)) // corner3 = c1 + m + n
                        n.apply(corner4).add(corner1) // corner4 = c1 + n

                        // Normal is always orthogonal to the quad
                        tmpAIV.reset()[direction] += backFaceM
                        tmpAIV.apply(normal)

                        val props = Item.Properties.fromType(block)!!
                        val tex = props.texture
                        val texture = when {
                            face == 1 -> tex // Top face
                            face == 4 -> props.block?.texBottom ?: tex // Bottom face
                            Block.Orientation.isFront(face, getFromAIV3(startPos)) ->
                                props.block?.texFront ?: props.block?.texSide ?: tex // Front face
                            else -> props.block?.texSide ?: tex // Side face
                        }
                        Builder.drawRect(
                            texture, props.block?.isBlend == true,
                            quadSize[workAxis1].toFloat(), quadSize[workAxis2].toFloat(), backFaceM == -1, direction == 0
                        )
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
    private fun faceVisible(world: World, pos: ArrIV3, axis: Int, backFaceM: Int): Boolean {
        tmpAIV.set(pos)[axis] += backFaceM
        return if (tmpAIV[axis] !in 0 until CHUNK_SIZE)
            ((position.y + tmpAIV[1]) > -1) && world.isBlended(tmpIV.set(position).add(tmpAIV[0], tmpAIV[1], tmpAIV[2]))
        else Item.Properties.fromType(getFromAIV3(tmpAIV) and TYPE)?.block?.isBlend ?: true
    }

    /** Takes a face and returns if the block at currPos can be merged
     * with the current mesh. */
    private fun canMerge(world: World, axis: Int, backFaceM: Int): Boolean {
        val blockA = getFromAIV3(startPos)
        val blockB = getFromAIV3(currPos)
        tmpAIV.set(currPos)[axis] += backFaceM
        return blockA == blockB
                && blockB != 0
                && faceVisible(world, currPos, axis, backFaceM)
                && modAO(world, axis, true)
    }

    private fun getFromAIV3(pos: ArrIV3) = this[pos[0], pos[1], pos[2], ALL]

    private fun setColor(world: World, axis: Int, backFaceM: Int) {
        tmpAIV.set(startPos)[axis] += backFaceM
        val localColor = if (tmpAIV[axis] !in 0 until CHUNK_SIZE)
            world.getLocalLight(tmpIV.set(position).add(tmpAIV[0], tmpAIV[1], tmpAIV[2]))
        else getLocalLight(startPos[0], startPos[1], startPos[2])
        localColor ?: return

        color.r = localColor.x / 15f
        color.g = localColor.y / 15f
        color.b = localColor.z / 15f

        modAO(world, axis, false)
    }

    /** Will set AO to the value for the face at `tmpAIV[axis]+=backFaceM`, see
     * calls for more details on position.
     * If `check == true`, it will instead just check if the current
     * AO values match the ones of the given face.
     * This rather complicated AO implementation is based on these 2 blog posts:
     * https://0fps.net/2013/07/03/ambient-occlusion-for-minecraft-like-worlds/
     * http://mgerhardy.blogspot.com/2016/06/ambient-occlusion-for-polyvox.html */
    private fun modAO(world: World, axis: Int, check: Boolean): Boolean {
        val front = tmpAIV
        val workAxis1 = (axis + 1) % 3 // The first other axis
        val workAxis2 = (axis + 2) % 3 // The second other axis

        // Left: X Negative
        // Right: X Positive
        // Top: Y Positive
        // Bottom: Y Negative

        front[workAxis1]--
        val left = existsAIVOrWorld(world, front)
        front[workAxis2]--
        val leftBottom = existsAIVOrWorld(world, front)
        front[workAxis1]++
        val bottom = existsAIVOrWorld(world, front)
        front[workAxis2] += 2
        val top = existsAIVOrWorld(world, front)
        front[workAxis1]--
        val topLeft = existsAIVOrWorld(world, front)
        front[workAxis1] += 2
        val topRight = existsAIVOrWorld(world, front)
        front[workAxis2]--
        val right = existsAIVOrWorld(world, front)
        front[workAxis2]--
        val rightBottom = existsAIVOrWorld(world, front)
        front[workAxis1]--
        front[workAxis2]++

        fun getAO(sideA: Int, sideB: Int, corner: Int): Int {
            if (sideA == 1 && sideB == 1) return 0
            return 3 - (sideA + sideB + corner)
        }

        tmpAo[0] = getAO(left, bottom, leftBottom)
        tmpAo[1] = getAO(right, bottom, rightBottom)
        tmpAo[2] = getAO(right, top, topRight)
        tmpAo[3] = getAO(left, top, topLeft)

        return if (check) {
            tmpAo.contentEquals(ao)
        } else {
            for (i in 0 until 4) ao[i] = tmpAo[i]
            false // does not matter
        }
    }

    /** Returns 1 if block at given position exists, 0 if not. */
    private fun existsAIVOrWorld(world: World, pos: ArrIV3): Int {
        val block = if (
            pos[0] !in 0 until CHUNK_SIZE ||
            pos[1] !in 0 until CHUNK_SIZE ||
            pos[2] !in 0 until CHUNK_SIZE
        ) {
            tmpAIV2.set(pos)
            pos[0] = (pos[0] + CHUNK_SIZE) % CHUNK_SIZE
            pos[1] = (pos[1] + CHUNK_SIZE) % CHUNK_SIZE
            pos[2] = (pos[2] + CHUNK_SIZE) % CHUNK_SIZE
            val block =
                ((world.getLoadedChunk(tmpIV.set(position).add(tmpAIV2[0], tmpAIV2[1], tmpAIV2[2])) ?: return 0) as RenderableChunk).getFromAIV3(pos)
            pos.set(tmpAIV2)
            block
        } else getFromAIV3(pos)

        return if (block and TYPE == 0) 0 else 1
    }

    /** Returns if the chunk is meshed and visible to the given camera. */
    fun shouldRender(cam: Camera) = hasMesh && cam.frustum.boundsInFrustum(positionCentered, dimensions)

    override fun hashCode() = position.hashCode()
    override fun equals(other: Any?) = other is RenderableChunk && other.position == position

    private companion object {

        private val dimensions = Vector3(CHUNK_SIZE.toFloat(), CHUNK_SIZE.toFloat(), CHUNK_SIZE.toFloat())

        /** The attributes used to render the world.
         * - Position: Simply the position of the vertex; 3 floats
         * - Normal: Normal of the vertex; 3 floats
         * - TexCoord: Texture coordinates of the vertex; 2 floats; textures wrap
         * - Color: Vertex will be tinted with the RGB values of this color
         * which is used for local lighting. The fourth alpha value is (mis)used
         * for storing the vertexes ambient occlusion multiplier and is multiplied
         * with the final fragment color. */
        private const val attributes =
            VertexAttributes.Usage.Position.toLong() or
                    VertexAttributes.Usage.Normal.toLong() or
                    VertexAttributes.Usage.TextureCoordinates.toLong() or
                    VertexAttributes.Usage.ColorPacked.toLong()

        private val aoVals = floatArrayOf(0.15f, 0.6f, 0.8f, 1f)

        private val corner1 = Vector3()
        private val corner2 = Vector3()
        private val corner3 = Vector3()
        private val corner4 = Vector3()
        private val normal = Vector3()
        private val color = Color()
        private var ao = IntArray(4)
        private var tmpAo = IntArray(4)

        private val tmpAIV = ArrIV3()
        private val tmpAIV2 = ArrIV3()
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

            private val builder = MeshBuilder()
            private var model = Model()
            private var node = Node()
            private var materials = ObjectMap<String, Material>(50)

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

            private val vertTmp1 = VertexInfo()
            private val vertTmp2 = VertexInfo()
            private val vertTmp3 = VertexInfo()
            private val vertTmp4 = VertexInfo()

            /** Draw a rectangle, using the positions inside corner1-4. */
            fun drawRect(texture: String, blend: Boolean, width: Float, height: Float, backFace: Boolean, xFace: Boolean) {
                val meshPart = builder.part("c", GL20.GL_TRIANGLES)
                val material = materials[texture] ?: getMaterial(texture, blend)
                node.parts.add(NodePart(meshPart, material))

                val ao1 = aoVals[ao[0]]
                val ao2 = aoVals[ao[1]]
                val ao3 = aoVals[ao[2]]
                val ao4 = aoVals[ao[3]]

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
                    if (backFace) rect(corner1, corner4, corner3, corner2, ao1, ao4, ao3, ao2, height, width)
                    else rect(corner4, corner1, corner2, corner3, ao4, ao1, ao2, ao3, height, width)
                } else {
                    if (backFace) rect(corner2, corner1, corner4, corner3, ao2, ao1, ao4, ao3, width, height)
                    else rect(corner1, corner2, corner3, corner4, ao1, ao2, ao3, ao4, width, height)
                }
            }

            private fun rect(
                corner00: Vector3, corner10: Vector3, corner11: Vector3, corner01: Vector3,
                ao1: Float, ao2: Float, ao3: Float, ao4: Float,
                width: Float, height: Float
            ) {
                builder.rect(
                    vertTmp1.set(corner00, normal, color.set(color.r, color.g, color.b, ao1), null).setUV(0f, height),
                    vertTmp2.set(corner10, normal, color.set(color.r, color.g, color.b, ao2), null).setUV(width, height),
                    vertTmp3.set(corner11, normal, color.set(color.r, color.g, color.b, ao3), null).setUV(width, 0f),
                    vertTmp4.set(corner01, normal, color.set(color.r, color.g, color.b, ao4), null).setUV(0f, 0f)
                )
            }

            private fun getMaterial(texture: String, blend: Boolean): Material {
                val tex = ResourceManager.get<Texture>(texture)
                tex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
                val mat = if (blend && configuration.video.blend) Material(TextureAttribute.createDiffuse(tex), BlendingAttribute())
                else Material(TextureAttribute.createDiffuse(tex))
                materials[texture] = mat
                return mat
            }
        }
    }
}
