package gg.norisk.enchantments.mixin;

import gg.norisk.enchantments.EnchantmentUtils;
import gg.norisk.enchantments.impl.SlipperyEnchantment;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
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
}
