package com.hp.jei_gateways.client;

import com.hp.jei_gateways.JeiGateways;
import com.hp.jei_gateways.gateway.GatewayEntityCache;
import com.hp.jei_gateways.gateway.GatewayLootCache;
import com.hp.jei_gateways.jei.JeiGatewaysPlugin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = JeiGateways.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        GatewayEntityCache.rebuild();
        GatewayLootCache.rebuild();
        JeiGatewaysPlugin.refreshRuntimeRecipes();
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) {
            GatewayEntityCache.rebuild();
            GatewayLootCache.rebuild();
            JeiGatewaysPlugin.refreshRuntimeRecipes();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        GatewayEntityCache.clear();
        GatewayLootCache.clear();
    }
}
