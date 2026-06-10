package com.realmyaccount.aicompanion;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;
import com.realmyaccount.aicompanion.event.CompanionEventHandler;
import com.realmyaccount.aicompanion.command.CommandCompanion;

@Mod(modid = AICompanionMod.MODID, name = AICompanionMod.NAME, version = AICompanionMod.VERSION)
public class AICompanionMod
{
    public static final String MODID = "aicompanion";
    public static final String NAME = "AI Companion";
    public static final String VERSION = "1.0.0";

    private static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        logger.info("AI Companion Pre-Init starting...");
        MinecraftForge.EVENT_BUS.register(new CompanionEventHandler());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        logger.info("AI Companion Init starting...");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        logger.info("AI Companion Post-Init complete!");
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandCompanion());
        logger.info("AI Companion commands registered!");
    }
}
