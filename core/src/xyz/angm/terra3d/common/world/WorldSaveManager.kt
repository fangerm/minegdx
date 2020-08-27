package xyz.angm.terra3d.common.world

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import kotlinx.serialization.Serializable
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.assets.toLocalFile
import xyz.angm.terra3d.common.ecs.components.RemoveFlag
import xyz.angm.terra3d.common.ecs.components.specific.PlayerComponent
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.fst
import xyz.angm.terra3d.common.networking.JoinPacket
import xyz.angm.terra3d.common.yaml

/** Used for managing world saves. */
object WorldSaveManager {

    private const val saveLocation = "worlds"

    /** Returns all worlds available as an ArrayList<[Save]> */
    fun getWorlds(): ArrayList<Save> {
        val list = ArrayList<Save>()
        saveLocation.toLocalFile().list().filter { it.isDirectory }.forEach {
            list += loadSave(it.path())
        }
        return list
    }

    /** Add a new world. */
    fun addWorld(name: String, seed: String = System.currentTimeMillis().toString()): Save {
        if ("$saveLocation/$name".toLocalFile().exists()) return loadSave("$saveLocation/$name")
        val save = Save(name, "$saveLocation/$name", seed)
        "$saveLocation/$name".toLocalFile().mkdirs()
        "${save.location}/metadata.yaml".toLocalFile().writeString(yaml.encodeToString(Save.serializer(), save), false)
        return save
    }

    /** Delete a world. */
    fun deleteWorld(location: String) = location.toLocalFile().deleteDirectory()

    /** Returns a Save object. */
    private fun loadSave(path: String) = yaml.decodeFromString(Save.serializer(), "$path/metadata.yaml".toLocalFile().readString())

    /** A world save's metadata.
     * TODO: Move logic out of this
     * @property name The name of the save/world.
     * @property location The location of the save's directory, relative to [saveLocation].
     * @property seed The world seed */
    @Serializable
    data class Save(val name: String, val location: String, val seed: String) {

        /** Returns a player from the save and adds them to the engine. If no such player exists, a new one is created.
         * @param info The packet containing the required info to create the player.  */
        fun getPlayer(engine: Engine, info: JoinPacket): Entity {
            val player = "$location/players/${info.uuid}.bin".toLocalFile()
            val entity = if (player.exists()) {
                val player = fst.asObject(player.readBytes()) as Entity
                engine.addEntity(player)
                player
            } else PlayerComponent.create(engine, name, info.uuid)

            entity.remove(RemoveFlag::class.java) // Unsure why but this is here?
            return entity
        }

        /** Saves player to disk.
         * @param player The player to save */
        fun savePlayer(player: Entity) {
            "$location/players".toLocalFile().mkdirs()
            "$location/players/${player[playerM]!!.clientUUID}.bin".toLocalFile().writeBytes(fst.asByteArray(player), false)
        }

        /** Will save all entities to disk, including players.
         * @param engine The engine to get the entities from. */
        fun saveAllEntities(engine: Engine) {
            val playerFamily = allOf(PlayerComponent::class).get()
            val otherFamily = exclude(PlayerComponent::class).get()

            engine.getEntitiesFor(playerFamily).forEach { savePlayer(it) }

            val entities = engine.getEntitiesFor(otherFamily).toArray<Entity>(Entity::class.java)
            val rawData = fst.asByteArray(entities)
            "$location/entities.bin".toLocalFile().writeBytes(rawData, false)
        }

        /** Gets all entities from the save.
         * @param engine The engine to register the entities to. */
        fun getAllEntities(engine: Engine) {
            val file = "$location/entities.bin".toLocalFile()
            if (!file.exists()) return // No entities to restore
            val entities = fst.asObject(file.readBytes()) as Array<Entity>
            entities.forEach { engine.addEntity(it) }
        }
    }
}