package gg.norisk.enchantments.mixin;

import gg.norisk.enchantments.EnchantmentUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(method = "onTargetDamaged(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
    private static void onTargetDamagedInjection(ServerWorld serverWorld, Entity entity, DamageSource damageSource, ItemStack itemStack, CallbackInfo ci) {
        EnchantmentUtils.INSTANCE.applyTargetDamage(serverWorld,entity,damageSource,itemStack);
    }

    @Inject(method = "onProjectileSpawned", at = @At("TAIL"))
    private static void stupid$onProjectileSpawned(ServerWorld serverWorld, ItemStack itemStack, PersistentProjectileEntity persistentProjectileEntity, Consumer<Item> consumer, CallbackInfo ci) {
        EnchantmentUtils.INSTANCE.applyProjectileSpawned(serverWorld,itemStack, persistentProjectileEntity);
    }
}
