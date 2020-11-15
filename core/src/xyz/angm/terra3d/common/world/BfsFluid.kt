/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/15/20, 10:22 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.world

import com.badlogic.gdx.utils.Queue
import xyz.angm.terra3d.common.IntVector3

/** A BFS search algorithm used to make fluids flow through the world.
 * This alg is abridged from the one in [BfsLight] with a few differences to support
 * stepping and other related features needed for fluids.
 * This implementation is NOT thread safe. */
class BfsFluid(private val world: IWorld) {

    // Fluid queues to empty each tick
    private var fluidQ = Queue<FluidNode>()
    private var removeQ = Queue<FRemoveNode>()

    // Fluid queues to empty the next tick
    private var fluidQNext = Queue<FluidNode>()
    private var removeQNext = Queue<FRemoveNode>()

    /** Advance fluid propagation/flow by one tick. */
    fun tick() {
        // Swap queues to make the ones for next tick
        // the ones to empty now since it is the next tick
        val tmpFQ = fluidQ
        val tmpRQ = removeQ
        fluidQ = fluidQNext
        removeQ = removeQNext
        fluidQNext = tmpFQ
        removeQNext = tmpRQ

        emptyFluidQueue()
        emptyRemoveQueue()
    }

    fun blockSet(block: Block, oldBlock: Block?) {
        // Check if fluid source was removed
        if (oldBlock != null) {
            removeQNext.addLast(FRemoveNode(block.position, oldBlock.fluidLevel))
        }

        // Check if new block is a fluid that needs to flow
        // (the == 0 check prevents this method calling itself via the world)
        if (block.fluidLevel == 0 && block.properties?.block?.fluid == true) {
            block.fluidLevel = block.properties?.block!!.fluidReach
            world.setBlockRaw(block.position, block.toRaw())
            fluidQNext.addLast(FluidNode(block.position))
        }
    }

    private fun emptyFluidQueue() {
        while (fluidQ.notEmpty()) {
            val node = fluidQ.removeFirst() ?: continue // this is null sometimes??
            val block = world.getBlock(node) ?: continue
            val level = block.fluidLevel
            block.fluidLevel--

            node.y--
            val below = world.getBlock(node)
            if ((below?.properties?.block?.fluid != false)) {
                // Block below the node is air or a fluid,
                // only visit the block below
                // Note that this depends on the neighbor's fluid level,
                // so set it to the value we need for a sec
                block.fluidLevel = block.properties!!.block!!.fluidReach
                visitBlock(node, block, block.properties!!.block!!.fluidReach + 1)
                block.fluidLevel = level - 1
            } else {
                node.y++

                // Node is on solid ground, spread in all 4 directions
                node.x--
                visitBlock(node, block, level)
                node.x += 2
                visitBlock(node, block, level)
                node.x--

                node.z--
                visitBlock(node, block, level)
                node.z += 2
                visitBlock(node, block, level)
                node.z--
            }
        }
    }

    private fun visitBlock(pos: IntVector3, neighbor: Block, level: Int) {
        val block = world.getBlock(pos)
        if (block == null && level > 1) {
            fluidQNext.addLast(FluidNode(pos))
            world.setBlockRaw(pos, neighbor.toRaw())
        } else if (block != null && block.fluidLevel + 2 <= level && neighbor.type == block.type) {
            block.fluidLevel = level - 1
            fluidQNext.addLast(FluidNode(pos))
            world.setBlockRaw(pos, block.toRaw())
        }
    }

    private fun emptyRemoveQueue() {
        while (removeQ.notEmpty()) {
            val node = removeQ.removeFirst() ?: continue // this is null sometimes??
            val level = node.level

            node.x--
            visitBlockRemove(node, level)
            node.x += 2
            visitBlockRemove(node, level)
            node.x--

            // Ensure blocks under this one are removed
            node.y--
            removeQNext.addLast(FRemoveNode(node, 15))
            node.y++

            node.z--
            visitBlockRemove(node, level)
            node.z += 2
            visitBlockRemove(node, level)
            node.z--
        }
    }

    private fun visitBlockRemove(pos: IntVector3, neighborLevel: Int) {
        val block = world.getBlock(pos) ?: return
        if ((block.fluidLevel == neighborLevel && neighborLevel != 0) || block.fluidLevel > neighborLevel) {
            fluidQNext.addLast(FluidNode(pos))
        } else if (block.fluidLevel != 0 && block.fluidLevel < neighborLevel) {
            removeQNext.addLast(FRemoveNode(pos, block.fluidLevel))
            world.setBlockRaw(pos, 0)
        }
    }
}

private class FluidNode(dat: IntVector3) : IntVector3(dat)
private class FRemoveNode(dat: IntVector3, val level: Int) : IntVector3(dat)