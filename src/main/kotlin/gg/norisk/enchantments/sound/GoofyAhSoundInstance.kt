package gg.norisk.enchantments.sound

import gg.norisk.enchantments.EnchantmentRegistry.dopamin
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.sound.MovingSoundInstance
import net.minecraft.client.sound.SoundInstance
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent

@Environment(EnvType.CLIENT)
class GoofyAhSoundInstance(
    val player: PlayerEntity,
    val sound: SoundEvent = SoundRegistry.GOOFY_AAH_BEAT
) :
    MovingSoundInstance(sound, SoundCategory.PLAYERS, SoundInstance.createRandom()) {
    private var tickCount = 0

    init {
        this.repeat = false
        this.repeatDelay = 0
        this.volume = 0.5f
    }

    fun stop() {
        this.setDone()
    }

    override fun tick() {
        tickCount++
        if (!player.isRemoved && dopamin.getLevel(player.getEquippedStack(EquipmentSlot.HEAD)) != null) {
            this.x = player.x.toFloat().toDouble()
            this.y = player.y.toFloat().toDouble()
            this.z = player.z.toFloat().toDouble()
        } else {
            this.setDone()
        }
    }
}
