package gg.norisk.enchantments.sound

import gg.norisk.enchantments.impl.GlitchEnchantment.isGlitching
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.sound.MovingSoundInstance
import net.minecraft.client.sound.SoundInstance
import net.minecraft.entity.Entity
import net.minecraft.sound.SoundCategory

class GlitchSoundInstance(
    val entity: Entity,
) : MovingSoundInstance(SoundRegistry.GLITCH, SoundCategory.PLAYERS, SoundInstance.createRandom()) {
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
        if (!entity.isRemoved && entity.isGlitching) {
            this.x = entity.x.toFloat().toDouble()
            this.y = entity.y.toFloat().toDouble()
            this.z = entity.z.toFloat().toDouble()
        } else {
            this.setDone()
        }
    }
}
