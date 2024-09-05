package gg.norisk.enchantments.sound

import gg.norisk.enchantments.StupidEnchantments.toId
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.SoundEvent

object SoundRegistry {
    var GOOFY_AAH_BEAT =
        Registry.register(Registries.SOUND_EVENT, "goofy_ah_beat".toId(), SoundEvent.of("goofy_ah_beat".toId()))
    var GLITCH =
        Registry.register(Registries.SOUND_EVENT, "glitch".toId(), SoundEvent.of("glitch".toId()))
    var BOUNCY =
        Registry.register(Registries.SOUND_EVENT, "bouncy".toId(), SoundEvent.of("bouncy".toId()))

    fun init() {
    }

}
