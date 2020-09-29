/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 9:20 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.recipes

import com.badlogic.gdx.utils.IntMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import ktx.assets.file
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.ItemType
import xyz.angm.terra3d.common.yaml

/** A simple recipe with one input and one output. */
class OneToOneRecipe(
    val input: Int,
    val output: Int,
    val inAmount: Int,
    val outAmount: Int
)

/** A class for generic 1-to-1 recipes like furnaces.
 * Implement this class; put the recipes in `recipes` in assets. */
abstract class OneToOneRecipes(name: String) {

    val recipes: IntMap<OneToOneRecipe>

    init {
        val recipesRaw = yaml.decodeFromString(ListSerializer(Serialized.serializer()), file("recipes/$name.yaml").readString())
        recipes = IntMap(recipesRaw.size)
        recipesRaw.forEach {
            val input = Item.Properties.fromIdentifier(it.input).type
            val output = Item.Properties.fromIdentifier(it.output).type
            recipes.put(input, OneToOneRecipe(input, output, it.inAmount, it.outAmount))
        }
    }

    /** Returns recipe for input type, if any */
    operator fun get(input: ItemType): OneToOneRecipe? = recipes[input]

    @Serializable
    class Serialized(
        val input: String,
        val output: String,
        val inAmount: Int = 1,
        val outAmount: Int = 1
    )
}

/** Recipes for furnaces. */
object FurnaceRecipes : OneToOneRecipes("furnace")