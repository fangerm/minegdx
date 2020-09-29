/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 7:00 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.world

import com.badlogic.gdx.utils.Queue
import xyz.angm.terra3d.common.IntVector3

/** A BFS search algorithm used to propagate light through the world.
 * This alg is a adaption and reimplementation of the one used in "Seed of Andromeda",
 * the blog post about it can be found here:
 * https://www.seedofandromeda.com/blogs/29-fast-flood-fill-lighting-in-a-blocky-voxel-game-pt-1
 * This implementation is NOT thread safe. */
class BfsLight(private val world: WorldInterface) {

    private val tmpColor = IntVector3()
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
        while (lightQ.notEmpty()) {
            val node = lightQ.removeFirst()
            val light = tmpColor.set(world.getLocalLight(node) ?: continue)

            node.forAxes {
                node[it]--
                visitBlockLight(node, light)
                node[it] += 2
                visitBlockLight(node, light)
                node[it]--
            }
        }
    }

    private fun visitBlockLight(pos: IntVector3, neighborLevel: IntVector3) {
        val light = world.getLocalLight(pos) ?: return
        var modified = false

        light.forAxes {
            if (light[it] + 2 <= neighborLevel[it]) {
                light[it] = neighborLevel[it] - 1
                modified = true
            }
        }

        if (modified) {
            this.lightQ.addLast(LightNode(pos))
            world.setLocalLight(pos, light)
        }
    }

    private fun emptyRemoveQueue() {
        while (removeQ.notEmpty()) {
            val node = removeQ.removeFirst()
            tmpColor.delinearize(node.color, (RED_LIGHT shr RED_LIGHT_SHIFT) + 1)
            val light = tmpColor

            node.forAxes {
                node[it]--
                visitBlockRemove(node, light)
                node[it] += 2
                visitBlockRemove(node, light)
                node[it]--
            }
        }
        emptyLightQueue()
    }

    private fun visitBlockRemove(pos: IntVector3, neighborLevel: IntVector3) {
        val light = world.getLocalLight(pos) ?: return
        var modified = false

        light.forAxes {
            if (light[it] != 0 && light[it] < neighborLevel[it]) {
                light[it] = 0
                modified = true
            }
        }

        val queueLight = IntRange(0, 2).any { (light[it] >= neighborLevel[it] && neighborLevel[it] != 0) }

        if (modified) {
            removeQ.addLast(RemoveNode(pos, neighborLevel.linearize((RED_LIGHT shr RED_LIGHT_SHIFT) + 1)))
            world.setLocalLight(pos, light)
        }
        if (queueLight) lightQ.addLast(LightNode(pos))
    }
}

private class LightNode(dat: IntVector3) : IntVector3(dat)
private class RemoveNode(dat: IntVector3, val color: Int) : IntVector3(dat)