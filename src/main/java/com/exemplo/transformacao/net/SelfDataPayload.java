package com.exemplo.transformacao.net;

import com.exemplo.transformacao.TransformacaoMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor -> o próprio jogador: dados completos da armadura (para a tela de status). */
public record SelfDataPayload(CompoundTag tag) implements CustomPacketPayload {
    public static final Type<SelfDataPayload> TYPE = new Type<>(TransformacaoMod.id("self_data"));
    public static final StreamCodec<ByteBuf, SelfDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, SelfDataPayload::tag,
            SelfDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
