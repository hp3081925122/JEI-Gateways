package com.hp.jei_gateways;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(JeiGateways.MODID)
public class JeiGateways {
    public static final String MODID = "jei_gateways";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JeiGateways() {
        FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
    }
}
