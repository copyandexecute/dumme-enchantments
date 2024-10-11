package gg.norisk.enchantments.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.norisk.enchantments.EnchantmentRegistry;
import gg.norisk.enchantments.EnchantmentUtils;
import gg.norisk.enchantments.impl.InvertedEnchantment;
import gg.norisk.enchantments.impl.MedusaEnchantment;
import gg.norisk.enchantments.impl.SlipperyEnchantment;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    protected abstract float getKnockbackAgainst(Entity entity, DamageSource damageSource);

    @Shadow
    public abstract boolean damage(DamageSource damageSource, float f);

    public LivingEntityMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true)
    private void getGravityInjection(CallbackInfoReturnable<Double> cir) {
        EnchantmentUtils.INSTANCE.applyGravity((LivingEntity) (Object) this, cir);
    }

    @Inject(method = "getFallSound", at = @At("RETURN"), cancellable = true)
    private void getFallSoundInjection(int i, CallbackInfoReturnable<SoundEvent> cir) {
        EnchantmentUtils.INSTANCE.applyFallSound((LivingEntity) (Object) this, i, cir);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getSlipperiness()F"))
    private float injected(Block instance) {
        return SlipperyEnchantment.INSTANCE.apply((LivingEntity) (Object) this, instance);
    }

    @ModifyExpressionValue(method = "playHurtSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getHurtSound(Lnet/minecraft/entity/damage/DamageSource;)Lnet/minecraft/sound/SoundEvent;"))
    private SoundEvent stupid$MedusaHurtSound(SoundEvent original) {
        if (MedusaEnchantment.INSTANCE.isStupidMedusa((LivingEntity) (Object) this)) {
            return Blocks.STONE.getDefaultState().getSoundGroup().getBreakSound();
        }
        return original;
    }

    @WrapOperation(
            method = "damage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;takeKnockback(DDD)V")
    )
    private void stupid$invertedKnockback(LivingEntity instance, double oldD, double oldE, double oldF, Operation<Void> original, DamageSource damageSource, float f) {
        if (instance instanceof PlayerEntity player && player == (LivingEntity) (Object) this && EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getInverted(), player.getMainHandStack()) != null) {
            double d = 0.0;
            double e = 0.0;
            if (damageSource.getSource() instanceof ProjectileEntity projectileEntity) {
                DoubleDoubleImmutablePair doubleDoubleImmutablePair = projectileEntity.getKnockback(instance, damageSource);
                d = -doubleDoubleImmutablePair.leftDouble();
                e = -doubleDoubleImmutablePair.rightDouble();
            } else if (damageSource.getPosition() != null) {
                var pos = InvertedEnchantment.INSTANCE.getStupidKnockbackPos(player);
                d = pos.getX() - this.getX();
                e = pos.getZ() - this.getZ();
            }
            float knockbackAgainst = 1 + this.getKnockbackAgainst(player, damageSource);
            original.call(instance, (double) (knockbackAgainst * oldD), d, e);
        } else {
            original.call(instance, oldD, oldE, oldF);
        }
    }
}
