package com.exemplo.transformacao.server;

import com.exemplo.transformacao.TransformacaoMod;
import com.exemplo.transformacao.data.ArmorData;
import com.exemplo.transformacao.net.StatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = TransformacaoMod.MODID)
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            TransformationService.tick(sp);
        }
    }

    /** XP: jogador mata um monstro (inclui flechas/tridente: o "causador" é o jogador). */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getEntity() instanceof Enemy
                && event.getSource().getEntity() instanceof ServerPlayer sp) {
            TransformationService.onKill(sp, event.getEntity());
        }
    }

    /** Durante a transformação o jogador não leva dano nenhum (raio incluído). */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && TransformationService.isProtected(sp)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) TransformationService.onJoin(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) TransformationService.onJoin(sp);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) TransformationService.onJoin(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) TransformationService.onLogout(sp);
    }

    /** Nível/XP/pontos sobrevivem à morte; a armadura volta ao estado "destransformada". */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        ArmorData data = ArmorData.get(event.getOriginal());
        if (event.isWasDeath()) data.transformed = false;
        data.store(event.getEntity());
    }

    /** Quem passa a ver um jogador transformado precisa ser avisado para desenhar a armadura. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
            if (ArmorData.get(target).transformed) {
                PacketDistributor.sendToPlayer(viewer, new StatePayload(target.getUUID(), true, false));
            }
        }
    }
}
