package com.exemplo.transformacao.client;

import com.exemplo.transformacao.data.ArmorData;
import com.exemplo.transformacao.net.ActionPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Tela de status da armadura (tecla G): nível, barra de XP, pontos e distribuição de status. */
public class StatusScreen extends Screen {
    private static final int W = 230;
    private static final int H = 196;
    private static final String[] STAT_KEYS = {"str", "vit", "def", "agi"};

    private final List<Button> buttons = new ArrayList<>();
    private final Button[] plus = new Button[4];
    private Button transformButton;

    public StatusScreen() {
        super(Component.translatable("screen.transformacao.title"));
    }

    @Override
    protected void init() {
        buttons.clear();
        int left = (width - W) / 2;
        int top = (height - H) / 2;

        for (int i = 0; i < 4; i++) {
            final int stat = i;
            plus[i] = addRenderableWidget(Button.builder(Component.literal("+"),
                            b -> PacketDistributor.sendToServer(new ActionPayload(ActionPayload.SPEND, stat)))
                    .bounds(left + W - 36, top + 76 + i * 22, 20, 18).build());
            buttons.add(plus[i]);
        }

        transformButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                    PacketDistributor.sendToServer(new ActionPayload(ActionPayload.TOGGLE, 0));
                    onClose();
                })
                .bounds(left + 16, top + H - 34, W - 32, 20).build());
        buttons.add(transformButton);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Desenhamos tudo manualmente (sem super.render) para não depender do blur/background do Screen.
        ArmorData d = ClientState.self;
        int left = (width - W) / 2;
        int top = (height - H) / 2;

        g.fill(0, 0, width, height, 0xA0000000);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF5A1A12);
        g.fillGradient(left, top, left + W, top + H, 0xFF2A1414, 0xFF120808);

        g.drawCenteredString(font, title, left + W / 2, top + 9, 0xFFFF6A3D);

        Component levelText = d.level >= ArmorData.MAX_LEVEL
                ? Component.translatable("screen.transformacao.level_max", d.level)
                : Component.translatable("screen.transformacao.level", d.level);
        g.drawString(font, levelText, left + 16, top + 26, 0xFFFFFFFF);

        // barra de XP
        int bx = left + 16, by = top + 38, bw = W - 32, bh = 11;
        float frac = d.level >= ArmorData.MAX_LEVEL ? 1F : Math.min(1F, d.xp / (float) d.xpForNext());
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF000000);
        g.fill(bx, by, bx + bw, by + bh, 0xFF3A2A2A);
        if (frac > 0F) g.fillGradient(bx, by, bx + Math.round(bw * frac), by + bh, 0xFFFFB02E, 0xFFD6401A);
        String xpText = d.level >= ArmorData.MAX_LEVEL ? "MAX" : d.xp + " / " + d.xpForNext() + " XP";
        g.drawCenteredString(font, Component.literal(xpText), left + W / 2, by + 2, 0xFFFFFFFF);

        // pontos disponíveis
        g.drawString(font, Component.translatable("screen.transformacao.points", d.points),
                left + 16, top + 58, d.points > 0 ? 0xFFFFD34D : 0xFFAAAAAA);

        // status
        for (int i = 0; i < 4; i++) {
            int y = top + 76 + i * 22 + 5;
            g.drawString(font, Component.translatable("screen.transformacao.stat." + STAT_KEYS[i]), left + 16, y, 0xFFFFFFFF);
            String value = d.stats[i] + "/" + ArmorData.MAX_STAT + "  (" + bonus(i, d.stats[i]) + ")";
            g.drawString(font, Component.literal(value), left + 92, y, 0xFFBBBBBB);
            plus[i].active = d.points > 0 && d.stats[i] < ArmorData.MAX_STAT;
        }

        transformButton.setMessage(Component.translatable(
                d.transformed ? "screen.transformacao.untransform" : "screen.transformacao.transform"));

        g.drawCenteredString(font, Component.translatable("screen.transformacao.hint"),
                left + W / 2, top + H - 11, 0xFF887777);

        for (Button b : buttons) b.render(g, mouseX, mouseY, partialTick);
    }

    private static String bonus(int stat, int points) {
        return switch (stat) {
            case ArmorData.STR -> String.format(Locale.ROOT, "+%.1f", ArmorData.PER_STR_ATTACK * points);
            case ArmorData.VIT -> String.format(Locale.ROOT, "+%.0f", ArmorData.PER_VIT_HEALTH * points);
            case ArmorData.DEF -> String.format(Locale.ROOT, "+%.0f", ArmorData.PER_DEF_ARMOR * points);
            default -> String.format(Locale.ROOT, "+%.0f%%", ArmorData.PER_AGI_SPEED * 100.0 * points);
        };
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.STATUS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
