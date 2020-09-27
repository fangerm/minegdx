package xyz.angm.terra3d.client.world

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.IntMap
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
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
     * @param direction The direction the block is facing. */
    fun render(location: IntVector3, direction: Block.Orientation, corners: Array<Vector3>, normal: Vector3)

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
    override fun render(location: IntVector3, direction: Block.Orientation, corners: Array<Vector3>, normal: Vector3) {
        // TODO!!
    }
}
