package gg.norisk.enchantments

import gg.norisk.enchantments.impl.SquishEnchantment
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object StupidEnchantments : ModInitializer, ClientModInitializer {
    const val MOD_ID = "enchantments"
    val logger: Logger = LogManager.getLogger(MOD_ID)
    fun String.toId() = Identifier.of(MOD_ID, this)

    override fun onInitialize() {
        logger.info("Helloooo")
        EnchantmentRegistry.initialize()
    }

    override fun onInitializeClient() {
        logger.info("Helloooo Client")
        SquishEnchantment.initClient()
    }
}
