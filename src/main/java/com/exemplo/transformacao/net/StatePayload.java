package com.exemplo.transformacao.net;

import com.exemplo.transformacao.TransformacaoMod;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor -> todos que veem o jogador: ele está transformado? está rugindo agora? */
public record StatePayload(UUID player, boolean transformed, boolean roaring) implements CustomPacketPayload {
    public static final Type<StatePayload> TYPE = new Type<>(TransformacaoMod.id("state"));
    public static final StreamCodec<ByteBuf, StatePayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, StatePayload::player,
            ByteBufCodecs.BOOL, StatePayload::transformed,
            ByteBufCodecs.BOOL, StatePayload::roaring,
            StatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
