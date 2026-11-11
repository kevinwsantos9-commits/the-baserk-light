package com.exemplo.transformacao.server;

import com.exemplo.transformacao.TransformacaoMod;
import com.exemplo.transformacao.data.ArmorData;
import com.exemplo.transformacao.net.ActionPayload;
import com.exemplo.transformacao.net.SelfDataPayload;
import com.exemplo.transformacao.net.StatePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Toda a lógica de servidor: XP, pontos, sequência de transformação e bônus de atributos. */
public final class TransformationService {
    private TransformationService() {}

    /** jogador -> ticks decorridos desde que começou a rugir. */
    private static final Map<UUID, Integer> ROAR = new HashMap<>();
    /** jogador -> game time até o qual ele não toma dano (durante a transformação). */
    private static final Map<UUID, Long> PROTECT_UNTIL = new HashMap<>();
    /** jogador -> game time em que pode usar o botão de novo. */
    private static final Map<UUID, Long> NEXT_USE = new HashMap<>();

    // ---------------------------------------------------------------- entrada de rede

    public static void handleAction(ActionPayload payload, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        switch (payload.action()) {
            case ActionPayload.TOGGLE -> toggle(sp);
            case ActionPayload.SPEND -> spendPoint(sp, payload.arg());
            default -> { }
        }
    }

    // ---------------------------------------------------------------- transformação

    public static void toggle(ServerPlayer sp) {
        if (!sp.isAlive() || sp.isSpectator()) return;
        UUID id = sp.getUUID();
        if (ROAR.containsKey(id)) return; // já está no meio da sequência
        long now = sp.level().getGameTime();
        if (now < NEXT_USE.getOrDefault(id, 0L)) return;

        ArmorData data = ArmorData.get(sp);
        if (data.transformed) {
            untransform(sp, data);
            NEXT_USE.put(id, now + 40);
        } else {
            startRoar(sp);
        }
    }

    private static void startRoar(ServerPlayer sp) {
        UUID id = sp.getUUID();
        ServerLevel level = sp.serverLevel();
        long now = level.getGameTime();

        ROAR.put(id, 0);
        PROTECT_UNTIL.put(id, now + ArmorData.ROAR_TICKS + 60);
        NEXT_USE.put(id, now + ArmorData.ROAR_TICKS + 20);

        // "trava" o jogador no lugar durante o rugido
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ArmorData.ROAR_TICKS + 10, 5, false, false, false));

        // grunhido
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.6F, 1.2F);

        // avisa todo mundo que vê o jogador para tocar a animação de rugido
        broadcastState(sp, false, true);
    }

    /** Chamado todo tick de cada jogador no servidor. */
    public static void tick(ServerPlayer sp) {
        UUID id = sp.getUUID();
        Integer current = ROAR.get(id);
        if (current == null) return;

        if (!sp.isAlive()) {
            cancel(sp);
            return;
        }

        int t = current + 1;
        ROAR.put(id, t);
        ServerLevel level = sp.serverLevel();

        if (t < ArmorData.ROAR_TICKS && t % 2 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, sp.getX(), sp.getY() + 1.0, sp.getZ(), 5, 0.5, 0.8, 0.5, 0.15);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, sp.getX(), sp.getY() + 0.1, sp.getZ(), 2, 0.5, 0.05, 0.5, 0.02);
        }

        if (t == ArmorData.ROAR_TICKS) {
            strikeLightning(sp, level);
        }

        if (t >= ArmorData.ROAR_TICKS + 2) {
            finishTransformation(sp);
        }
    }

    private static void strikeLightning(ServerPlayer sp, ServerLevel level) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(sp.getX(), sp.getY(), sp.getZ());
            bolt.setVisualOnly(true); // só efeito visual/sonoro: sem dano, sem fogo
            level.addFreshEntity(bolt);
        }
        sp.clearFire();
        level.sendParticles(ParticleTypes.FLASH, sp.getX(), sp.getY() + 1.0, sp.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, sp.getX(), sp.getY() + 1.0, sp.getZ(), 80, 0.6, 1.0, 0.6, 0.5);
    }

    private static void finishTransformation(ServerPlayer sp) {
        ROAR.remove(sp.getUUID());

        ArmorData data = ArmorData.get(sp);
        data.transformed = true;
        data.store(sp);

        sp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        sp.clearFire();
        applyBonuses(sp, data);
        sp.setHealth(sp.getMaxHealth());

        sp.serverLevel().sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.0, sp.getZ(), 40, 0.5, 0.9, 0.5, 0.2);
        syncSelf(sp, data);
        broadcastState(sp, true, false);
    }

    private static void cancel(ServerPlayer sp) {
        ROAR.remove(sp.getUUID());
        PROTECT_UNTIL.remove(sp.getUUID());
        sp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        ArmorData data = ArmorData.get(sp);
        broadcastState(sp, data.transformed, false);
    }

    public static void untransform(ServerPlayer sp, ArmorData data) {
        data.transformed = false;
        data.store(sp);
        removeBonuses(sp);

        ServerLevel level = sp.serverLevel();
        level.sendParticles(ParticleTypes.POOF, sp.getX(), sp.getY() + 1.0, sp.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);

        syncSelf(sp, data);
        broadcastState(sp, false, false);
    }

    /** O jogador está no meio da transformação (imune a qualquer dano, inclusive o do raio)? */
    public static boolean isProtected(ServerPlayer sp) {
        Long until = PROTECT_UNTIL.get(sp.getUUID());
        return until != null && until > sp.level().getGameTime();
    }

    // ---------------------------------------------------------------- XP e pontos

    public static void onKill(ServerPlayer sp, LivingEntity victim) {
        ArmorData data = ArmorData.get(sp);
        int gain = Math.max(1, (int) Math.ceil(victim.getMaxHealth() / 2.0F));
        if (data.transformed) gain *= 2;

        int levels = data.addXp(gain);
        data.store(sp);

        sp.displayClientMessage(Component.translatable("msg.transformacao.xp", gain), true);
        if (levels > 0) {
            sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            sp.sendSystemMessage(Component.translatable("msg.transformacao.levelup", data.level));
        }
        syncSelf(sp, data);
    }

    private static void spendPoint(ServerPlayer sp, int stat) {
        ArmorData data = ArmorData.get(sp);
        if (!data.spend(stat)) return;
        data.store(sp);
        if (data.transformed) {
            double before = sp.getMaxHealth();
            applyBonuses(sp, data);
            if (sp.getMaxHealth() > before) sp.heal((float) (sp.getMaxHealth() - before));
        }
        syncSelf(sp, data);
    }

    // ---------------------------------------------------------------- atributos

    public static void applyBonuses(ServerPlayer sp, ArmorData d) {
        setModifier(sp, Attributes.ATTACK_DAMAGE, "bonus_attack",
                ArmorData.BASE_ATTACK + ArmorData.PER_STR_ATTACK * d.stats[ArmorData.STR],
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(sp, Attributes.MAX_HEALTH, "bonus_health",
                ArmorData.BASE_HEALTH + ArmorData.PER_VIT_HEALTH * d.stats[ArmorData.VIT],
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(sp, Attributes.ARMOR, "bonus_armor",
                ArmorData.BASE_ARMOR + ArmorData.PER_DEF_ARMOR * d.stats[ArmorData.DEF],
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(sp, Attributes.MOVEMENT_SPEED, "bonus_speed",
                ArmorData.BASE_SPEED + ArmorData.PER_AGI_SPEED * d.stats[ArmorData.AGI],
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    public static void removeBonuses(ServerPlayer sp) {
        setModifier(sp, Attributes.ATTACK_DAMAGE, "bonus_attack", 0, AttributeModifier.Operation.ADD_VALUE);
        setModifier(sp, Attributes.MAX_HEALTH, "bonus_health", 0, AttributeModifier.Operation.ADD_VALUE);
        setModifier(sp, Attributes.ARMOR, "bonus_armor", 0, AttributeModifier.Operation.ADD_VALUE);
        setModifier(sp, Attributes.MOVEMENT_SPEED, "bonus_speed", 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (sp.getHealth() > sp.getMaxHealth()) sp.setHealth(sp.getMaxHealth());
    }

    private static void setModifier(ServerPlayer sp, Holder<Attribute> attribute, String name, double amount,
                                    AttributeModifier.Operation op) {
        AttributeInstance instance = sp.getAttribute(attribute);
        if (instance == null) return;
        ResourceLocation id = TransformacaoMod.id(name);
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    // ---------------------------------------------------------------- sincronização

    public static void syncSelf(ServerPlayer sp, ArmorData data) {
        PacketDistributor.sendToPlayer(sp, new SelfDataPayload(data.save()));
    }

    public static void broadcastState(ServerPlayer sp, boolean transformed, boolean roaring) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, new StatePayload(sp.getUUID(), transformed, roaring));
    }

    /** Login, respawn e troca de dimensão: reaplica bônus (modificadores transitórios se perdem) e ressincroniza. */
    public static void onJoin(ServerPlayer sp) {
        ArmorData data = ArmorData.get(sp);
        if (data.transformed) applyBonuses(sp, data);
        syncSelf(sp, data);
        broadcastState(sp, data.transformed, false);
    }

    public static void onLogout(ServerPlayer sp) {
        UUID id = sp.getUUID();
        ROAR.remove(id);
        PROTECT_UNTIL.remove(id);
        NEXT_USE.remove(id);
    }
}
