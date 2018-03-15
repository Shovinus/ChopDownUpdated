package com.shovinus.chopdownupdated.command;

import java.util.List;

import com.google.common.collect.Lists;
import com.shovinus.chopdownupdated.config.Config;
import com.shovinus.chopdownupdated.config.PersonalConfig;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class CDUCommand  extends CommandBase
{

	public static int blockCount = 0;
    public static int itemCount = 0;
    public static int entityCount = 0;
    public static int biomeCount = 0;
    
    @Override
    public String getCommandName()
    {
        return "chopdownupdated";
    }
    
    @Override
    public List<String> getCommandAliases()
    {
        return Lists.newArrayList("cdu");
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "cdu makeglass\\drop\\showblockname";
    }
    
    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
    	World world = sender.getEntityWorld();
    	if (world.isRemote) 
        { 
            return;
        } 
        if (args.length < 1)
        {
            throw new WrongUsageException("commands.chopdownupdated.usage");
        }
        setBool(sender, args);        
    }
    
    private void setBool(ICommandSender sender, String[] args) throws CommandException
    {
    	String action = args[0];
        
        if (args.length < 2)
        {
            throw new WrongUsageException("commands.chopdownupdated." + action + ".usage");
        }     
        
        boolean value = parseBoolean(args[1]);
        
        if(!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
        	return;
        }
        
        PersonalConfig playerConfig = Config.getPlayerConfig(((EntityPlayerMP)sender.getCommandSenderEntity()).getUniqueID());
        if(action.equals("makeGlass")){
            playerConfig.makeGlass= value;
        }else if(action.equals("dontDrop")){
            playerConfig.dontDrop= value;
        }else if(action.equals("showBlockName")){
            playerConfig.showBlockName= value;
        } else {
        	return;
        }
        
        sender.addChatMessage(new TextComponentTranslation("commands.chopdownupdated." + action + ".success", args[1]));
    }   

    @Override
    public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos)
    {
        if (args.length == 1)
        {
            return getListOfStringsMatchingLastWord(args, "makeGlass", "dontDrop", "showBlockName");
        }
        if (args.length == 2) 
        {
        	return getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return null;
    }
}
