package xyz.angm.terra3d.client.resources

import kotlinx.serialization.Serializable
import ktx.assets.toLocalFile

/** A resource pack to be used in the game.
 * Contains both sounds and textures; if a texture or sound is missing, it will be loaded from the default texture pack.
 *
 * @property name The name displayed to the user.
 * @property description The description of the pack displayed to the user.
 * @property sizeMod The size multiplier for the pack. 1.0 equals a 32x32 texture pack.
 * @property path The path of the pack. */
@Serializable
data class ResourcePack(
    val name: String,
    val path: String,
    val description: String,
    val sizeMod: Float
) {
    /** If the texture pack contains the file/asset specified. */
    fun containsFile(file: String) = "$path/$file".toLocalFile().exists()
}

/** Minecraft's pack.mcmeta. Why did Mojang use such an annoying format...
 * @property description The description of the pack. */
@Serializable
data class MinecraftResourcePackMetadata(val description: String)
