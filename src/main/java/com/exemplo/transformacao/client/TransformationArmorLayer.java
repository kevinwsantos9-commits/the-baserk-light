package com.exemplo.transformacao.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Desenha a armadura (armadura.obj) por cima dos jogadores que estão transformados. */
public class TransformationArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public TransformationArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !ClientState.isTransformed(player.getUUID())) return;

        ArmorObjModel model = ArmorObjModel.get();
        if (model == null) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(ArmorObjModel.TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        model.render(poseStack, vc, packedLight, overlay, getParentModel());
    }
}
