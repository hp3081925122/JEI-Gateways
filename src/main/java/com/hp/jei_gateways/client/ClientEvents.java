package com.hp.jei_gateways.client;

import com.hp.jei_gateways.JeiGateways;
import com.hp.jei_gateways.gateway.GatewayEntityCache;
import com.hp.jei_gateways.gateway.GatewayLootCache;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JeiGateways.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        GatewayEntityCache.rebuild();
        GatewayLootCache.rebuild();
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) {
            GatewayEntityCache.rebuild();
            GatewayLootCache.rebuild();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        GatewayEntityCache.clear();
        GatewayLootCache.clear();
    }
}
