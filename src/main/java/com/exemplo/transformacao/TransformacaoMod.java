package com.exemplo.transformacao;

import com.exemplo.transformacao.net.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(TransformacaoMod.MODID)
public class TransformacaoMod {
    public static final String MODID = "transformacao";

    public TransformacaoMod(IEventBus modBus) {
        modBus.addListener(ModNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
