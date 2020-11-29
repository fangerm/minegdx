/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 11/29/20, 5:56 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.windows

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisTextField
import ktx.actors.onClick
import ktx.actors.onKey
import ktx.scene2d.*
import ktx.scene2d.vis.visLabel
import xyz.angm.terra3d.client.graphics.actors.ItemActor
import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.click
import xyz.angm.terra3d.client.graphics.panels.game.inventory.InventoryPanel
import xyz.angm.terra3d.client.graphics.screens.worldHeight
import xyz.angm.terra3d.client.graphics.screens.worldWidth
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.recipes.CraftingRecipe

/** Window containing a crafting list / GUI. */
class CraftingWindow(panel: InventoryPanel) : InventoryWindow(panel, "crafting") {

    private val recipes: Table
    private val recipeActors: List<KButton>
    private val search = VisTextField()
    private var selected: CraftingRecipe? = null
    private val craftingInput = ItemGroup(this, Inventory(9), row = 1, column = 9)
    private val craftingResult = ItemGroup(this, Inventory(1), row = 1, column = 1, mutable = false)

    init {
        recipes = scene2d.buttonGroup(0, 1)
        recipeActors = CraftingRecipe.recipes.map { recipe ->
            scene2d.button("craft") {
                left()

                add(ItemActor(recipe.result, this@CraftingWindow)).padRight(10f).padLeft(5f)
                visLabel(I18N["crafting.needs"], style = "italic-16pt")
                for (item in recipe.items) add(ItemActor(item, this@CraftingWindow)).pad(5f)

                onClick {
                    selected = if (selected == recipe) null else recipe
                }

                click()
                pack()
            }
        }
        reload()
        recipes.pack()

        val craftButton = scene2d.textButton(I18N["crafting.craft"]) {
            onClick {
                val item = selected?.consume(craftingInput.inventory, craftingResult.inventory[0])
                craftingResult.inventory += item ?: return@onClick
            }
            click()
        }

        search.onKey { reload() }

        add(Label(I18N["crafting.search"], skin, "default-16pt"))
        add(search).width(300f).padBottom(5f).row()
        add(scene2d.table {
            add(ScrollPane(recipes)).height(300f)
            add(craftButton).padBottom(5f).row()
            add(craftingInput).padRight(50f)
            add(craftingResult)
            pack()
        }).colspan(2)
        pack()
        setPosition((worldWidth / 4) * 3, worldHeight / 2, Align.center)
    }

    private fun reload() {
        recipes.clearChildren()
        val filter = search.text
        for (i in 0 until recipeActors.size) {
            val properties = CraftingRecipe.recipes[i].result.properties
            if (properties.ident.contains(filter) || properties.name.contains(filter))
                recipes.add(recipeActors[i]).padBottom(5f).width(400f).row()
        }
    }
}