package com.realmyaccount.aicompanion.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class WorldStateObserver
{
    private EntityLivingBase companion;
    private World world;

    public WorldStateObserver(EntityLivingBase companion, World world)
    {
        this.companion = companion;
        this.world = world;
    }

    public String observeWorld()
    {
        StringBuilder state = new StringBuilder();

        state.append("Companion Position: ").append(String.format("X:%.1f Y:%.1f Z:%.1f\n",
                companion.posX, companion.posY, companion.posZ));

        state.append("Health: ").append(String.format("%.1f/20\n", companion.getHealth()));

        state.append("Nearby Resources:\n");
        state.append(getNearbyBlocksInfo());

        state.append("\nNearby Entities:\n");
        state.append(getNearbyEntitiesInfo());

        int lightLevel = world.getLight(new BlockPos(companion.posX, companion.posY, companion.posZ));
        state.append("\nLight Level: ").append(lightLevel);
        if (lightLevel < 8) state.append(" (DARK - DANGER!)");

        long time = world.getWorldTime() % 24000;
        state.append("\nTime: ");
        if (time < 6000) state.append("DAY");
        else if (time < 12000) state.append("AFTERNOON");
        else if (time < 18000) state.append("NIGHT");
        else state.append("EARLY MORNING");

        return state.toString();
    }

    private String getNearbyBlocksInfo()
    {
        StringBuilder info = new StringBuilder();
        BlockPos companionPos = new BlockPos(companion.posX, companion.posY, companion.posZ);
        int range = 5;

        List<String> resources = new ArrayList<>();

        for (int x = companionPos.getX() - range; x <= companionPos.getX() + range; x++)
        {
            for (int y = companionPos.getY() - 2; y <= companionPos.getY() + 3; y++)
            {
                for (int z = companionPos.getZ() - range; z <= companionPos.getZ() + range; z++)
                {
                    BlockPos pos = new BlockPos(x, y, z);
                    IBlockState state = world.getBlockState(pos);
                    Block block = state.getBlock();

                    if (block == Blocks.COAL_ORE) resources.add("Coal Ore");
                    else if (block == Blocks.IRON_ORE) resources.add("Iron Ore");
                    else if (block == Blocks.GOLD_ORE) resources.add("Gold Ore");
                    else if (block == Blocks.DIAMOND_ORE) resources.add("Diamond Ore");
                    else if (block == Blocks.LOG || block == Blocks.LOG2) resources.add("Wood");
                    else if (block == Blocks.STONE) resources.add("Stone");
                    else if (block == Blocks.LAVA) resources.add("LAVA (Danger!)");
                    else if (block == Blocks.WATER) resources.add("Water");
                }
            }
        }

        if (resources.isEmpty())
        {
            info.append("No valuable resources nearby\n");
        }
        else
        {
            for (String resource : resources)
            {
                info.append("- ").append(resource).append("\n");
            }
        }

        return info.toString();
    }

    private String getNearbyEntitiesInfo()
    {
        StringBuilder info = new StringBuilder();
        List<Entity> nearby = world.getEntitiesWithinAABBExcludingEntity(
                companion,
                companion.getEntityBoundingBox().expand(15, 15, 15)
        );

        int hostiles = 0, friendlies = 0, players = 0;

        for (Entity entity : nearby)
        {
            if (entity instanceof EntityMob) hostiles++;
            else if (entity instanceof EntityAnimal) friendlies++;
            else if (entity instanceof EntityPlayer) players++;
        }

        info.append("- Players: ").append(players).append("\n");
        info.append("- Hostile Mobs: ").append(hostiles);
        if (hostiles > 0) info.append(" (DANGER!)");
        info.append("\n");
        info.append("- Friendly Animals: ").append(friendlies).append("\n");

        return info.toString();
    }
}
