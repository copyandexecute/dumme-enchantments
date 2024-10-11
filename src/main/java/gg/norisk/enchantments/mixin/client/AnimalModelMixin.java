package gg.norisk.enchantments.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.norisk.enchantments.impl.BalloonEnchantment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(AnimalModel.class)
public abstract class AnimalModelMixin<E extends Entity> extends EntityModel<E> {
    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Ljava/lang/Iterable;forEach(Ljava/util/function/Consumer;)V", ordinal = 2)
    )
    private void stupid$BalloonRenderScale(Iterable<ModelPart> instance, Consumer<ModelPart> consumer, Operation<Void> original, MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int k) {
        BalloonEnchantment.INSTANCE.handleHead(this, instance, consumer, original, matrixStack);
    }
}
