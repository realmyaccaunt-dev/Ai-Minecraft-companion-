package com.realmyaccount.aicompanion.entity;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.text.TextComponentString;
import com.realmyaccount.aicompanion.ai.WorldStateObserver;
import com.realmyaccount.aicompanion.ai.OpenAIManager;

public class EntityAICompanion extends EntityAnimal
{
    private static final DataParameter<String> COMPANION_NAME = EntityDataManager.createKey(EntityAICompanion.class, DataSerializers.STRING);
    private static final DataParameter<Integer> COMPANION_MODE = EntityDataManager.createKey(EntityAICompanion.class, DataSerializers.VARINT);
    private static final DataParameter<String> CURRENT_ACTION = EntityDataManager.createKey(EntityAICompanion.class, DataSerializers.STRING);

    private EntityPlayer owner;
    private int aiUpdateTick = 0;
    private int actionCooldown = 0;
    private String lastCommand = "";
    private WorldStateObserver worldObserver;
    private OpenAIManager aiManager;
    private String openAIKey = "";

    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean jumping = false;
    private boolean sprinting = false;
    private boolean crouching = false;

    private static final int MODE_IDLE = 0;
    private static final int MODE_FOLLOWING = 1;
    private static final int MODE_MINING = 2;
    private static final int MODE_BUILDING = 3;
    private static final int MODE_COMBAT = 4;
    private static final int MODE_AI_CONTROLLED = 5;

    public EntityAICompanion(World worldIn)
    {
        super(worldIn);
        this.setSize(0.6F, 1.8F);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        this.setHealth(20.0F);
        this.worldObserver = new WorldStateObserver(this, worldIn);
    }

    @Override
    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(COMPANION_NAME, "AI Companion");
        this.dataManager.register(COMPANION_MODE, MODE_IDLE);
        this.dataManager.register(CURRENT_ACTION, "idle");
    }

    @Override
    protected void initEntityAI()
    {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAILookIdle(this));
        this.tasks.addTask(2, new EntityAIWander(this, 0.8D));
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();

        if (!this.world.isRemote)
        {
            this.actionCooldown--;
            this.aiUpdateTick++;

            int mode = this.getCompanionMode();

            if (mode == MODE_AI_CONTROLLED && this.aiUpdateTick >= 20)
            {
                this.aiUpdateTick = 0;
                this.updateAIBehavior();
            }

            this.executeMovement();
            this.handleLooking();
        }
    }

    private void updateAIBehavior()
    {
        if (this.aiManager == null || this.openAIKey.isEmpty())
        {
            return;
        }

        try
        {
            String worldState = this.worldObserver.observeWorld();
            String response = this.aiManager.getAIResponse(worldState, this.lastCommand);
            this.parseAndExecuteAction(response);
            this.lastCommand = "";
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void parseAndExecuteAction(String actionString)
    {
        String action = actionString.toLowerCase().replaceAll("action:", "").trim();
        this.dataManager.set(CURRENT_ACTION, action);

        this.moveForward = false;
        this.moveBackward = false;
        this.moveLeft = false;
        this.moveRight = false;
        this.jumping = false;
        this.sprinting = false;
        this.crouching = false;

        if (action.startsWith("move_forward")) this.moveForward = true;
        else if (action.startsWith("move_backward")) this.moveBackward = true;
        else if (action.startsWith("move_left")) this.moveLeft = true;
        else if (action.startsWith("move_right")) this.moveRight = true;
        else if (action.startsWith("jump")) this.jumping = true;
        else if (action.startsWith("sprint")) this.sprinting = true;
        else if (action.startsWith("crouch")) this.crouching = true;
        else if (action.startsWith("look_up")) this.rotationPitch = Math.max(-90, this.rotationPitch - 5);
        else if (action.startsWith("look_down")) this.rotationPitch = Math.min(90, this.rotationPitch + 5);
        else if (action.startsWith("look_left")) this.rotationYaw += 5;
        else if (action.startsWith("look_right")) this.rotationYaw -= 5;
        else if (action.startsWith("mine")) this.performMining();
        else if (action.startsWith("attack")) this.performAttack();
        else if (action.startsWith("chat:")) this.performChat(action.substring(5));
    }

    private void executeMovement()
    {
        if (this.moveForward)
        {
            this.motionX += Math.cos(Math.toRadians(this.rotationYaw + 90)) * 0.1;
            this.motionZ += Math.sin(Math.toRadians(this.rotationYaw + 90)) * 0.1;
        }
        if (this.moveBackward)
        {
            this.motionX -= Math.cos(Math.toRadians(this.rotationYaw + 90)) * 0.1;
            this.motionZ -= Math.sin(Math.toRadians(this.rotationYaw + 90)) * 0.1;
        }
        if (this.moveLeft)
        {
            this.motionX += Math.cos(Math.toRadians(this.rotationYaw)) * 0.1;
            this.motionZ += Math.sin(Math.toRadians(this.rotationYaw)) * 0.1;
        }
        if (this.moveRight)
        {
            this.motionX -= Math.cos(Math.toRadians(this.rotationYaw)) * 0.1;
            this.motionZ -= Math.sin(Math.toRadians(this.rotationYaw)) * 0.1;
        }
        if (this.jumping && this.onGround)
        {
            this.motionY = 0.42F;
        }
        if (this.crouching)
        {
            this.setSneaking(true);
        }
        else
        {
            this.setSneaking(false);
        }
    }

    private void handleLooking()
    {
        if (this.owner != null && this.getCompanionMode() == MODE_FOLLOWING)
        {
            double dX = this.owner.posX - this.posX;
            double dY = this.owner.posY + this.owner.getEyeHeight() - (this.posY + this.getEyeHeight());
            double dZ = this.owner.posZ - this.posZ;

            double distance = Math.sqrt(dX * dX + dZ * dZ);
            this.rotationYaw = (float) Math.toDegrees(Math.atan2(dZ, dX)) - 90;
            this.rotationPitch = (float) -Math.toDegrees(Math.atan2(dY, distance));
        }
    }

    private void performMining()
    {
        BlockPos targetBlock = this.getPosition().offset(this.getHorizontalFacing());
        IBlockState state = this.world.getBlockState(targetBlock);
        if (state.getBlock() != Blocks.AIR)
        {
            this.world.destroyBlock(targetBlock, true);
        }
    }

    private void performAttack()
    {
        for (Object obj : this.world.loadedEntityList)
        {
            if (obj instanceof net.minecraft.entity.monster.EntityMob)
            {
                net.minecraft.entity.monster.EntityMob mob = (net.minecraft.entity.monster.EntityMob) obj;
                if (this.getDistanceSq(mob) < 16.0D)
                {
                    this.attackEntityAsMob(mob);
                    break;
                }
            }
        }
    }

    private void performChat(String message)
    {
        if (this.owner != null)
        {
            this.owner.sendMessage(new TextComponentString("§b[" + this.getCompanionName() + "]§f " + message));
        }
    }

    public void setOpenAIKey(String key)
    {
        this.openAIKey = key;
        this.aiManager = OpenAIManager.getInstance(key);
    }

    public void setOwner(EntityPlayer player)
    {
        this.owner = player;
    }

    public EntityPlayer getOwner()
    {
        return this.owner;
    }

    public void setCompanionMode(int mode)
    {
        this.dataManager.set(COMPANION_MODE, mode);
    }

    public int getCompanionMode()
    {
        return this.dataManager.get(COMPANION_MODE);
    }

    public void setCompanionName(String name)
    {
        this.dataManager.set(COMPANION_NAME, name);
    }

    public String getCompanionName()
    {
        return this.dataManager.get(COMPANION_NAME);
    }

    public void giveCommand(String command)
    {
        this.lastCommand = command;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        if (this.owner != null)
        {
            compound.setString("Owner", this.owner.getUniqueID().toString());
        }
        compound.setString("CompanionName", this.getCompanionName());
        compound.setString("OpenAIKey", this.openAIKey);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("CompanionName"))
        {
            this.setCompanionName(compound.getString("CompanionName"));
        }
        if (compound.hasKey("OpenAIKey"))
        {
            this.setOpenAIKey(compound.getString("OpenAIKey"));
        }
    }

    @Override
    public EntityAICompanion createChild(EntityAnimal agentb)
    {
        return null;
    }
}
