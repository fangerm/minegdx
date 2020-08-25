package xyz.angm.terra3d.client.resources

import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.ObjectMap
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import ktx.assets.toLocalFile
import ktx.collections.*
import xyz.angm.terra3d.common.yaml
import java.util.*

/** The main object used to retrieve locale/language-specific text to be used by the game.
 * The name is a shorthand of 'internationalization'.
 *
 * Retrieving a string is as simple as:
 * I18N['identifier']
 *
 * To add a new string, take a look at the 'i18n' directory in core/assets.
 * Languages are loaded dynamically! It is possible to add new localizations into
 * the same directory as the game executable and have them be loaded.
 */
object I18N {

    private val bundles = ObjectMap<String, I18NBundle>()
    var bundle: I18NBundle

    init {
        val info = yaml.decodeFromString(
            MapSerializer(String.serializer(), String.serializer()), "i18n/locales.yaml"
                .toLocalFile().readString()
        )
        val fileHandle = "i18n/locale".toLocalFile()
        for (bundleInfo in info) {
            val locale = Locale(bundleInfo.value)
            val bundle = I18NBundle.createBundle(fileHandle, locale)
            bundles[bundleInfo.key] = bundle
        }
        bundle = bundles[configuration.language]
    }

    operator fun get(name: String) = bundle[name]!!

    fun tryGet(name: String): String? {
        return try {
            this[name]
        } catch (e: MissingResourceException) {
            null
        }
    }
}