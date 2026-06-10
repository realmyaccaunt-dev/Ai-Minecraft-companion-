package com.realmyaccount.aicompanion.event;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import com.realmyaccount.aicompanion.entity.EntityAICompanion;

public class CompanionEventHandler
{
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (event.getEntity() instanceof EntityAICompanion)
        {
            EntityAICompanion companion = (EntityAICompanion) event.getEntity();
            companion.setCompanionName("AI Companion");
        }
    }
}
