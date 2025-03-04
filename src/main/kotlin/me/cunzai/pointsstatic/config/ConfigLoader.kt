package me.cunzai.pointsstatic.config


import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common5.FileWatcher
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.getStringListColored
import kotlin.math.max
import kotlin.math.min

object ConfigLoader {

    @Config("rewards.yml")
    lateinit var config: Configuration

    val map = HashMap<String, RewardsConfig>()

    @Awake(LifeCycle.ENABLE)
    fun i() {
        load()

        FileWatcher.INSTANCE.addSimpleListener(config.file) {
            config.reload()
            load()
        }
    }

    private fun load() {
        for (key in config.getKeys(false)) {
            val section = config.getConfigurationSection(key)!!
            val timeRange = section.getString("time")?.let {
                val split = it.split("-")
                if (split.size != 2) {
                    return@let null
                }

                val firstTimestamp = split[0].toLongOrNull() ?: return@let null
                val secondTimestamp = split[1].toLongOrNull() ?: return@let null
                min(firstTimestamp, secondTimestamp) .. max(firstTimestamp, secondTimestamp)
            }

            val rewards = section.getConfigurationSection("rewards")!!
            val rewardList = rewards.getKeys(false).map { rewardNode ->
                val need = rewards.getInt("${rewardNode}.need")
                val rewardCommands = rewards.getStringList("${rewardNode}.rewards")
                val icon = rewards.getItemStack("${rewardNode}.icon")

                RewardData(rewardNode, need, icon!!, rewardCommands,
                    rewards.getStringListColored("${rewardNode}.icon.claimed_lore"),
                    rewards.getStringListColored("${rewardNode}.icon.unreached_lore"),
                )
            }

            map[key] = RewardsConfig(timeRange, rewardList)
        }
    }

    data class RewardsConfig(
        val timeRange: LongRange?,
        val rewards: List<RewardData>,
    )

    data class RewardData(
        val id: String,
        val need: Int,
        val icon: ItemStack,
        val rewards: List<String>,
        val claimedLore: List<String>,
        val unreachedLore: List<String>
    )

}