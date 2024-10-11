package gg.norisk.enchantments.mixin.client;

import gg.norisk.enchantments.impl.BalloonEnchantment;
import gg.norisk.enchantments.utils.Animation;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements BalloonEnchantment.BallonModelPart {
    @Unique
    private Animation stupidBalloonAnimation;

    @Nullable
    @Override
    public Animation getStupid_ballonAnimation() {
        return stupidBalloonAnimation;
    }

    @Override
    public void setStupid_ballonAnimation(@Nullable Animation animation) {
        stupidBalloonAnimation = animation;
    }

    @Inject(method = "rotate(Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("TAIL"))
    private void stupid$BalloonScaling(MatrixStack matrixStack, CallbackInfo ci) {
        if (stupidBalloonAnimation != null) {
            matrixStack.scale(stupidBalloonAnimation.get(), stupidBalloonAnimation.get(), stupidBalloonAnimation.get());
        }
    }
}
