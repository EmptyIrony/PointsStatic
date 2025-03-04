package me.cunzai.pointsstatic.ui

import me.cunzai.pointsstatic.config.ConfigLoader
import me.cunzai.pointsstatic.database.MySQLHandler
import me.cunzai.pointsstatic.database.MySQLHandler.save
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.command.CommandContext
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.command
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.expansion.submitChain
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import taboolib.platform.compat.replacePlaceholder
import taboolib.platform.util.modifyLore
import taboolib.platform.util.sendLang
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object UIHandler {

    @Awake(LifeCycle.ENABLE)
    fun i() {
        command("recharge-rewards", permissionDefault = PermissionDefault.TRUE) {
            dynamic("node") {
                execute<Player> { sender, context, argument ->
                    open(sender, argument)
                }
            }
        }
    }

    private fun open(player: Player, node: String) {
        val rewardsConfig = ConfigLoader.map[node] ?: return
        submitChain {
            val total = MySQLHandler.getPointsTotal(player.name, MySQLHandler.server, rewardsConfig.timeRange)

            val cacheMap = MySQLHandler.claimedCache[player.name] ?: return@submitChain
            val claimed = cacheMap[node] ?: hashSetOf()

            sync {
                player.openMenu<Chest>("累充奖励 - $node") {
                    rows(kotlin.math.max(1, kotlin.math.round(rewardsConfig.rewards.size / 9.0).toInt()))
                    rewardsConfig.rewards.forEachIndexed { index, reward ->
                        set(index, reward.icon.clone().modifyLore {
                            if (total < reward.need) {
                                clear()
                                addAll(reward.unreachedLore.replacePlaceholder(player))
                            } else {
                                if (claimed.contains(reward.id)) {
                                    clear()
                                    addAll(reward.claimedLore.replacePlaceholder(player))
                                } else {
                                    val newLore = replacePlaceholder(player)
                                    clear()
                                    addAll(newLore)
                                }
                            }
                        }) {
                            isCancelled = true

                            val event = clickEventOrNull() ?: return@set
                            if (event.click == ClickType.DOUBLE_CLICK) return@set
                            if (event.click != ClickType.LEFT) return@set

                            if (total < reward.need) {
                                player.sendLang("not_enough", total, reward.need)
                                return@set
                            }

                            if (!claimed.add(reward.id)) {
                                player.sendLang("claimed")
                                return@set
                            }

                            cacheMap[node] = claimed

                            player.save()

                            for (rewardCommand in reward.rewards) {
                                Bukkit.dispatchCommand(
                                    Bukkit.getConsoleSender(),
                                    rewardCommand.replace("%player%", player.name)
                                )
                            }

                            player.sendLang("claim_success")
                            open(player, node)
                        }
                    }


                }
            }
        }
    }

}