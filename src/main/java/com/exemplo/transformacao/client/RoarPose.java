package com.exemplo.transformacao.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Pose de "grunhido": cabeça para cima, peito para fora, braços abertos e tremor. */
public final class RoarPose {
    private RoarPose() {}

    public static void apply(PlayerModel<?> m, LivingEntity entity, float age) {
        float p = ClientState.roarProgress(entity, age);
        if (p < 0F) return;

        // entra suave nos primeiros 20 %, sai suave nos últimos 15 %
        float in = smooth(Mth.clamp(p / 0.20F, 0F, 1F));
        float out = 1F - smooth(Mth.clamp((p - 0.85F) / 0.15F, 0F, 1F));
        float w = in * out;
        float shake = Mth.sin(age * 2.9F) * 0.05F * w;

        m.head.xRot = Mth.lerp(w, m.head.xRot, -0.80F) + shake;
        m.head.yRot = Mth.lerp(w, m.head.yRot, 0F);
        m.body.xRot = Mth.lerp(w, m.body.xRot, -0.12F);

        m.rightArm.xRot = Mth.lerp(w, m.rightArm.xRot, -0.35F);
        m.rightArm.zRot = Mth.lerp(w, m.rightArm.zRot, 1.15F) + shake * 3F;
        m.leftArm.xRot = Mth.lerp(w, m.leftArm.xRot, -0.35F);
        m.leftArm.zRot = Mth.lerp(w, m.leftArm.zRot, -1.15F) - shake * 3F;

        m.rightLeg.zRot = Mth.lerp(w, m.rightLeg.zRot, 0.20F);
        m.leftLeg.zRot = Mth.lerp(w, m.leftLeg.zRot, -0.20F);

        // camadas de roupa (chapéu, jaqueta, mangas, calças) precisam copiar as partes que mexemos
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
    }

    private static float smooth(float t) {
        return t * t * (3F - 2F * t);
    }
}
