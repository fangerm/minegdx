package xyz.angm.terra3d.common.world

import com.badlogic.gdx.utils.Queue
import xyz.angm.terra3d.common.IntVector3

/** A BFS search algorithm used to propagate light through the world.
 * This alg is a adaption and reimplementation of the one used in "Seed of Andromeda",
 * the blog post about it can be found here:
 * https://www.seedofandromeda.com/blogs/29-fast-flood-fill-lighting-in-a-blocky-voxel-game-pt-1
 * This implementation is NOT thread safe. */
class BfsLight(private val world: WorldInterface) {

    private val tmpIVLocal = ThreadLocal.withInitial { IntVector3() }
    private val tmpColor get() = tmpIVLocal.get()
    private val lightQ = Queue<LightNode>()
    private val removeQ = Queue<RemoveNode>()

    fun blockSet(block: Block, oldBlock: Block?) {
        // Check if light source was removed
        if (oldBlock?.properties?.block?.emitsLight == true) {
            val light = world.getLocalLight(block.position) ?: return
            removeQ.addLast(RemoveNode(block.position, light.linearize((RED_LIGHT shr RED_LIGHT_SHIFT) + 1)))
            world.setLocalLight(block.position, IntVector3.ZERO)
            emptyRemoveQueue()
        }

        // Check if new block adds light
        if (block.properties?.block?.emitsLight == true) {
            val props = block.properties?.block!!
            world.setLocalLight(block.position, IntVector3(props.redLight, props.greenLight, props.blueLight))
            lightQ.addLast(LightNode(block.position))
            emptyLightQueue()
        }
    }

    private fun emptyLightQueue() {
        while (!lightQ.isEmpty) {
            val node = lightQ.removeFirst()
            val light = tmpColor.set(world.getLocalLight(node) ?: return)

            node.x--
            visitBlockLight(node, light)
            node.x += 2
            visitBlockLight(node, light)
            node.x--

            node.y--
            visitBlockLight(node, light)
            node.y += 2
            visitBlockLight(node, light)
            node.y--

            node.z--
            visitBlockLight(node, light)
            node.z += 2
            visitBlockLight(node, light)
            node.z--
        }
    }

    private fun visitBlockLight(pos: IntVector3, neighborLevel: IntVector3) {
        val light = world.getLocalLight(pos) ?: return
        var modified = false

        if (light.x + 2 <= neighborLevel.x) {
            light.x = neighborLevel.x - 1
            modified = true
        }
        if (light.y + 2 <= neighborLevel.y) {
            light.y = neighborLevel.y - 1
            modified = true
        }
        if (light.z + 2 <= neighborLevel.z) {
            light.z = neighborLevel.z - 1
            modified = true
        }

        if (modified) {
            this.lightQ.addLast(LightNode(pos))
            world.setLocalLight(pos, light)
        }
    }

    private fun emptyRemoveQueue() {
        while (!removeQ.isEmpty) {
            val node = removeQ.removeFirst()
            tmpColor.delinearize(node.color, (RED_LIGHT shr RED_LIGHT_SHIFT) + 1)
            val light = tmpColor

            node.x--
            visitBlockRemove(node, light)
            node.x += 2
            visitBlockRemove(node, light)
            node.x--

            node.y--
            visitBlockRemove(node, light)
            node.y += 2
            visitBlockRemove(node, light)
            node.y--

            node.z--
            visitBlockRemove(node, light)
            node.z += 2
            visitBlockRemove(node, light)
            node.z--
        }
        emptyLightQueue()
    }

    private fun visitBlockRemove(pos: IntVector3, neighborLevel: IntVector3) {
        val light = world.getLocalLight(pos) ?: return
        var modified = false

        if (light.x != 0 && light.x < neighborLevel.x) {
            light.x = 0
            modified = true
        }
        if (light.y != 0 && light.y < neighborLevel.y) {
            light.y = 0
            modified = true
        }
        if (light.z != 0 && light.z < neighborLevel.z) {
            light.z = 0
            modified = true
        }
        val queueLight = (light.x >= neighborLevel.x && neighborLevel.x != 0)
                || (light.y >= neighborLevel.y && neighborLevel.y != 0)
                || (light.z >= neighborLevel.z && neighborLevel.z != 0)

        if (modified) {
            removeQ.addLast(RemoveNode(pos, neighborLevel.linearize((RED_LIGHT shr RED_LIGHT_SHIFT) + 1)))
            world.setLocalLight(pos, light)
        }
        if (queueLight) lightQ.addLast(LightNode(pos))
    }
}

private class LightNode(dat: IntVector3) : IntVector3(dat)
private class RemoveNode(dat: IntVector3, val color: Int) : IntVector3(dat)