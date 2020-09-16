package xyz.angm.terra3d.server.world

import com.badlogic.gdx.utils.Queue
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.world.Block

/** A BFS search algorithm used to make fluids flow through the world.
 * This alg is abridged from the one in [BfsLight] with a few differences to support
 * stepping and other related features needed for fluids.
 * This implementation is NOT thread safe. */
class BfsFluid(private val world: World) {

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
        if ((oldBlock?.fluidLevel ?: 0) != 0) {
            removeQNext.addLast(FRemoveNode(block.position, oldBlock!!.fluidLevel))
            block.fluidLevel = 0
            world.setBlock(block.position, block)
        }

        // Check if new block is a fluid that needs to flow
        // (the == 0 check prevents this method calling itself via the world)
        if (block.fluidLevel == 0 && block.properties?.block?.fluid == true) {
            block.fluidLevel = block.properties?.block!!.fluidReach
            world.setBlock(block.position, block)
            fluidQNext.addLast(FluidNode(block.position))
        }
    }

    private fun emptyFluidQueue() {
        while (fluidQ.notEmpty()) {
            val node = fluidQ.removeFirst() ?: continue // this is null sometimes??
            val block = world.getBlock(node) ?: continue
            val level = block.fluidLevel - 1

            node.y--
            val below = world.getBlock(node)
            if ((below?.properties?.block?.fluid != false)) {
                // Block below the node is air or a fluid,
                // only visit the block below
                visitBlock(node, block, block.properties!!.block!!.fluidReach)
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
        if (block == null) {
            fluidQNext.addLast(FluidNode(pos))
            world.setBlock(pos, neighbor.copy(position = pos, fluidLevel = level))
        } else if (block.fluidLevel + 2 <= level && neighbor.type == block.type) {
            block.fluidLevel = level - 1
            fluidQNext.addLast(FluidNode(pos))
            world.setBlock(pos, block)
        }
    }

    private fun emptyRemoveQueue() {
        while (removeQ.notEmpty()) {
            val node = removeQ.removeFirst() ?: continue // this is null sometimes??
            val block = world.getBlock(node) ?: continue
            val level = block.fluidLevel

            node.x--
            visitBlockRemove(node, level)
            node.x += 2
            visitBlockRemove(node, level)
            node.x--

            // todo what is correct here
            node.y--
            visitBlockRemove(node, level)
            node.y += 2
            visitBlockRemove(node, level)
            node.y--

            node.z--
            visitBlockRemove(node, level)
            node.z += 2
            visitBlockRemove(node, level)
            node.z--
        }
    }

    private fun visitBlockRemove(pos: IntVector3, neighborLevel: Int) {
        val block = world.getBlock(pos) ?: return
        if (block.fluidLevel != 0 && block.fluidLevel < neighborLevel) {
            block.fluidLevel = 0
            removeQNext.addLast(FRemoveNode(pos, neighborLevel))
            world.setBlock(pos, block)

            if (block.fluidLevel >= neighborLevel && neighborLevel != 0)
                fluidQNext.addLast(FluidNode(pos))
        }
    }
}

private class FluidNode(dat: IntVector3) : IntVector3(dat)
private class FRemoveNode(dat: IntVector3, val level: Int) : IntVector3(dat)