package gg.norisk.enchantments.mixin.freeze;

import gg.norisk.enchantments.impl.freeze.FreezeOverlayFeatureRenderer;
import gg.norisk.enchantments.utils.SlipperyBlockFeatureRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends EntityRenderer<T, S>
        implements FeatureRendererContext<S, M> {
    @Shadow
    protected abstract boolean addFeature(FeatureRenderer<S, M> feature);

    protected LivingEntityRendererMixin(EntityRendererFactory.Context context) {
        super(context);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initDataTrackerInjecetion(EntityRendererFactory.Context ctx, EntityModel model, float shadowRadius, CallbackInfo ci) {
        this.addFeature(new FreezeOverlayFeatureRenderer<>((FeatureRendererContext) (Object) this));
        //this.addFeature(new SlipperyBlockFeatureRenderer<>((FeatureRendererContext) (Object) this, ctx.getPart()));
    }
}