package xyz.angm.terra3d.common.world

import kotlinx.serialization.Serializable
import ktx.assets.toLocalFile
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
     * @property name The name of the save/world.
     * @property location The location of the save's directory, relative to [saveLocation].
     * @property seed The world seed */
    @Serializable
    data class Save(val name: String, val location: String, val seed: String)
}