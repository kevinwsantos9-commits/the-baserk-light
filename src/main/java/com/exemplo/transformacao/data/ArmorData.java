package com.exemplo.transformacao.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Dados da armadura de um jogador (nível, XP, pontos e status).
 * Guardado no NBT persistente do jogador, então sobrevive a relogar e a morrer
 * (a cópia na morte é feita em ServerEvents).
 */
public class ArmorData {
    // ===== Ajuste aqui o balanceamento =====
    public static final int MAX_LEVEL = 30;
    public static final int MAX_STAT = 20;          // pontos máximos por status
    public static final int ROAR_TICKS = 40;        // duração do rugido antes do raio (20 ticks = 1 s)

    public static final double BASE_ATTACK = 2.0;   // bônus ao se transformar
    public static final double BASE_HEALTH = 4.0;   // 4 = 2 corações
    public static final double BASE_ARMOR = 4.0;
    public static final double BASE_SPEED = 0.05;   // +5 %

    public static final double PER_STR_ATTACK = 0.5;
    public static final double PER_VIT_HEALTH = 2.0;
    public static final double PER_DEF_ARMOR = 1.0;
    public static final double PER_AGI_SPEED = 0.03; // +3 % por ponto
    // =======================================

    public static final int STR = 0, VIT = 1, DEF = 2, AGI = 3;
    private static final String KEY = "transformacao";

    public int level = 1;
    public int xp = 0;
    public int points = 0;
    public final int[] stats = new int[4];
    public boolean transformed = false;

    public int xpForNext() {
        return xpForNext(level);
    }

    public static int xpForNext(int level) {
        return 50 + 25 * (level - 1);
    }

    /** Soma XP e devolve quantos níveis foram ganhos. */
    public int addXp(int amount) {
        if (level >= MAX_LEVEL) return 0;
        xp += amount;
        int gained = 0;
        while (level < MAX_LEVEL && xp >= xpForNext()) {
            xp -= xpForNext();
            level++;
            points++;
            gained++;
        }
        if (level >= MAX_LEVEL) xp = 0;
        return gained;
    }

    public boolean spend(int stat) {
        if (stat < 0 || stat >= stats.length) return false;
        if (points <= 0 || stats[stat] >= MAX_STAT) return false;
        stats[stat]++;
        points--;
        return true;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("level", level);
        t.putInt("xp", xp);
        t.putInt("points", points);
        t.putIntArray("stats", stats.clone());
        t.putBoolean("transformed", transformed);
        return t;
    }

    public static ArmorData load(CompoundTag t) {
        ArmorData d = new ArmorData();
        if (t == null || t.isEmpty()) return d;
        d.level = Math.max(1, Math.min(MAX_LEVEL, t.getInt("level")));
        d.xp = Math.max(0, t.getInt("xp"));
        d.points = Math.max(0, t.getInt("points"));
        int[] s = t.getIntArray("stats");
        for (int i = 0; i < d.stats.length && i < s.length; i++) {
            d.stats[i] = Math.max(0, Math.min(MAX_STAT, s[i]));
        }
        d.transformed = t.getBoolean("transformed");
        return d;
    }

    public static ArmorData get(Player player) {
        return load(player.getPersistentData().getCompound(KEY));
    }

    public void store(Player player) {
        player.getPersistentData().put(KEY, save());
    }
}
