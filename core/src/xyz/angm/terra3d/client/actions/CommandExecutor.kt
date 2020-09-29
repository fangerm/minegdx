/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 7:27 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.actions

import com.badlogic.gdx.math.MathUtils
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.string
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.*
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.items.metadata.DefaultMeta

/** Responsible for parsing commands entered by the user.
 * These commands are entered by typing a chat message with a $ as prefix. */
object CommandExecutor {

    private val dispatcher = CommandDispatcher<GameScreen>()
    private var returnMessage = ""

    init {
        dispatcher.register(
            literal<GameScreen>("give")
                .then(argument<GameScreen, String>("item", string()).executes { give(it) }
                    .then(argument<GameScreen, Int>("amount", integer()).executes { give(it) }))
                .executes {
                    returnMessage = "[ORANGE]Syntax: give [item] <amount=1>"
                    -1
                }
        )

        dispatcher.register(
            literal<GameScreen>("tp")
                .then(
                    argument<GameScreen, Int>("x", integer())
                        .then(
                            argument<GameScreen, Int>("y", integer())
                                .then(argument<GameScreen, Int>("z", integer()).executes {
                                    val pos = IntVector3(getInteger(it, "x"), getInteger(it, "y"), getInteger(it, "z"))
                                    it.source.player[localPlayer].teleport(pos.toV3())
                                    returnMessage = "[GREEN]Teleported to $pos"
                                    1
                                })
                        )
                )
                .executes {
                    returnMessage = "[ORANGE]Syntax: tp [x] [y] [z]"
                    -1
                }
        )

        dispatcher.register(
            literal<GameScreen>("kill")
                .executes {
                    it.source.player[health].health = 0
                    returnMessage = "[ORANGE]You died."
                    1
                }
        )

        fun CommandContext<GameScreen>.setTime(time: Float): Int {
            val dtE = source.engine.entities.find { it has dayTime }!!
            dtE[dayTime].time = time
            dtE[network].needsSync = true
            returnMessage = "[GREEN]Set time."
            return 1
        }

        dispatcher.register(literal<GameScreen>("day").executes { it.setTime(0.5f) })
        dispatcher.register(literal<GameScreen>("night").executes { it.setTime(MathUtils.PI + 0.5f) })

        dispatcher.register(literal<GameScreen>("tool").executes {
            execute("give ultratool", it.source)
            1
        })
    }

    /** Executes the command given by the user. The leading $ should be cut. */
    fun execute(command: String, screen: GameScreen): String {
        return try {
            dispatcher.execute(command.trim(' '), screen)
            returnMessage
        } catch (e: CommandSyntaxException) {
            "[RED]Invalid command."
        }
    }

    /** Returns arg from context, or null if the arg does not exist */
    private inline fun <reified T : Any> getArgOrNull(context: CommandContext<GameScreen>, arg: String): T? {
        return try {
            context.getArgument(arg, T::class.java)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** Commands */

    private fun give(context: CommandContext<GameScreen>): Int {
        val itemIdent = getString(context, "item").toLowerCase()
        val itemAmount = getArgOrNull(context, "amount") ?: 1

        val itemProps = Item.Properties.tryFromIdentifier(itemIdent)
        if (itemProps != null) {
            context.source.player[playerM].inventory += Item(itemProps.type, itemAmount, DefaultMeta of itemProps.type)
            returnMessage = "[GREEN]Given ${itemAmount}x ${itemProps.name}"
        } else returnMessage = "[RED]Not a known item type."
        return 1
    }
}