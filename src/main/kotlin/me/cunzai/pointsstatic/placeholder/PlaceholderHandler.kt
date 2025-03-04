package me.cunzai.pointsstatic.placeholder

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import me.cunzai.pointsstatic.config.ConfigLoader
import me.cunzai.pointsstatic.database.MySQLHandler
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import taboolib.platform.compat.PlaceholderExpansion
import java.util.concurrent.TimeUnit

object PlaceholderHandler: PlaceholderExpansion {

    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.SECONDS)
        .build(object :CacheLoader<String, Int>(){
            override fun load(key: String): Int {
                val split = key.split("_", limit = 3)
                val server = split[0]
                val range = split[1]
                val playerName = split[2]

                val rewardsConfig = ConfigLoader.map[range] ?: return 0

                return MySQLHandler.getPointsTotal(playerName, server, rewardsConfig.timeRange)
            }
        })

    override val identifier: String
        get() = "pointsstatic"

    override val enabled: Boolean
        get() = true
    override val autoReload: Boolean
        get() = false

    override fun onPlaceholderRequest(player: Player?, args: String): String {
        return onPlaceholderRequest(player as? OfflinePlayer?, args)
    }

    override fun onPlaceholderRequest(player: OfflinePlayer?, args: String): String {
        val input = args + "_" + (player?.name ?: return "0")
        return cache.get(input).toString()
    }


}