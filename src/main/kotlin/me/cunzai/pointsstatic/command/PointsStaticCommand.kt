package me.cunzai.pointsstatic.command

import me.cunzai.pointsstatic.database.MySQLHandler
import me.cunzai.pointsstatic.database.MySQLHandler.datasource
import me.cunzai.pointsstatic.database.MySQLHandler.server
import org.bukkit.command.CommandSender
import taboolib.common.platform.command.*
import taboolib.common.platform.function.submitAsync
import taboolib.expansion.createHelper

@CommandHeader("pointsStatic")
object PointsStaticCommand {

    @CommandBody
    val addRecord = subCommand {
        dynamic("player") {
            dynamic("points") {
                execute<CommandSender> { sender, context, argument ->
                    val points = argument.toIntOrNull() ?: return@execute
                    val playerName = context["player"]
                    submitAsync {
                        MySQLHandler.table.insert(datasource, "player_name", "cost", "timestamp", "server") {
                            value(playerName, points, System.currentTimeMillis(), server)
                        }
                        sender.sendMessage("ok")
                    }
                }
            }
        }
    }

    @CommandBody
    val check = subCommand {
        dynamic("player") {
            dynamic("server") {
                execute<CommandSender> { sender, context, argument ->
                    val playerName = context["player"]
                    submitAsync {
                        sender.sendMessage(MySQLHandler.getPointsTotal(playerName, argument).toString())
                    }
                }
            }
        }
    }

    @CommandBody
    val main = mainCommand {
        createHelper(checkPermissions = true)
    }

}