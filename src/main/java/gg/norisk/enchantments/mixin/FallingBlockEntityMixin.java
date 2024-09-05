package gg.norisk.enchantments.mixin;

import gg.norisk.enchantments.impl.HelicopterEnchantment;
import gg.norisk.enchantments.utils.Animation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity implements HelicopterEnchantment.HelicopterBlock {
    public FallingBlockEntityMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Unique
    private final Animation animation = HelicopterEnchantment.INSTANCE.defaultAnimation();

    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true)
    private void getGravityInjection(CallbackInfoReturnable<Double> cir) {
        HelicopterEnchantment.INSTANCE.applyGravity((FallingBlockEntity) (Object) this, cir);
    }

    @NotNull
    @Override
    public Animation getAnimation() {
        return animation;
    }
}
