package gg.norisk.enchantments

import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier

object EnchantmentRegistry {
    val fastFalling: RegistryKey<Enchantment> = of("fast_falling")

    private fun of(name: String): RegistryKey<Enchantment> {
        return RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MOD_ID, name))
    }

    fun initialize() {
    }
}
