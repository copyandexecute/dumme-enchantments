package gg.norisk.enchantments.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.norisk.enchantments.impl.GlitchEnchantment;
import gg.norisk.enchantments.impl.MedusaEnchantment;
import gg.norisk.enchantments.impl.RollEnchantment;
import gg.norisk.enchantments.impl.SlipperyEnchantment;
import gg.norisk.enchantments.impl.SquishEnchantment;
import gg.norisk.enchantments.impl.TrashEnchantment;
import gg.norisk.enchantments.utils.Animation;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin implements SquishEnchantment.SquishEntity, MedusaEnchantment.MedusaEntity {
    @Unique
    private final List<Animation> squishAnimations = new ArrayList<>();
    @Unique
    private Vec3d squishSize = new Vec3d(1, 1, 1);

    @Unique
    private Float stupidAnimationProgress;
    @Unique
    private Float stupidBodyYaw;
    @Unique
    private Float stupidHeadYaw;
    @Unique
    private Float stupidPitch;
    @Unique
    private Float stupidLimbSpeed;
    @Unique
    private Float stupidLimbPos;

    @NotNull
    @Override
    public List<Animation> getSquishAnimations() {
        return squishAnimations;
    }

    @NotNull
    @Override
    public Vec3d getSquishSize() {
        return squishSize;
    }

    @Override
    public void setSquishSize(@NotNull Vec3d vec3d) {
        this.squishSize = vec3d;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickInjection(CallbackInfo ci) {
        SquishEnchantment.INSTANCE.onTick((Entity) (Object) this);
        GlitchEnchantment.INSTANCE.onTick((Entity) (Object) this);
    }

    @Inject(method = "setSneaking", at = @At("HEAD"))
    private void setSneakingInjection(boolean bl, CallbackInfo ci) {
        var entity = (Entity) (Object) this;
        TrashEnchantment.INSTANCE.handleSneaking(entity, bl);
        RollEnchantment.INSTANCE.handleSneaking(entity, bl);
    }


    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void stupid$changePitch(double d, double e, CallbackInfo callBackInfo) {
        RollEnchantment.INSTANCE.handlePitchChange((Entity) (Object) this, d, e, callBackInfo);
    }

    @WrapOperation(
            method = "playStepSound",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V")
    )
    private void playStepSoundWrapper(Entity instance, SoundEvent soundEvent, float f, float g, Operation<Void> original) {
        if (SlipperyEnchantment.INSTANCE.applyStepSound(instance, soundEvent, f, g, original)) {
        } else if (RollEnchantment.INSTANCE.applyStepSound(instance, soundEvent, f, g, original)) {
        } else {
            original.call(instance, soundEvent, f, g);
        }
    }

    @Nullable
    @Override
    public Float getStupid_animationProgress() {
        return stupidAnimationProgress;
    }

    @Override
    public void setStupid_animationProgress(@Nullable Float value) {
        stupidAnimationProgress = value;
    }

    @Nullable
    @Override
    public Float getStupid_bodyYaw() {
        return stupidBodyYaw;
    }

    @Override
    public void setStupid_bodyYaw(@Nullable Float aFloat) {
        stupidBodyYaw = aFloat;
    }

    @Nullable
    @Override
    public Float getStupid_headYaw() {
        return stupidHeadYaw;
    }

    @Override
    public void setStupid_headYaw(@Nullable Float aFloat) {
        stupidHeadYaw = aFloat;
    }

    @Nullable
    @Override
    public Float getStupid_pitch() {
        return stupidPitch;
    }

    @Override
    public void setStupid_pitch(@Nullable Float aFloat) {
        stupidPitch = aFloat;
    }

    @Nullable
    @Override
    public Float getStupid_limbPos() {
        return stupidLimbPos;
    }

    @Override
    public void setStupid_limbPos(@Nullable Float aFloat) {
        stupidLimbPos = aFloat;
    }

    @Nullable
    @Override
    public Float getStupid_limbSpeed() {
        return stupidLimbSpeed;
    }

    @Override
    public void setStupid_limbSpeed(@Nullable Float aFloat) {
        stupidLimbSpeed = aFloat;
    }
}
