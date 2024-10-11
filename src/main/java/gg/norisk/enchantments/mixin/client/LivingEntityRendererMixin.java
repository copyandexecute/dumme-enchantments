package gg.norisk.enchantments.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.norisk.enchantments.impl.BalloonEnchantment;
import gg.norisk.enchantments.impl.MedusaEnchantment;
import gg.norisk.enchantments.impl.SquishEnchantment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> {
    @Shadow
    protected M model;

    protected LivingEntityRendererMixin(EntityRendererFactory.Context context) {
        super(context);
    }


    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;scale(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V", shift = At.Shift.AFTER))
    private void afterScale(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        SquishEnchantment.INSTANCE.handleSquishRendering(livingEntity, matrixStack);
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "HEAD"))
    private void stupid$balloonHeadRendering(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        ((BalloonEnchantment.BallonModel) this.model).setStupid_ballonAnimation(BalloonEnchantment.INSTANCE.getStupidBallooning(livingEntity));
    }

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getAnimationProgress(Lnet/minecraft/entity/LivingEntity;F)F")
    )
    private float stupid$medusaAnimationProgress(float original, T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if (livingEntity instanceof MedusaEnchantment.MedusaEntity medusa && MedusaEnchantment.INSTANCE.isStupidMedusa(livingEntity)) {
            if (medusa.getStupid_animationProgress() == null) {
                medusa.setStupid_animationProgress(original);
            }
            return medusa.getStupid_animationProgress();
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F", ordinal = 0)
    )
    private float stupid$medusaBodyYaw(float original, T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if (livingEntity instanceof MedusaEnchantment.MedusaEntity medusa && MedusaEnchantment.INSTANCE.isStupidMedusa(livingEntity)) {
            if (medusa.getStupid_bodyYaw() == null) {
                medusa.setStupid_bodyYaw(original);
            }
            return medusa.getStupid_bodyYaw();
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F", ordinal = 1)
    )
    private float stupid$medusaHeadYaw(float original, T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if (livingEntity instanceof MedusaEnchantment.MedusaEntity medusa && MedusaEnchantment.INSTANCE.isStupidMedusa(livingEntity)) {
            if (medusa.getStupid_headYaw() == null) {
                medusa.setStupid_headYaw(original);
            }
            return medusa.getStupid_headYaw();
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F")
    )
    private float stupid$medusaPitch(float original, T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if (livingEntity instanceof MedusaEnchantment.MedusaEntity medusa && MedusaEnchantment.INSTANCE.isStupidMedusa(livingEntity)) {
            if (medusa.getStupid_pitch() == null) {
                medusa.setStupid_pitch(original);
            }
            return medusa.getStupid_pitch();
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LimbAnimator;getSpeed(F)F")
    )
    private float stupid$medusaLimbSpeed(float original, T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if (livingEntity instanceof MedusaEnchantment.MedusaEntity medusa && MedusaEnchantment.INSTANCE.isStupidMedusa(livingEntity)) {
            if (medusa.getStupid_limbSpeed() == null) {
                medusa.setStupid_limbSpeed(original);
            }
            return medusa.getStupid_limbSpeed();
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LimbAnimator;getPos(F)F")
    )
    private float stupid$medusaLimbPos(float original, T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if (livingEntity instanceof MedusaEnchantment.MedusaEntity medusa && MedusaEnchantment.INSTANCE.isStupidMedusa(livingEntity)) {
            if (medusa.getStupid_limbPos() == null) {
                medusa.setStupid_limbPos(original);
            }
            return medusa.getStupid_limbPos();
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "getRenderLayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getTexture(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/Identifier;")
    )
    private Identifier stupid$MedusaTexture(Identifier original, T livingEntity, boolean bl, boolean bl2, boolean bl3) {
        if (MedusaEnchantment.INSTANCE.isStupidMedusa(livingEntity)) {
            return MedusaEnchantment.INSTANCE.getTexture();
        } else {
            return original;
        }
    }
}
