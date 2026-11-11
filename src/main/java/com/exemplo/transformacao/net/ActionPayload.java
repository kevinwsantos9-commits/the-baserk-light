package com.exemplo.transformacao.net;

import com.exemplo.transformacao.TransformacaoMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente -> servidor: apertou o botão de transformar (TOGGLE) ou gastou um ponto (SPEND, arg = status). */
public record ActionPayload(int action, int arg) implements CustomPacketPayload {
    public static final int TOGGLE = 0;
    public static final int SPEND = 1;

    public static final Type<ActionPayload> TYPE = new Type<>(TransformacaoMod.id("action"));
    public static final StreamCodec<ByteBuf, ActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ActionPayload::action,
            ByteBufCodecs.VAR_INT, ActionPayload::arg,
            ActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
