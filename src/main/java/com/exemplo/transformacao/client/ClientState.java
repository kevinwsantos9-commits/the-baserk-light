package com.exemplo.transformacao.client;

import com.exemplo.transformacao.data.ArmorData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Estado visto pelo cliente: meus dados (para a tela) e quem está transformado/rugindo (para desenhar). */
public final class ClientState {
    private ClientState() {}

    /** Meus dados de armadura, enviados pelo servidor. */
    public static ArmorData self = new ArmorData();

    private static final Set<UUID> TRANSFORMED = new HashSet<>();
    /** jogador -> tickCount (da entidade) em que o rugido começou. */
    private static final Map<UUID, Float> ROAR_START = new HashMap<>();

    public static void setSelf(CompoundTag tag) {
        self = ArmorData.load(tag);
    }

    public static void setState(UUID id, boolean transformed, boolean roaring) {
        if (transformed) TRANSFORMED.add(id); else TRANSFORMED.remove(id);

        if (roaring) {
            Level level = Minecraft.getInstance().level;
            Player p = level == null ? null : level.getPlayerByUUID(id);
            if (p != null) ROAR_START.put(id, (float) p.tickCount);
        } else {
            ROAR_START.remove(id);
        }
    }

    public static boolean isTransformed(UUID id) {
        return TRANSFORMED.contains(id);
    }

    /** Progresso do rugido de 0..1, ou -1 se a entidade não está rugindo. {@code age} = tickCount + partialTick. */
    public static float roarProgress(Entity entity, float age) {
        Float start = ROAR_START.get(entity.getUUID());
        if (start == null) return -1F;
        float p = (age - start) / ArmorData.ROAR_TICKS;
        if (p > 1F) {
            ROAR_START.remove(entity.getUUID());
            return -1F;
        }
        return p < 0F ? -1F : p;
    }

    public static void clear() {
        self = new ArmorData();
        TRANSFORMED.clear();
        ROAR_START.clear();
    }
}
