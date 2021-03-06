/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/27/20, 2:02 AM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.recipes

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import ktx.assets.file
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.metadata.DefaultMeta
import xyz.angm.terra3d.common.yaml

/** A recipe that is crafted in the player's inventory. */
class CraftingRecipe(
    items: HashMap<String, Int>,
    result: String,
    amount: Int = 1,
) {

    val items = items.map { Item(Item.Properties.fromIdentifier(it.key).type, it.value) }
    val result = Item(Item.Properties.fromIdentifier(result).type, amount)

    /** Consume this recipe and return the result.
     * Result is null if the recipe is incomplete.
     * @param current The item currently in the results slot.
     * If it does not stack with the result, null is returned. */
    fun consume(inventory: Inventory, current: Item?): Item? {
        if (current != null && !(result stacksWith current)) return null
        if (items.any { !inventory.contains(it) }) return null
        items.forEach { inventory -= it }
        return result.copy(metadata = DefaultMeta of result.type)
    }

    companion object {

        val recipes = init()

        /** Translates recipes from the serialized format using identifiers to the
         * item types used by the game. */
        private fun init(): Array<CraftingRecipe> {
            val recipesRaw = yaml.decodeFromString(ListSerializer(CraftingRecipeSerialized.serializer()), file("recipes/crafting.yaml").readString())
            return Array(recipesRaw.size) {
                val raw = recipesRaw[it]
                CraftingRecipe(raw.items, raw.result, raw.amount)
            }
        }

        @Serializable
        private class CraftingRecipeSerialized(
            val items: HashMap<String, Int>,
            val result: String,
            val amount: Int = 1
        )
    }
}
