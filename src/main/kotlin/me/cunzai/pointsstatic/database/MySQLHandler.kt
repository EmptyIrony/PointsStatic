package me.cunzai.pointsstatic.database

import net.luckperms.api.LuckPermsProvider
import org.black_ixx.playerpoints.event.PlayerPointsChangeEvent
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.Table
import taboolib.module.database.getHost

object MySQLHandler {

    @Config("config.yml")
    lateinit var config: Configuration

    val server by lazy {
        config.getString("server")
    }

    val host by lazy {
        config.getHost("mysql")
    }

    val datasource by lazy {
        host.createDataSource()
    }

    val table by lazy {
        Table("points_static", host) {
            add { id() }
            add("player_name") {
                type(ColumnTypeSQL.VARCHAR, 48)
            }
            add("cost") {
                type(ColumnTypeSQL.INT)
            }
            add("timestamp") {
                type(ColumnTypeSQL.BIGINT)
            }
            add("server") {
                type(ColumnTypeSQL.VARCHAR, 36)
            }
        }
    }

    val claimedTable by lazy {
        Table("points_cliamed", host) {
            add { id() }
            add("player_name") {
                type(ColumnTypeSQL.VARCHAR, 48)
            }
            add("server") {
                type(ColumnTypeSQL.VARCHAR, 36)
            }
            add("reward_id") {
                type(ColumnTypeSQL.VARCHAR, 48)
            }
            add("claimed") {
                type(ColumnTypeSQL.LONGTEXT)
            }
        }
    }

    @Awake(LifeCycle.ENABLE)
    fun i() {
        table.createTable(datasource, checkExists = true)
        table.createIndex(datasource, "idx_player_name", listOf("player_name"), checkExists = true)
        table.createIndex(datasource, "idx_timestamp", listOf("timestamp"), checkExists = true)

        claimedTable.createTable(datasource, checkExists = true)
        claimedTable.createIndex(datasource, "idx_player_name", listOf("player_name"), checkExists = true)
        claimedTable.createIndex(datasource, "idx_server", listOf("server"), checkExists = true)
        claimedTable.createIndex(datasource, "idx_reward_id", listOf("reward_id"), checkExists = true)
    }

    val claimedCache = HashMap<String, HashMap<String, MutableSet<String>>>()

    @SubscribeEvent
    fun e(e: PlayerJoinEvent) {
        submitAsync {
            val map = HashMap<String, MutableSet<String>>()
            claimedCache[e.player.name] = map
            claimedTable.select(datasource) {
                where {
                    "player_name" eq e.player.name and ("server" eq server)
                }
            }.forEach {
                map[getString("reward_id")] = getString("claimed").split(",").toMutableSet()
            }
        }
    }

    @SubscribeEvent
    fun e(e: PlayerQuitEvent) {
        claimedCache.remove(e.player.name)
    }

    fun Player.save() {
        val map = claimedCache[name] ?: return
        submitAsync {
            map.forEach { (rewardId, claimedList) ->
                claimedTable.select(datasource) {
                    where {
                        "player_name" eq name and ("server" eq server) and ("reward_id" eq rewardId)
                    }
                }.firstOrNull {
                    claimedTable.update(datasource) {
                        set("claimed", claimedList.joinToString(","))
                        where {
                            "player_name" eq name and ("server" eq server) and ("reward_id" eq rewardId)
                        }
                    }
                } ?: claimedTable.insert(datasource, "player_name", "server", "reward_id", "claimed") {
                    value(name, server, rewardId, claimedList.joinToString(","))
                }
            }
        }
    }


    @SubscribeEvent
    fun e(e: PlayerPointsChangeEvent) {
        val change = e.change
        if (change >= 0) return
        submitAsync {
            val playerName = LuckPermsProvider.get().userManager.lookupUsername(e.playerId).get()

            table.insert(datasource, "player_name", "cost", "timestamp", "server") {
                value(playerName, -change, System.currentTimeMillis(), server)
            }
        }
    }

    fun getPointsTotal(playerName: String, server: String? = null, timeRange: LongRange? = null): Int {
        return table.select(datasource) {
            where {
                "player_name" eq playerName
                if (server != null) {
                    "server" eq server
                }
                if (timeRange != null) {
                    "timestamp" between (timeRange.first to timeRange.last)
                }
            }
        }.map {
            getInt("cost")
        }.sum()
    }

}