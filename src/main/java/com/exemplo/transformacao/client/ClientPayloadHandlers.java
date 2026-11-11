package com.exemplo.transformacao.client;

import com.exemplo.transformacao.net.SelfDataPayload;
import com.exemplo.transformacao.net.StatePayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Handlers de pacote que só rodam no cliente (mantidos numa classe separada para não carregar no servidor). */
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {}

    public static void handleSelf(SelfDataPayload payload, IPayloadContext ctx) {
        ClientState.setSelf(payload.tag());
    }

    public static void handleState(StatePayload payload, IPayloadContext ctx) {
        ClientState.setState(payload.player(), payload.transformed(), payload.roaring());
    }
}
