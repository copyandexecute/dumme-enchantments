package gg.norisk.enchantments

import gg.norisk.enchantments.command.EnchantmentsCommand
import gg.norisk.enchantments.command.SatisfyingCommand
import gg.norisk.enchantments.impl.*
import gg.norisk.enchantments.impl.antiknockback.AntiKnockbackEnchantment
import gg.norisk.enchantments.impl.boomerang.BoomerangEnchantment
import gg.norisk.enchantments.impl.fork.ForkEnchantment
import gg.norisk.enchantments.impl.freeze.FreezeEnchantment
import gg.norisk.enchantments.impl.presslufthammer.BlockBreaker
import gg.norisk.enchantments.impl.presslufthammer.PressLuftHammer
import gg.norisk.enchantments.impl.schleuder.SchleuderEnchantment
import gg.norisk.enchantments.impl.stolper.StolperEnchantment
import gg.norisk.enchantments.impl.whiper.WhiperEnchantment
import gg.norisk.enchantments.mixin.client.GameRendererAccessor
import gg.norisk.enchantments.sound.SoundRegistry
import gg.norisk.enchantments.utils.CameraShaker
import gg.norisk.satisfying.*
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import net.silkmc.silk.commands.clientCommand
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object StupidEnchantments : ModInitializer, ClientModInitializer {
    const val MOD_ID = "enchantments"
    val logger: Logger = LogManager.getLogger(MOD_ID)
    fun String.toId() = Identifier.of(MOD_ID, this)

    override fun onInitialize() {
        logger.info("Helloooo")
        EnchantmentRegistry.initialize()
        SoundRegistry.init()
        //DopaminEnchantment.initServer()
        //ColossalEnchantment.initServer()
        //HotEnchantment.initServer()
        HelicopterEnchantment.initServer()
        BoomerangEnchantment.initServer()
        StolperEnchantment.initServer()
        AntiKnockbackEnchantment.initServer()
        ForkEnchantment.initServer()
        HelicopterEnchantmentV2.initServer()
        FreezeEnchantment.initServer()
        SchleuderEnchantment.initServer()
        //TrashEnchantment.initServer()
        //EnchantmentsCommand.initServer()
        //SatisfyingCommand.initServer()
        //MedusaEnchantment.initServer()
        //BalloonEnchantment.initServer()
        AimBotEnchantment.initServer()
        WhiperEnchantment.initServer()
        CameraShaker.initServer()
        PressLuftHammer.initServer()
        BlockBreaker.initServer()
        //RamEnchantment.initServer()
        //SatisfyingExperience.initServer()
        //SatisfyingChainReaction.initServer()
        //SatisfyingSuperStar.initServer()
        //SatisfyingBlockAnimation.initServer()
        //SatisfyingTrail.initServer()

        if (FabricLoader.getInstance().environmentType == EnvType.SERVER || FabricLoader.getInstance().isDevelopmentEnvironment) {
            SlipperyEnchantment.initServer()
        }
    }

    override fun onInitializeClient() {
        logger.info("Helloooo Client")
        //SatisfyingSuperStar.initClient()
        //SquishEnchantment.initClient()
        //RollEnchantment.initClient()
        //TrashEnchantment.initClient()
        //DopaminEnchantment.initClient()
        GlitchEnchantment.initClient()
        CameraShaker.initClient()
        PressLuftHammer.initClient()
        SlipperyEnchantment.initClient()
        VerificationEnchantment.initClient()
        HelicopterEnchantment.initClient()
        HelicopterEnchantmentV2.initClient()
        FreezeEnchantment.initClient()
        WhiperEnchantment.initClient()
        //MemeEnchantment.initClient()
        //SatisfyingTrail.initClient()
        //SatisfyingExperience.initClient()
        //SatisfyingCrush.initClient()
        //SatisfyingBlockAnimation.initClient()
    }
}
