package gg.norisk.enchantments.impl

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import net.silkmc.silk.core.entity.directionVector

object InvertedEnchantment {
    fun PlayerEntity.getStupidKnockbackPos(): Vec3d {
        return eyePos.add(directionVector.normalize().multiply(10.0))
    }
}
