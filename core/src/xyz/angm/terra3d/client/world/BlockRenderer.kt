/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/27/20, 9:33 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.world

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.IntMap
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.get
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.items.metadata.IMetadata
import xyz.angm.terra3d.common.items.metadata.TranslocatorMetadata
import xyz.angm.terra3d.common.set
import xyz.angm.terra3d.common.world.Block

/** An interface for all block types that need a custom renderer, like translocators.
 * This is heavily entwined with [RenderableChunk] and should be used with it.
 *
 * If you want to add a new block renderer:
 * - Set [Item.Properties.BlockProperties.model] on the block to true
 * - Ensure the block has metadata; only blocks with metadata can be rendered like this
 * - Implement this interface, ideally on an object (see docs on [render] for a guide on how to render)
 * - Add it to the companion object. */
interface BlockRenderer {

    /** Render the block with the given parameters.
     * Set [corners] and [normal] to a quad you want to render, then render the quad
     * with [RenderableChunk.rect] (See it's documentation for more parameters.).
     * @param location The location of the block. Should not be modified, or returned to original state.
     * @param direction The direction the block is facing.
     * @param meta The metadata of the block. */
    fun render(location: IntVector3, direction: Block.Orientation, meta: IMetadata, corners: Array<Vector3>, normal: Vector3)

    companion object {

        private val renderers = IntMap<BlockRenderer>()

        init {
            fun add(ty: String, r: BlockRenderer) = renderers.put(Item.Properties.fromIdentifier(ty).type, r)

            // Add block renderers here
            add("translocator", TranslocatorRender)
        }

        /** Returns the renderer for this block type if any */
        operator fun get(type: ItemType): BlockRenderer? = renderers[type]
    }
}

/** A renderer for translocators, which should render as a small disk on an adjacent block. */
object TranslocatorRender : BlockRenderer {

    private const val WIDTH = 0.5f
    private const val S_OFF = (1f - WIDTH) / 2f
    private const val HEIGHT = 0.125f

    private const val sideTex = "textures/blocks/translocator_side.png"
    private const val topTexPull = "textures/blocks/translocator_top_pull.png"
    private const val topTexPush = "textures/blocks/translocator_top_push.png"
    private const val topTexUnlinked = "textures/blocks/translocator_top_none.png"

    private val tmpV = Vector3()

    override fun render(location: IntVector3, direction: Block.Orientation, meta: IMetadata, corners: Array<Vector3>, normal: Vector3) {
        // Like most rendering code, this is a mess.
        val meta = meta as TranslocatorMetadata
        val face = direction.toId()

        val dirCorrection = if (face > 2) 1f else 0f
        val heightAdj = if (face > 2) -HEIGHT else HEIGHT
        val dir = face % 3
        val other1 = (dir + 1) % 3
        val other2 = (dir + 2) % 3

        location.toV3(tmpV)
        tmpV[dir] += dirCorrection

        tmpV[other1] += S_OFF
        tmpV[other2] += S_OFF
        corners[0].set(tmpV)
        tmpV[dir] += heightAdj
        corners[1].set(tmpV)

        fun renderSide(axis: Int, width: Float, heightAdj: Float, corner: Int, back: Boolean) {
            tmpV[axis] += width
            corners[corner].set(tmpV)
            tmpV[dir] += heightAdj
            corners[corner + 1].set(tmpV)
            RenderableChunk.rect(sideTex, false, WIDTH, HEIGHT, back, dir == 0)
        }

        renderSide(other1, +WIDTH, -heightAdj, 2, face <= 2)
        renderSide(other2, +WIDTH, +heightAdj, 0, face > 2)
        renderSide(other1, -WIDTH, -heightAdj, 2, face <= 2)
        renderSide(other2, -WIDTH, +heightAdj, 0, face > 2)

        fun renderTop(back: Boolean) {
            tmpV[dir] += dirCorrection
            tmpV[other1] += S_OFF
            tmpV[other2] += S_OFF

            corners[0].set(tmpV)
            tmpV[other1] += WIDTH
            corners[1].set(tmpV)
            tmpV[other2] += WIDTH
            corners[2].set(tmpV)
            tmpV[other1] -= WIDTH
            corners[3].set(tmpV)
            RenderableChunk.rect(if (meta.other == null) topTexUnlinked else if (meta.push) topTexPush else topTexPull, false, 1f, 1f, back, dir == 0)
        }

        location.toV3(tmpV)
        tmpV[dir] += heightAdj
        renderTop(face > 2)

        location.toV3(tmpV)
        renderTop(face <= 2)
    }
}
