package com.exemplo.transformacao.client;

import com.exemplo.transformacao.TransformacaoMod;
import com.exemplo.transformacao.net.ActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Eventos de jogo do cliente: teclas e tremor de câmera durante o rugido. */
@EventBusSubscriber(modid = TransformacaoMod.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        while (KeyBindings.TRANSFORM.consumeClick()) {
            if (mc.screen == null) {
                PacketDistributor.sendToServer(new ActionPayload(ActionPayload.TOGGLE, 0));
            }
        }
        while (KeyBindings.STATUS.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new StatusScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        float age = (float) (player.tickCount + event.getPartialTick());
        float p = ClientState.roarProgress(player, age);
        if (p < 0F) return;

        float w = Mth.sin((float) Math.PI * p); // sobe e desce ao longo do rugido
        event.setRoll(event.getRoll() + Mth.sin(age * 2.3F) * 1.6F * w);
        event.setYaw(event.getYaw() + Mth.sin(age * 3.7F) * 0.8F * w);
        event.setPitch(event.getPitch() + Mth.sin(age * 3.1F) * 0.6F * w);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.clear();
    }
}
