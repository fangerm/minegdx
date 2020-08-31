package xyz.angm.terra3d.client.resources

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import ktx.assets.file
import ktx.assets.toLocalFile
import org.zeroturnaround.zip.ZipUtil
import xyz.angm.terra3d.client.graphics.Skin
import xyz.angm.terra3d.client.world.ModelCache
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.yaml

/** Object for retrieving resources. Uses a [ResourcePack] internally to obtain resources.
 * Should a resource pack not contain a certain resource, the manager falls back to the default pack. */
object ResourceManager {

    private val assets = AssetManager()
    private val defaultPack = yaml.decodeFromString(ResourcePack.serializer(), file("resourcepacks/default/pack.yaml").readString())

    /** A cache for 3D models. **/
    val models = ModelCache(this)

    /** A list of all available resource packs. */
    val availablePacks = ArrayList<ResourcePack>()

    /** The resource pack in use. Should always mirror [Configuration.resourcePack]. */
    var pack: ResourcePack = defaultPack
        set(value) {
            if (field == value) return
            field = value
            assets.clear()
            models.clear()
            loadMenuAssets()
            loadGameAssets()
        }

    /** Called after libGDX application has been created, for initialization. */
    fun init() {
        if (availablePacks.isNotEmpty()) return // Loading already took place

        availablePacks.add(defaultPack)
        // Add all other packs
        "resourcepacks".toLocalFile().list().filter { it.isDirectory && it.nameWithoutExtension() != "default" }.forEach {
            availablePacks.add(yaml.decodeFromString(ResourcePack.serializer(), it.child("pack.yaml").readString()))
        }

        pack = configuration.resourcePack
        loadMenuAssets()
        loadGameAssets()
    }

    /** Returns a resource from the asset manager. */
    fun <T> get(file: String): T = assets.get(
        getFullPath(
            file
        )
    )

    private inline fun <reified T : Any> load(file: String) = assets.load(
        getFullPath(
            file
        ), T::class.java
    )

    /** Turns a local resource path into the full path for the current pack.
     * If the pack does not contain the file, the default packs file path will be returned instead. */
    fun getFullPath(file: String) =
        if (assets.isLoaded("${pack.path}/$file") || pack.containsFile(file)) "${pack.path}/$file"
        else "${defaultPack.path}/$file"

    /** Returns the texture region specified, scaled to fit the current texture pack. */
    fun getTextureRegion(texture: String, x: Int, y: Int, width: Int, height: Int): TextureRegion {
        val sizeMod = if (pack.containsFile(texture)) pack.sizeMod else 1f
        return TextureRegion(
            get<Texture>(texture),
            (x * sizeMod).toInt(), (y * sizeMod).toInt(),
            (width * sizeMod).toInt(), (height * sizeMod).toInt()
        )
    }

    /** Continues loading game assets. Returns loading progress as a float with value range 0-1. 1 means loading is finished. */
    fun continueLoading(): Float {
        assets.update()
        return assets.progress
    }

    private fun loadGameAssets() {
        for (item in Item.Properties.allItems) {
            load<Texture>(item.texture)
            load<Texture>(item.block?.texSide ?: continue)
            load<Texture>(item.block.texBottom ?: continue)
        }
        for (i in 0 until 10) load<Texture>("textures/blocks/destroy_stage_$i.png")
        load<Texture>("textures/gui/icons.png")
        load<Texture>("textures/gui/block_highlighted.png")
        load<Texture>("textures/gui/container/inventory.png")
        load<Texture>("textures/gui/container/crafting_table.png")
        load<Texture>("textures/gui/container/furnace.png")
        load<Texture>("textures/gui/container/generic_54.png")
    }

    private fun loadMenuAssets() {
        load<Texture>("textures/gui/widgets.png")
        load<Texture>("textures/gui/title/terra3d.png")
        load<Model>("models/skybox.obj")
        assets.finishLoading()
        Skin.reload()
    }

    /** Import a minecraft resource pack into MineGDX.
     * @param pack The pack to import. Can be either a folder or a zip file. */
    fun importMinecraftPack(pack: FileHandle) {
        val packDir = if (!pack.isDirectory) unzipFile(pack) else pack
        createResourcePackFromMCPack(packDir)
        if (!pack.isDirectory) packDir.deleteDirectory()
    }

    private fun addNewPack(pack: ResourcePack) {
        availablePacks.add(pack)
        "${pack.path}/pack.yaml".toLocalFile().writeString(yaml.encodeToString(ResourcePack.serializer(), pack), false)
    }

    private fun unzipFile(file: FileHandle): FileHandle {
        ZipUtil.unpack(file.file(), file.nameWithoutExtension().toLocalFile().file())
        return file.nameWithoutExtension().toLocalFile()
    }

    private fun createResourcePackFromMCPack(packDir: FileHandle) {
        "resourcepacks/${packDir.nameWithoutExtension()}".toLocalFile().mkdirs()

        // Why, Mojang, why?!?
        val minecraftResourcePack = Json { isLenient = true }.decodeFromString(
            MapSerializer(String.serializer(), MinecraftResourcePackMetadata.serializer()),
            packDir.child("pack.mcmeta").readString()
        ).getValue("pack")

        val tmpPack = ResourcePack(
            packDir.nameWithoutExtension(),
            "resourcepacks/${packDir.nameWithoutExtension()}",
            minecraftResourcePack.description,
            1f
        )

        copyAssetsFromMinecraftPack(packDir, tmpPack)
        Gdx.app.postRunnable {
            addNewPack(autodetectMCPackScale(tmpPack))
        }
    }

    private fun copyAssetsFromMinecraftPack(packDir: FileHandle, pack: ResourcePack) {
        packDir.child("assets/minecraft/textures").copyTo(pack.path.toLocalFile())
    }

    private fun autodetectMCPackScale(pack: ResourcePack): ResourcePack {
        val textureFile = "${pack.path}/textures/items".toLocalFile().list().first()
        return pack.copy(sizeMod = Texture(textureFile).width.toFloat() / 32f)
    }
}