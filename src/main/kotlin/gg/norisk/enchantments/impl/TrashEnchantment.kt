package gg.norisk.enchantments.impl

import gg.norisk.datatracker.entity.EntityWrapper
import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.registeredTypes
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.emote.network.EmoteNetworking.playEmote
import gg.norisk.emote.network.EmoteNetworking.stopEmote
import gg.norisk.enchantments.EnchantmentRegistry.trashEnchantment
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.StupidEnchantments.toId
import gg.norisk.enchantments.sound.SoundRegistry
import gg.norisk.enchantments.utils.ItemStackSerializer
import gg.norisk.enchantments.utils.TrashItemsFeatureRenderer
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.world.GameRules
import net.silkmc.silk.commands.command
import net.silkmc.silk.network.packet.s2cPacket
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.random.Random

object TrashEnchantment {
    fun initClient() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register { entityType, entityRenderer, registrationHelper, context ->
            val modelPart = EntityModelLayers.getLayers().toList()
                .firstOrNull { it.id.path == entityType.untranslatedName.lowercase() }
            if (modelPart != null) {
                val model = context.getPart(modelPart)
                registrationHelper.register(
                    TrashItemsFeatureRenderer(
                        entityRenderer as FeatureRendererContext<LivingEntity, EntityModel<LivingEntity>>,
                        context.heldItemRenderer,
                        model
                    )
                )
            }
        }
    }

    @Serializable
    data class TrashItems(
        val trash: List<@Serializable(with = ItemStackSerializer::class) ItemStack>
    )

    fun initServer() {
        (registeredTypes as MutableMap<Any, Any>).put(
            TrashItems::class,
            s2cPacket<Pair<EntityWrapper, TrashItems>>("entity-trash-items-sync".toId())
        )

        if (!FabricLoader.getInstance().isDevelopmentEnvironment) return
        command("trash") {
            runs {
                val player = this.source.playerOrThrow
                player.toggleTrash()
            }
        }
        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvents.ServerStarted {
            it.gameRules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, it)
            it.gameRules.get(GameRules.DO_WEATHER_CYCLE).set(false, it)
            for (world in it.worlds) {
                world.timeOfDay = 6000
                world.resetWeather()
            }
        })
    }

    private fun ServerPlayerEntity.toggleTrash() {
        isTrash = !isTrash
        if (isTrash) {
            playEmote("emotes/trash_open.animation.json".toId())
        } else {
            world.playSoundFromEntity(
                null,
                this,
                SoundRegistry.TRASH_CLOSE,
                SoundCategory.PLAYERS,
                1f,
                1f
            )
            stopEmote("emotes/trash_open.animation.json".toId())
            trashItems = TrashItems(emptyList())
        }
    }

    fun PlayerEntity.applyTrash(
        itemStack: ItemStack,
        bl: Boolean,
        bl2: Boolean,
        cir: CallbackInfoReturnable<ItemEntity>
    ) {
        if (this.isTrash) {
            world.playSoundFromEntity(
                null,
                this,
                SoundEvents.ITEM_BUNDLE_REMOVE_ONE,
                SoundCategory.PLAYERS,
                1f,
                Random.nextDouble(0.9, 1.5).toFloat()
            )
            cir.returnValue = null
            trashItems = TrashItems(buildList {
                addAll(trashItems.trash)
                add(itemStack)
            })
        }
    }

    fun Entity.handleSneaking(isSneaking: Boolean) {
        val player = this as? ServerPlayerEntity? ?: return
        val trash = trashEnchantment.getLevel(getEquippedStack(EquipmentSlot.LEGS))

        if (isSneaking && trash != null) {
            player.toggleTrash()
        } else if (isTrash && trash == null) {
            player.toggleTrash()
        }
    }

    var Entity.trashItems: TrashItems
        get() = this.getSyncedData<TrashItems>("$MOD_ID:TrashItems") ?: TrashItems(emptyList())
        set(value) {
            this.setSyncedData("$MOD_ID:TrashItems", value)
        }

    var Entity.isTrash: Boolean
        get() = this.getSyncedData<Boolean>("$MOD_ID:IsTrash") ?: false
        set(value) {
            this.setSyncedData("$MOD_ID:IsTrash", value)
        }
}
