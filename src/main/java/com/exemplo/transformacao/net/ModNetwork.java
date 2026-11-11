package com.exemplo.transformacao.net;

import com.exemplo.transformacao.client.ClientPayloadHandlers;
import com.exemplo.transformacao.server.TransformationService;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ActionPayload.TYPE, ActionPayload.CODEC, TransformationService::handleAction);
        registrar.playToClient(SelfDataPayload.TYPE, SelfDataPayload.CODEC, ClientPayloadHandlers::handleSelf);
        registrar.playToClient(StatePayload.TYPE, StatePayload.CODEC, ClientPayloadHandlers::handleState);
    }
}
