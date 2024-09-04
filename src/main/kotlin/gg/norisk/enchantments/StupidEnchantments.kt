package gg.norisk.enchantments

import net.fabricmc.api.ModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object StupidEnchantments : ModInitializer {
    const val MOD_ID = "enchantments"
    val logger: Logger = LogManager.getLogger(MOD_ID)

    override fun onInitialize() {
        logger.info("Helloooo")
    }
}
