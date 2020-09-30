/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/30/20, 4:20 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.windows

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import ktx.actors.onClick
import ktx.scene2d.button
import ktx.scene2d.buttonGroup
import ktx.scene2d.scene2d
import ktx.scene2d.textButton
import ktx.scene2d.vis.visLabel
import xyz.angm.terra3d.client.graphics.actors.ItemActor
import xyz.angm.terra3d.client.graphics.actors.ItemGroup
import xyz.angm.terra3d.client.graphics.click
import xyz.angm.terra3d.client.graphics.panels.game.inventory.InventoryPanel
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.recipes.CraftingRecipe

/** Window containing a crafting list / GUI. */
class CraftingWindow(panel: InventoryPanel) : InventoryWindow(panel, "crafting") {

    private val recipes: Table
    private var selected: CraftingRecipe? = null
    private val craftingInput = ItemGroup(this, Inventory(9), row = 1, column = 9)
    private val craftingResult = ItemGroup(this, Inventory(1), row = 1, column = 1, mutable = false)

    init {
        recipes = scene2d.buttonGroup(0, 1) {
            for (recipe in CraftingRecipe.recipes) {
                button("craft") {
                    left()

                    add(ItemActor(recipe.result, this@CraftingWindow)).padRight(10f).padLeft(5f)
                    visLabel(I18N["crafting.needs"], style = "italic-16pt")
                    for (item in recipe.items) add(ItemActor(item, this@CraftingWindow)).pad(5f)

                    onClick {
                        selected = if (selected == recipe) null else recipe
                    }

                    click()
                    it.padBottom(5f).width(400f).row()
                    pack()
                }
            }
        }
        recipes.pack()

        val craftButton = scene2d.textButton(I18N["crafting.craft"]) {
            onClick {
                val item = selected?.consume(craftingInput.inventory, craftingResult.inventory[0])
                craftingResult.inventory += item ?: return@onClick
            }
            click()
        }

        add(ScrollPane(recipes)).height(300f)
        add(craftButton).padBottom(5f).row()
        add(craftingInput).padRight(50f)
        add(craftingResult)
        pack()
        setPosition((WORLD_WIDTH / 4) * 3, WORLD_HEIGHT / 2, Align.center)
    }
}