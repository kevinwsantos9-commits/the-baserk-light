package com.exemplo.transformacao.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private KeyBindings() {}

    /** R: transformar / destransformar (muda em Opções > Controles). */
    public static final KeyMapping TRANSFORM =
            new KeyMapping("key.transformacao.transform", GLFW.GLFW_KEY_R, "key.categories.transformacao");
    /** G: abre a tela de status da armadura. */
    public static final KeyMapping STATUS =
            new KeyMapping("key.transformacao.status", GLFW.GLFW_KEY_G, "key.categories.transformacao");
}
