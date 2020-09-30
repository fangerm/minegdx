/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/30/20, 5:09 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.common.quest

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import ktx.assets.file
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.common.items.Inventory
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.yaml

/** A quest which requires the player to obtain an item to complete it.
 * See PlayerComponent in ECS for usage, as well as the various menu windows
 * on the client.
 *
 * @param ident The identifier used for I18N name/descriptions (`quest.name.ident quest.desc.ident`)
 * @param itemReq The identifier of the item needed
 * @param amount The amount of the item required
 *
 * @property name The localized name of the quest
 * @property description The localized description of the quest */
@Serializable
data class Quest(
    private val ident: String,
    private val itemReq: String,
    private val amount: Int
) : java.io.Serializable {

    val item get() = Item(Item.Properties.fromIdentifier(itemReq).type, amount)
    val name get() = I18N["quest.name.$ident"]
    val description get() = I18N["quest.desc.$ident"]

    fun checkComplete(inventory: Inventory) = inventory.contains(item)
}

object Quests {

    val quests = yaml.decodeFromString(ListSerializer(Quest.serializer()), file("quests.yaml").readString())

    fun next(quest: Quest) = quests[quests.indexOf(quest) + 1]
}