package gg.norisk.enchantments.mixin.client;

import gg.norisk.enchantments.impl.BalloonEnchantment;
import gg.norisk.enchantments.utils.Animation;
import net.minecraft.client.render.entity.model.EntityModel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityModel.class)
public abstract class EntityModelMixin implements BalloonEnchantment.BallonModel {
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
}
