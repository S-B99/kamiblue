package org.kamiblue.client.mixin.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBoat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import org.kamiblue.client.module.modules.movement.BoatFly;
import org.kamiblue.client.util.Wrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created by 086 on 15/12/2017.
 */
@Mixin(ModelBoat.class)
public class MixinModelBoat {

    @Inject(method = "render", at = @At("HEAD"))
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo info) {
        if (Wrapper.getPlayer().getRidingEntity() == entityIn && BoatFly.INSTANCE.isEnabled()) {
            GlStateManager.color(1.0f, 1.0f, 1.0f, BoatFly.INSTANCE.getOpacity());
            GlStateManager.enableBlend();
        }
    }

    @Inject(method = { "render" }, at = { @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBoat;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V") }, cancellable = true)
    private void onRender(final Entity entityIn, final float limbSwing, final float limbSwingAmount, final float ageInTicks, final float netHeadYaw, final float headPitch, final float scale, final CallbackInfo ci) {
        if (BoatFly.INSTANCE.isEnabled() && entityIn == Wrapper.getPlayer().getRidingEntity()) {
            final double size = BoatFly.INSTANCE.getSize();
            //in 3rd person this thing freaks out
            if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 && size != 1.0) {
                GlStateManager.scale(size, size, size);
            }
        }
    }

}
