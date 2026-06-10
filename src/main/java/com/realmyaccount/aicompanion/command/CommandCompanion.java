package com.realmyaccount.aicompanion.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import com.realmyaccount.aicompanion.entity.EntityAICompanion;

public class CommandCompanion extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "companion";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/companion <spawn|follow|ai|command|stop|status>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws WrongUsageException
    {
        if (!(sender instanceof EntityPlayer))
        {
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;

        if (args.length == 0)
        {
            throw new WrongUsageException(this.getCommandUsage(player));
        }

        String command = args[0].toLowerCase();

        switch(command)
        {
            case "spawn":
                handleSpawn(player);
                break;
            case "follow":
                handleFollow(player);
                break;
            case "ai":
                handleAI(player, args);
                break;
            case "command":
                handleCommand(player, args);
                break;
            case "stop":
                handleStop(player);
                break;
            case "status":
                handleStatus(player);
                break;
            default:
                throw new WrongUsageException(this.getCommandUsage(player));
        }
    }

    private void handleSpawn(EntityPlayer player)
    {
        EntityAICompanion companion = new EntityAICompanion(player.world);
        companion.setLocationAndAngles(player.posX + 2, player.posY, player.posZ + 2, 0, 0);
        companion.setOwner(player);
        companion.setCompanionName("AI Companion");
        player.world.spawnEntity(companion);
        player.sendMessage(new TextComponentString("§aAI Companion spawned!"));
    }

    private void handleFollow(EntityPlayer player)
    {
        EntityAICompanion companion = getCompanion(player);
        if (companion != null)
        {
            companion.setOwner(player);
            companion.setCompanionMode(1);
            player.sendMessage(new TextComponentString("§aCompanion is now following you!"));
        }
        else
        {
            player.sendMessage(new TextComponentString("§cNo companion found nearby"));
        }
    }

    private void handleAI(EntityPlayer player, String[] args)
    {
        EntityAICompanion companion = getCompanion(player);
        if (companion == null)
        {
            player.sendMessage(new TextComponentString("§cNo companion found nearby"));
            return;
        }

        if (args.length < 2)
        {
            throw new WrongUsageException("/companion ai <openai_key>");
        }

        String apiKey = args[1];
        companion.setOpenAIKey(apiKey);
        companion.setCompanionMode(5);
        player.sendMessage(new TextComponentString("§aAI Companion activated with OpenAI!"));
    }

    private void handleCommand(EntityPlayer player, String[] args)
    {
        EntityAICompanion companion = getCompanion(player);
        if (companion == null)
        {
            player.sendMessage(new TextComponentString("§cNo companion found nearby"));
            return;
        }

        StringBuilder command = new StringBuilder();
        for (int i = 1; i < args.length; i++)
        {
            command.append(args[i]).append(" ");
        }

        companion.giveCommand(command.toString());
        player.sendMessage(new TextComponentString("§aCommand sent to companion: " + command.toString()));
    }

    private void handleStop(EntityPlayer player)
    {
        EntityAICompanion companion = getCompanion(player);
        if (companion != null)
        {
            companion.setCompanionMode(0);
            player.sendMessage(new TextComponentString("§aCompanion stopped"));
        }
    }

    private void handleStatus(EntityPlayer player)
    {
        EntityAICompanion companion = getCompanion(player);
        if (companion != null)
        {
            int mode = companion.getCompanionMode();
            String modeStr = getModeString(mode);
            player.sendMessage(new TextComponentString("§b" + companion.getCompanionName() + " - Status: " + modeStr));
        }
        else
        {
            player.sendMessage(new TextComponentString("§cNo companion found nearby"));
        }
    }

    private EntityAICompanion getCompanion(EntityPlayer player)
    {
        for (Object obj : player.world.loadedEntityList)
        {
            if (obj instanceof EntityAICompanion)
            {
                EntityAICompanion companion = (EntityAICompanion) obj;
                if (player.getDistanceSq(companion) < 2500.0D)
                {
                    return companion;
                }
            }
        }
        return null;
    }

    private String getModeString(int mode)
    {
        switch(mode)
        {
            case 0: return "Idle";
            case 1: return "Following";
            case 2: return "Mining";
            case 3: return "Building";
            case 4: return "Combat";
            case 5: return "AI Controlled";
            default: return "Unknown";
        }
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }
}
