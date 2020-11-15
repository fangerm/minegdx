/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 10:15 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.world

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.model.MeshPart
import com.badlogic.gdx.graphics.g3d.model.NodePart
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Pool
import ktx.collections.*
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.client.resources.configuration
import xyz.angm.terra3d.common.CHUNK_SIZE
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.world.*
import xyz.angm.terra3d.server.ecs.systems.PhysicsSystem
import kotlin.math.abs


/** A chunk capable of rendering itself. Constructed from a regular chunk sent by the server.
 * Do be warned that this rendering code is written to be as optimized as possible.
 * In other words, it's unreadable and you should not bother. */
internal class RenderableChunk(serverChunk: Chunk) : Chunk(fromChunk = serverChunk), Disposable {

    private val positionCentered = position.toV3().add(CHUNK_SIZE / 2f, CHUNK_SIZE / 2f, CHUNK_SIZE / 2f)

    @Transient
    private lateinit var renderable: ChunkRenderable

    /** If this chunk has a mesh and needs to be rendered.
     * This can still be false even after [RenderableChunk.mesh()]
     * if the chunk is just air. */
    private var hasMesh = false

    /** If this chunk is queued for meshing. */
    internal var isQueued = false

    /** Renders itself. */
    fun render(modelBatch: ModelBatch, environment: Environment?) = modelBatch.render(renderable, environment)

    override fun dispose() {
        if (hasMesh) {
            for (part in renderable.nodeParts) {
                meshPartPool.free(part.meshPart)
                nodePartPool.free(part)
            }
            renderable.nodeParts[0].meshPart.mesh.dispose()
        }
    }

    /** Called when the chunk is in the rendering queue. Will create the model.
     * This is a greedy meshing implementation. It's abridged from https://eddieabbondanz.io/post/voxel/greedy-mesh/. */
    internal fun mesh(world: World) {
        hasMesh = false

        // Render each face separately
        for (face in 0 until 6) {
            meshFace(world, face)
        }

        // Reset AO first, custom renderers usually do not have AO
        for (i in 0 until 4) ao[i] = 3
        // Render all blocks with custom renderers
        // (Custom renderers require metadata so this is fine)
        for (entry in blockMetadata) {
            val pos = entry.key
            val all = this[pos.x, pos.y, pos.z, ALL]
            val ty = all and TYPE
            val orient = (all and ORIENTATION) shr ORIENTATION_SHIFT

            val props = Item.Properties.fromType(ty)
            if (props?.block?.model == false) continue
            val renderer = BlockRenderer[ty] ?: continue
            renderer.render(pos, Block.Orientation.fromId(orient), entry.value, corners, normal)
        }

        if (hasMesh) {
            renderable = Builder.end()
            position.toV3(renderable.position)
        }
        isQueued = false
    }

    private fun meshFace(world: World, face: Int) {
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

                    val all = getFromAIV3(startPos)
                    val block = all and TYPE
                    val fluidLevel = (all and FLUID_LEVEL) shr FLUID_LEVEL_SHIFT

                    // Skip this block if it's been merged already, is air, or isn't visible
                    if (isMerged(startPos[workAxis1], startPos[workAxis2])
                        || block == 0
                        || !faceVisible(world, startPos, direction, backFaceM, fluidLevel)
                    )
                        continue

                    val props = Item.Properties.fromType(block)!!
                    val blockP = props.block!!
                    if (blockP.model) continue

                    setColor(world, direction, backFaceM) // Set the blocks ambient color to be used by the vertex

                    quadSize.reset() // Making a new quad, reset

                    // Figure out width & save
                    currPos.set(startPos)
                    while (currPos[workAxis2] < CHUNK_SIZE
                        && !isMerged(currPos[workAxis1], currPos[workAxis2])
                        && canMerge(world, direction, backFaceM, fluidLevel)
                    ) currPos[workAxis2]++
                    quadSize[workAxis2] = currPos[workAxis2] - startPos[workAxis2]

                    // Figure out height & save
                    currPos.set(startPos)
                    while (currPos[workAxis1] < CHUNK_SIZE
                        && !isMerged(currPos[workAxis1], currPos[workAxis2])
                        && canMerge(world, direction, backFaceM, fluidLevel)
                    ) {
                        currPos[workAxis2] = startPos[workAxis2]
                        while (currPos[workAxis2] < CHUNK_SIZE
                            && !isMerged(currPos[workAxis1], currPos[workAxis2])
                            && canMerge(world, direction, backFaceM, fluidLevel)
                        ) currPos[workAxis2]++

                        if (currPos[workAxis2] - startPos[workAxis2] < quadSize[workAxis2]) break
                        else currPos[workAxis2] = startPos[workAxis2]

                        currPos[workAxis1]++
                    }
                    quadSize[workAxis1] = currPos[workAxis1] - startPos[workAxis1]

                    // Finally render the quad
                    tmpAIV.set(startPos)
                    tmpAIV[direction] += (backFaceM + 1) / 2 // if it's -1, make it 0
                    tmpAIV.apply(corners[0])

                    m.reset()[workAxis1] = quadSize[workAxis1]
                    n.reset()[workAxis2] = quadSize[workAxis2]

                    m.apply(corners[1]).add(corners[0]) // corner2 = c1 + m
                    m.apply(corners[2]).add(corners[0]).add(n.apply(tmpV3)) // corner3 = c1 + m + n
                    n.apply(corners[3]).add(corners[0]) // corner4 = c1 + n

                    // Normal is always orthogonal to the quad
                    tmpAIV.reset()[direction] += backFaceM
                    tmpAIV.apply(normal)

                    if (blockP.fluid) {
                        val level = (getFromAIV3(startPos) and FLUID_LEVEL) shr FLUID_LEVEL_SHIFT
                        tmpAIV.set(startPos)[direction] += backFaceM
                        if (face == 1) adjustTopFluidLevel(level, blockP.fluidReach)
                        else if (face % 3 != 1) adjustSideFluidLevel(world, block, level, blockP.fluidReach, direction)
                    } else if (blockP.collider != PhysicsSystem.BlockCollider.FULL) {
                        val isX = direction == 0
                        when (blockP.collider) {
                            PhysicsSystem.BlockCollider.HALF_LOWER -> {
                                if (face == 1) corners.forEach { it.y -= 0.5f }
                                else if (direction != 1) {
                                    corners[if (isX) 1 else 3].y -= 0.5f
                                    corners[2].y -= 0.5f
                                }
                            }
                            PhysicsSystem.BlockCollider.HALF_UPPER -> {
                                if (face == 4) corners.forEach { it.y += 0.5f }
                                else if (direction != 1) {
                                    corners[if (isX) 3 else 1].y += 0.5f
                                    corners[0].y += 0.5f
                                }
                            }
                        }
                    }

                    val tex = props.texture
                    val texture = when {
                        face == 1 -> tex // Top face
                        face == 4 -> blockP.texBottom ?: tex // Bottom face
                        Block.Orientation.isFront(face, getFromAIV3(startPos)) ->
                            blockP.texFront ?: blockP.texSide ?: tex // Front face
                        else -> blockP.texSide ?: tex // Side face
                    }

                    rect(
                        texture,
                        props.block.isBlend,
                        quadSize[workAxis1].toFloat(),
                        quadSize[workAxis2].toFloat(),
                        backFaceM == -1,
                        direction == 0 && !blockP.fluid // Rotating on fluids messes with the height adjust
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

    /** Is this face visible and needs to be rendered? */
    private fun faceVisible(world: World, pos: ArrIV3, axis: Int, backFaceM: Int, fluidLevel: Int): Boolean {
        tmpAIV.set(pos)[axis] += backFaceM
        if ((position.y + tmpAIV[1]) <= -1) return false

        val raw = if (tmpAIV[axis] !in 0 until CHUNK_SIZE)
            world.getBlockRaw(tmpIV.set(position).add(tmpAIV[0], tmpAIV[1], tmpAIV[2]))
        else getFromAIV3(tmpAIV)
        val props = Item.Properties.fromType(raw and TYPE) ?: return true

        return if (props.block!!.fluid && fluidLevel != 0) {
            ((raw and FLUID_LEVEL) shr FLUID_LEVEL_SHIFT) != fluidLevel
        } else props.block.isBlend
    }

    /** Takes a face and returns if the block at currPos can be merged
     * with the current mesh. */
    private fun canMerge(world: World, axis: Int, backFaceM: Int, fluidLevel: Int): Boolean {
        val blockA = getFromAIV3(startPos)
        val blockB = getFromAIV3(currPos)
        tmpAIV.set(currPos)[axis] += backFaceM
        return blockA == blockB
                && blockB != 0
                && faceVisible(world, currPos, axis, backFaceM, fluidLevel)
                && checkAO(world, axis)
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

        setAO(world, axis)
    }

    /** Will set AO to the value for the face at `tmpAIV[axis]+=backFaceM`, see
     * calls for more details on position.
     * If `check == true`, it will instead just check if the current
     * AO values match the ones of the given face.
     * This rather complicated AO implementation is based on these 2 blog posts:
     * https://0fps.net/2013/07/03/ambient-occlusion-for-minecraft-like-worlds/
     * http://mgerhardy.blogspot.com/2016/06/ambient-occlusion-for-polyvox.html */
    private fun setAO(world: World, axis: Int) {
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

        ao[0] = getAO(left, bottom, leftBottom)
        ao[1] = getAO(right, bottom, rightBottom)
        ao[2] = getAO(right, top, topRight)
        ao[3] = getAO(left, top, topLeft)
    }

    private fun checkAO(world: World, axis: Int): Boolean {
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

        if (getAO(left, bottom, leftBottom) != ao[0]) {
            front[workAxis2]--
            return false
        }

        val top = existsAIVOrWorld(world, front)
        front[workAxis1]--
        val topLeft = existsAIVOrWorld(world, front)
        front[workAxis1] += 2

        if (getAO(left, top, topLeft) != ao[3]) {
            front[workAxis1]--
            front[workAxis2]--
            return false
        }

        val topRight = existsAIVOrWorld(world, front)
        front[workAxis2]--
        val right = existsAIVOrWorld(world, front)
        front[workAxis2]--

        if (getAO(right, top, topRight) != ao[2]) {
            front[workAxis1]--
            front[workAxis2]++
            return false
        }

        val rightBottom = existsAIVOrWorld(world, front)
        front[workAxis1]--
        front[workAxis2]++

        return getAO(right, bottom, rightBottom) == ao[1]
    }

    private fun getAO(sideA: Int, sideB: Int, corner: Int): Int {
        if (sideA == 1 && sideB == 1) return 0
        return 3 - (sideA + sideB + corner)
    }

    /** Returns 1 if block at given position exists, 0 if not.
     * Used to determine ambient occlusion. */
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

        // Fluids should not trigger AO
        return if (block and TYPE == 0 || block and FLUID_LEVEL != 0) 0 else 1
    }

    // Adjust the fluid quad down to fit level of fluid
    private fun adjustTopFluidLevel(level: Int, fluidReach: Int) {
        val height = 1f - (level.toFloat() / fluidReach)
        corners.forEach { it.y -= height }
    }

    // Adjust the fluid quad down to fit level of fluid
    private fun adjustSideFluidLevel(world: World, type: ItemType, level: Int, fluidReach: Int, direction: Int) {
        val isX = direction == 0
        val topHeightSub = 1f - (level.toFloat() / fluidReach)
        corners[if (isX) 1 else 3].y -= topHeightSub
        corners[2].y -= topHeightSub

        val front = getAIV3OrWorld(world, direction)
        val frontLevel = ((front and FLUID_LEVEL) shr FLUID_LEVEL_SHIFT)
        if (front and TYPE == type && frontLevel != 0) {
            val levelDiff = abs(frontLevel - level + 1)
            val botHeightAdd = (0.9f * (levelDiff.toFloat() / fluidReach))
            corners[if (isX) 3 else 1].y += botHeightAdd
            corners[0].y += botHeightAdd
        }
    }

    private fun getAIV3OrWorld(world: World, direction: Int): Int {
        if ((position.y + tmpAIV[1]) <= -1) return 0
        return if (tmpAIV[direction] !in 0 until CHUNK_SIZE)
            world.getBlockRaw(tmpIV.set(position).add(tmpAIV[0], tmpAIV[1], tmpAIV[2]))
        else getFromAIV3(tmpAIV)
    }

    /** Returns if the chunk is meshed and visible to the given camera. */
    fun shouldRender(cam: Camera) = hasMesh && cam.frustum.boundsInFrustum(positionCentered, dimensions)

    override fun hashCode() = position.hashCode()
    override fun equals(other: Any?) = other is RenderableChunk && other.position == position

    companion object {

        private val dimensions = Vector3(CHUNK_SIZE.toFloat(), CHUNK_SIZE.toFloat(), CHUNK_SIZE.toFloat())

        /** The attributes used to render chunks.
         * - Position: Simply the position of the vertex; 3 floats
         * - Normal: Normal of the vertex; 3 floats
         * - TexCoord: Texture coordinates of the vertex; 2 floats; textures wrap
         * - Color: Vertex will be tinted with the RGB values of this color
         * which is used for local lighting. The fourth alpha value is (mis)used
         * for storing the vertexes ambient occlusion multiplier and is multiplied
         * with the final fragment color. */
        private const val attributes =
            VertexAttributes.Usage.Position.toLong() or
                    VertexAttributes.Usage.ColorPacked.toLong() or
                    VertexAttributes.Usage.Normal.toLong() or
                    VertexAttributes.Usage.TextureCoordinates.toLong()

        private val aoVals = floatArrayOf(0.15f, 0.6f, 0.8f, 1f)

        private val corners = Array(4) { Vector3() }
        private val normal = Vector3()
        private val color = Color()
        private val ao = IntArray(4) { 3 }

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
        class ArrIV3 {

            val values = IntArray(3)

            operator fun get(i: Int) = values[i]
            operator fun set(i: Int, v: Int) = values.set(i, v)

            fun set(o: ArrIV3): ArrIV3 {
                values[0] = o[0]
                values[1] = o[1]
                values[2] = o[2]
                return this
            }

            fun set(o: IntVector3): ArrIV3 {
                values[0] = o.x
                values[1] = o.y
                values[2] = o.z
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

        /** A builder for chunk meshes.
         * Defers actually building the mesh to end(). */
        private object Builder {

            private val builder = MeshBuilder()
            private val parts = OrderedMap<String, Part>(50)
            private lateinit var vbo: Part
            private var partsUsed = 0

            fun end(): ChunkRenderable {
                val nodes = GdxArray<NodePart>(partsUsed)

                builder.begin(attributes)
                for (part in parts) {
                    val vbo = part.value
                    if (vbo.size == 0) continue

                    val meshPart = builder.part("c", GL20.GL_TRIANGLES, meshPartPool.obtain())
                    val nodePart = nodePartPool.obtain()
                    nodePart.material = vbo.material
                    nodePart.meshPart = meshPart
                    nodes.add(nodePart)

                    for (i in 0 until vbo.size) {
                        val uvOff = (i * 2)
                        val normOff = (i * 3)
                        val colOff = (i * 7)
                        color.set(vbo.colors[colOff], vbo.colors[colOff + 1], vbo.colors[colOff + 2], 0f)
                        for (v in 0 until 4) {
                            val posOff = (i * 3 * 4) + v * 3
                            tmpV3.set(vbo.positions[posOff], vbo.positions[posOff + 1], vbo.positions[posOff + 2])
                            normal.set(vbo.normals[normOff], vbo.normals[normOff + 1], vbo.normals[normOff + 2])
                            color.a = vbo.colors[colOff + 3 + v]
                            vertTmp[v].set(tmpV3, normal, color, null)
                        }
                        vertTmp[0].setUV(0f, vbo.texUVs[uvOff + 1])
                        vertTmp[1].setUV(vbo.texUVs[uvOff], vbo.texUVs[uvOff + 1])
                        vertTmp[2].setUV(vbo.texUVs[uvOff], 0f)
                        vertTmp[3].setUV(0f, 0f)

                        builder.rect(
                            vertTmp[0],
                            vertTmp[1],
                            vertTmp[2],
                            vertTmp[3],
                        )
                    }

                    vbo.positions.clear()
                    vbo.normals.clear()
                    vbo.colors.clear()
                    vbo.texUVs.clear()
                    vbo.size = 0
                }

                partsUsed = 0
                builder.end()
                return ChunkRenderable(nodes)
            }

            private val vertTmp = Array(4) { MeshPartBuilder.VertexInfo() }

            /** @see [RenderableChunk.rect] */
            fun drawRect(texture: String, blend: Boolean, width: Float, height: Float, backFace: Boolean, xFace: Boolean) {
                var part = parts[texture]
                if (part == null) {
                    part = Part(getMaterial(texture, blend))
                    parts[texture] = part
                }
                vbo = part
                if (vbo.size == 0) partsUsed++

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
                    if (backFace) rect(0, 3, 2, 1, height, width)
                    else rect(3, 0, 1, 2, height, width)
                } else {
                    if (backFace) rect(1, 0, 3, 2, width, height)
                    else rect(0, 1, 2, 3, width, height)
                }
            }

            private fun rect(
                c1: Int, c2: Int, c3: Int, c4: Int,
                width: Float, height: Float
            ) {
                vertex(corners[c1])
                vertex(corners[c2])
                vertex(corners[c3])
                vertex(corners[c4])
                vbo.normals.add(normal.x)
                vbo.normals.add(normal.y)
                vbo.normals.add(normal.z)
                vbo.colors.add(color.r)
                vbo.colors.add(color.g)
                vbo.colors.add(color.b)
                vbo.colors.add(aoVals[ao[c1]])
                vbo.colors.add(aoVals[ao[c2]])
                vbo.colors.add(aoVals[ao[c3]])
                vbo.colors.add(aoVals[ao[c4]])
                vbo.texUVs.add(width)
                vbo.texUVs.add(height)
                vbo.size++
            }

            private fun vertex(pos: Vector3) {
                vbo.positions.add(pos.x)
                vbo.positions.add(pos.y)
                vbo.positions.add(pos.z)
            }

            private fun getMaterial(texture: String, blend: Boolean): Material {
                val tex = ResourceManager.get<Texture>(texture)
                tex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
                return if (blend && configuration.video.blend) Material(TextureAttribute.createDiffuse(tex), BlendingAttribute())
                else Material(TextureAttribute.createDiffuse(tex))
            }

            private class Part(val material: Material) {
                val positions = GdxFloatArray(4 * 3 * 5)
                val normals = GdxFloatArray(3 * 5)
                val colors = GdxFloatArray(7 * 5)
                val texUVs = GdxFloatArray(2 * 5)
                var size = 0
            }
        }

        /** Draw a quad. Should ideally always be orthogonal to one of the
         * three coordinate axes.
         * [corners], [normal], [ao] and [color] should be set
         * to the appropriate values to draw this quad with.
         * @param texture The texture to be used for rendering
         * @param blend If this quad should have blending enabled
         * @param width The width of the quad on the first axis
         * @param height The width of the quad on the second axis
         * @param backFace If this quad should be flipped, making it visible on the other side
         * @param xFace If this quad is orthogonal to the X axis, requiring an adjustment to alignment */
        fun rect(texture: String, blend: Boolean, width: Float, height: Float, backFace: Boolean, xFace: Boolean) =
            Builder.drawRect(texture, blend, width, height, backFace, xFace)

        private val meshPartPool: Pool<MeshPart> = object : Pool<MeshPart>() {
            override fun newObject() = MeshPart()
        }
        private val nodePartPool: Pool<NodePart> = object : Pool<NodePart>() {
            override fun newObject() = NodePart()
        }

        private class ChunkRenderable(val nodeParts: GdxArray<NodePart>) : RenderableProvider {

            val position = Vector3()

            override fun getRenderables(renderables: GdxArray<Renderable>, pool: Pool<Renderable>) {
                for (part in nodeParts) {
                    val renderable = pool.obtain()
                    part.setRenderable(renderable)
                    renderable.worldTransform.setToTranslation(position)
                    renderables.add(renderable)
                }
            }
        }
    }
}