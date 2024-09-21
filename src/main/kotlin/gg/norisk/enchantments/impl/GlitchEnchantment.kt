package gg.norisk.enchantments.impl

import gg.norisk.datatracker.entity.ISyncedEntity
import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.enchantments.EnchantmentRegistry.glitch
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.mixin.client.GameRendererAccessor
import gg.norisk.enchantments.sound.GlitchSoundInstance
import kotlinx.coroutines.Job
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Identifier
import net.silkmc.silk.core.event.Event
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.infiniteMcCoroutineTask
import net.silkmc.silk.core.task.mcCoroutineTask

object GlitchEnchantment {
    var glitchJob: Job? = null
    private val glitches = listOf(
        Identifier.ofVanilla("shaders/post/creeper.json"),
        Identifier.ofVanilla("shaders/post/spider.json"),
        Identifier.ofVanilla("shaders/post/invert.json")
    )

    fun initClient() {
        isGlitchingToggleEvent.listen { event ->
            if (event.entity == MinecraftClient.getInstance().player) {
                glitchJob?.cancel()
                mcCoroutineTask(client = true, sync = true) {
                    MinecraftClient.getInstance().gameRenderer.disablePostProcessor()
                }
                if (event.newValue) {
                    glitchJob = infiniteMcCoroutineTask(period = 4.ticks, client = true, sync = true) {
                        (MinecraftClient.getInstance().gameRenderer as GameRendererAccessor).invokeLoadPostProcessor(
                            glitches.random()
                        )
                    }
                }
            }
            if (event.newValue) {
                MinecraftClient.getInstance().soundManager.play(GlitchSoundInstance(event.entity))
            }
        }
    }

    fun Entity.onTick() {
        if (this is PlayerEntity) {
            val hasGlitch = glitch.getLevel(getEquippedStack(EquipmentSlot.FEET))
            if (!isSpectator) {
                isGlitching = hasGlitch != null && isSneaking
                if (isGlitching) {
                    noClip = isGlitching
                }
            } else {
                isGlitching = false
            }
        }
    }

    data class EntityGlitchingToggleEvent(val entity: Entity, val newValue: Boolean)

    val isGlitchingToggleEvent = Event.onlySync<EntityGlitchingToggleEvent>()

    var Entity.isGlitching: Boolean
        get() = this.getSyncedData<Boolean>("$MOD_ID:IsGlitching") ?: false
        set(value) {
            val preValue = (this as ISyncedEntity).getSyncedValuesMap()["$MOD_ID:IsGlitching"]
            if (preValue != value) {
                (this as ISyncedEntity).getSyncedValuesMap()["$MOD_ID:IsGlitching"] = value
                isGlitchingToggleEvent.invoke(EntityGlitchingToggleEvent(this, value))
            }
        }
}
