package xyz.angm.terra3d.common.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.utils.IntMap
import ktx.ashley.get
import xyz.angm.terra3d.common.ecs.EntityData
import xyz.angm.terra3d.common.ecs.ignoreSync
import xyz.angm.terra3d.common.ecs.network

/** A system that keeps track of all entities registered and gives each a unique ID.
 *
 * It also automatically manages all entities sent via network by adding them to the engine automatically;
 * entities that request network update are sent automatically as well. */
class NetworkSystem(
    private val send: (EntityData) -> Unit
) : EntitySystem(Int.MAX_VALUE - 1), EntityListener {

    private val entities = IntMap<Entity>()

    /** Send any entities that require updating. */
    override fun update(deltaTime: Float) {
        entities.values().forEach { entity ->
            if (entity[network]!!.needsSync) {
                entity[network]!!.needsSync = false
                send(EntityData.from(entity))
            }
        }
    }

    /** Either add the new entity or update the local one.
     * Called when entity was received over network. */
    fun receive(data: EntityData) {
        if (!entities.containsKey(data.network.id)) {
            engine.addEntity(data.toEntity())
        } else {
            val localEntity = entities[data.network.id]
            if (localEntity[ignoreSync] != null) return // Things with this flag shouldn't be synced
            data.components.forEach { localEntity.add(it) }
        }
    }

    /** Keep track of all entities. */
    override fun entityAdded(entity: Entity) {
        entities.put(entity[network]!!.id, entity)
    }

    /** Keep track of all entities. */
    override fun entityRemoved(entity: Entity) {
        entities.remove(entity[network]!!.id)
    }
}