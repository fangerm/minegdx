package xyz.angm.terra3d.common.ecs.components

import xyz.angm.rox.Component

/** A component for all entities that must be synced between client and server.
 * Tracks the unique ID of the entity, as well as if it needs sync.
 * @property id The ID of the entity.
 * @property needsSync If the entity needs to be updated via network. */
class NetworkSyncComponent : Component {
    var id = System.nanoTime().toInt()
    var needsSync = true
}