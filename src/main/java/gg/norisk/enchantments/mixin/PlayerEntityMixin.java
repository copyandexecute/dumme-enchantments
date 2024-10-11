package gg.norisk.enchantments.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.norisk.enchantments.EnchantmentRegistry;
import gg.norisk.enchantments.EnchantmentUtils;
import gg.norisk.enchantments.impl.ColossalEnchantment;
import gg.norisk.enchantments.impl.TrashEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements ColossalEnchantment.ColossalPlayer {
    @Shadow
    public abstract ActionResult interact(Entity entity, Hand hand);

    @Shadow
    public abstract boolean canInteractWithBlockAt(BlockPos blockPos, double d);

    @Shadow
    public abstract void attack(Entity entity);

    @Shadow public abstract void playSound(SoundEvent soundEvent, float f, float g);

    @Unique
    private Direction lastSide;
    @Unique
    private boolean stupidIsInvertingAttack;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;isClient:Z"), cancellable = true)
    private void dropItemInjection(ItemStack itemStack, boolean bl, boolean bl2, CallbackInfoReturnable<ItemEntity> cir) {
        TrashEnchantment.INSTANCE.applyTrash((PlayerEntity) ((Object) this), itemStack, bl, bl2, cir);
    }

    @Override
    public void setStupid_lastSide(@Nullable Direction direction) {
        lastSide = direction;
    }

    @Nullable
    @Override
    public Direction getStupid_lastSide() {
        return lastSide;
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;handleAttack(Lnet/minecraft/entity/Entity;)Z"), cancellable = true)
    private void stupid$invertAttack(Entity entity, CallbackInfo ci) {
        var player = (PlayerEntity) (Object) this;
        if (EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getInverted(), player.getMainHandStack()) != null && !stupidIsInvertingAttack) {
            stupidIsInvertingAttack = true;
            attack(player);
            ci.cancel();
        } else {
            stupidIsInvertingAttack = false;
        }
    }

    @ModifyReceiver(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;takeKnockback(DDD)V")
    )
    private LivingEntity stupid$invertedKnockback(LivingEntity instance, double d, double e, double f) {
        if ((PlayerEntity) (Object) this instanceof PlayerEntity player && EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getInverted(), player.getMainHandStack()) != null) {
            player.velocityDirty = true;
            player.velocityModified = true;
            return player;
        }
        return instance;
    }

    @WrapOperation(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;takeKnockback(DDD)V")
    )
    private void stupid$invertedKnockback(LivingEntity instance, double d, double e, double f, Operation<Void> original) {
    }
}
