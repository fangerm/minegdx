package xyz.angm.terra3d.common.recipes

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import ktx.assets.file
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.world.NOTHING
import xyz.angm.terra3d.common.yaml

/** A recipe that is crafted in the player's inventory or a crafting table. */
class CraftingRecipe(
    items: Array<String?>,
    result: String,
    private val amount: Int = 1,
    private val isShaped: Boolean
) {

    private val items = items.map { Item.Properties.fromIdentifier(it ?: return@map 0).type }
    private val result = Item.Properties.fromIdentifier(result).type

    private fun matches(inventory: Inventory, slotsUsed: Int): Boolean {
        return if (isShaped) matchShaped(inventory)
        else matchShapeless(inventory, slotsUsed)
    }

    private fun getReturnedItem() = Item(result, amount)

    private fun matchShapeless(inventory: Inventory, slotsUsed: Int): Boolean {
        if (items.size != slotsUsed) return false
        items.forEach { if (!inventory.contains(it, 1)) return false }
        return true
    }

    private fun matchShaped(inventory: Inventory): Boolean {
        val is2x2 = items.size <= 4
        val gridIs2x2 = inventory.size <= 4

        return if (!is2x2 && gridIs2x2) false
        else if (is2x2 == gridIs2x2) matchSameSize(inventory)
        else matchBiggerSize(inventory)
    }

    private fun matchSameSize(inventory: Inventory): Boolean {
        for (i in items.indices) {
            if (items[i] != (inventory[i]?.type ?: NOTHING)) return false
        }
        return true
    }

    private fun matchBiggerSize(inventory: Inventory): Boolean {
        // This code checks 2x2 recipes in a 3x3 grid. A 3x3 grid's indices look like this:
        // 0 1 2
        // 3 4 5
        // 6 7 8
        // To check for a 2x2, all points in the below array need to be checked.
        val offsets = intArrayOf(0, 1, 3, 4)
        val offsetsFix = intArrayOf(0, 1, 2, 2, 3) // Magic constants, just ignore, it works...
        for (startPoint in offsets) {
            var match = true
            for (i in offsets) {
                match = match && items[offsetsFix[i]] == inventory[startPoint + i]?.type
            }
            if (match) return true
        }
        return false
    }

    companion object {

        private val recipes = getRecipes()

        /** Matches all known crafting recipes.
         * @param inventory The inventory of the crafting interface to check against. */
        fun matchAll(inventory: Inventory): Item? {
            val slotsUsed = inventory.occupiedSize() // Get it once to prevent all recipes from iterating the inv needlessly
            val matchedRecipe = recipes.firstOrNull { it.matches(inventory, slotsUsed) }
            return matchedRecipe?.getReturnedItem()
        }

        /** Translates recipes from the serialized format using identifiers to the
         * item types used by the game. */
        private fun getRecipes(): Array<CraftingRecipe> {
            val recipesRaw = yaml.decodeFromString(ListSerializer(CraftingRecipeSerialized.serializer()), file("recipes/crafting.yaml").readString())
            return Array(recipesRaw.size) {
                val raw = recipesRaw[it]
                CraftingRecipe(raw.items, raw.result, raw.amount, raw.isShaped)
            }
        }

        @Serializable
        private class CraftingRecipeSerialized(
            val items: Array<String?>,
            val result: String,
            val amount: Int = 1,
            val isShaped: Boolean
        )
    }
}
