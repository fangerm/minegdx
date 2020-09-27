/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/19/20, 12:54 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.ecs.systems

import com.badlogic.gdx.utils.IntMap
import xyz.angm.rox.Entity
import xyz.angm.rox.EntityListener
import xyz.angm.rox.EntitySystem
import xyz.angm.rox.Family
import xyz.angm.terra3d.common.ecs.components.NetworkSyncComponent
import xyz.angm.terra3d.common.ecs.ignoreSync
import xyz.angm.terra3d.common.ecs.network

/** A system that keeps track of all entities registered and gives each a unique ID.
 *
 * It also automatically manages all entities sent via network by adding them to the engine automatically;
 * entities that request network update are sent automatically as well.
 *
 * REGISTER AS SECOND LAST!. */
class NetworkSystem(private val send: (Entity) -> Unit) : EntitySystem(Int.MAX_VALUE - 1), EntityListener {

    override val family = Family.allOf(NetworkSyncComponent::class)
    private val entities = IntMap<Entity>()

    /** Send any entities that require updating. */
    override fun update(delta: Float) {
        entities.values().forEach { entity ->
            if (entity[network].needsSync) {
                entity[network].needsSync = false
                send(entity)
            }
        }
    }

    /** Either add the new entity or update the local one.
     * Called when entity was received over network. */
    fun receive(netE: Entity) {
        val network = netE.c(network) ?: return
        if (!entities.containsKey(network.id)) {
            engine.add(netE)
        } else {
            val localEntity = entities[network.id]
            if (localEntity has ignoreSync) return // Things with this flag shouldn't be synced
            localEntity.addAll(engine, netE)
            Entity.free(netE)
        }
    }

    /** Keep track of all entities. */
    override fun entityAdded(entity: Entity) {
        entities.put(entity[network].id, entity)
    }

    /** Keep track of all entities. */
    override fun entityRemoved(entity: Entity) {
        entities.remove(entity[network].id)
    }
}