package com.exemplo.transformacao.mixin;

import com.exemplo.transformacao.client.RoarPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Depois que o Minecraft calcula a pose normal do jogador, aplicamos a pose de rugido por cima.
 * require = 0: se algum dia o método mudar de assinatura, o mod não crasha (só perde a animação).
 */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void transformacao$roar(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                    float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        RoarPose.apply((PlayerModel<?>) (Object) this, entity, ageInTicks);
    }
}
