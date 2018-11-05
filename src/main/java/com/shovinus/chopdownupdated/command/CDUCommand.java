package com.shovinus.chopdownupdated.command;

import java.util.List;

import com.google.common.collect.Lists;
import com.shovinus.chopdownupdated.config.Config;
import com.shovinus.chopdownupdated.config.PersonalConfig;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class CDUCommand extends CommandBase {

	@Override
	public String getName() {
		return "chopdownupdated";
	}

	@Override
	public List<String> getAliases() {
		return Lists.newArrayList("cdu");
	}

	@Override
	public String getUsage(ICommandSender sender) {
		if(sender == null || !(sender instanceof EntityPlayer || sender instanceof EntityPlayerMP)) {
			return "";
		}
		return "cdu makeGlass/dontDrop/showBlockName/lowerLogs/breakLeaves[true/false]\n"
				+ stateProperty(sender, "makeGlass") + ": Turn felled trees in to "
				+ "glass to view what we think a tree should look like\n" + 
				stateProperty(sender, "dontDrop")
				+ ": The blocks dont fall, " + "but are placed in the air where we think they should go (when combined "
				+ "with breakGlass it just turns the tree in to glass)\n" + 
				stateProperty(sender, "showBlockName")
				+ ": Any time you click on a log or leaf it will show the name "
				+ "as accepted by the trees configuration so if you come across a missing tree "
				+ "you can add it to your configuration (And hopefully report it please!)\n"
				+ stateProperty(sender, "breakLeaves")
				+ ": Leaves are all broken and do their drops only logs fall\n";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		World world = sender.getEntityWorld();
		if (world.isRemote) {
			return;
		}
		if (args.length < 1) {
			throw new WrongUsageException(getUsage(sender));
		}
		setBool(sender, args);
	}

	private boolean getValue(int setter, boolean previous) {
		switch (setter) {
		case 0:
			return false;
		case 1:
			return true;
		default:
			return !previous;
		}
	}

	private void setBool(ICommandSender sender, String[] args) throws CommandException {
		String action = args[0];
		int value = 2;
		if (args.length > 1) {
			boolean desiredValue = parseBoolean(args[1]);
			if (desiredValue) {
				value = 1;
			} else {
				value = 0;
			}
		}

		if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
			return;
		}

		PersonalConfig playerConfig = Config
				.getPlayerConfig(((EntityPlayerMP) sender.getCommandSenderEntity()).getUniqueID());
		boolean setValue = true;
		if (action.equals("makeGlass")) {
			setValue = getValue(value, playerConfig.makeGlass);
			playerConfig.makeGlass = setValue;
		} else if (action.equals("dontDrop")) {
			setValue = getValue(value, playerConfig.dontFell);
			playerConfig.dontFell = setValue;
		} else if (action.equals("showBlockName")) {
			setValue = getValue(value, playerConfig.showBlockName);
			playerConfig.showBlockName = setValue;
		} else if (action.equals("breakLeaves")) {
			setValue = getValue(value,getMainConfigValue("breakLeaves"));
			setMainConfigValue("breakLeaves",setValue);
		} else {
			return;
		}

		sender.sendMessage(
				new TextComponentTranslation(action + (setValue ? " Enabled" : " Disabled")));
	}
	public boolean getMainConfigValue(String config){
		return Config.config.getCategory(Config.CATEGORY).get(config).getBoolean();
	}
	public void setMainConfigValue(String config,boolean value) {
		Config.config.getCategory(Config.CATEGORY).get(config).set(value);
		try {
		Config.reloadConfig();
		}catch(Exception ex){}
	}
	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
			BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, 
					 "makeGlass","dontDrop", "showBlockName","breakLeaves");

		}
		if (args.length == 2) {
			return getListOfStringsMatchingLastWord(args, "true", "false");
		}
		return null;
	}

	private PersonalConfig getPlayerConfig(ICommandSender sender) {
		return Config.getPlayerConfig(((EntityPlayerMP) sender.getCommandSenderEntity()).getUniqueID());
	}

	private String stateProperty(ICommandSender sender, String property) {
		if (property.equals("makeGlass")) {
			return (getPlayerConfig(sender).makeGlass ? "[x] " : "[ ] ") + "makeGlass";
		}
		if (property.equals("dontDrop")) {
			return (getPlayerConfig(sender).dontFell ? "[x] " : "[ ] ") + "dontDrop";
		}
		if (property.equals("showBlockName")) {
			return (getPlayerConfig(sender).showBlockName ? "[x] " : "[ ] ") + "showBlockName";
		}		
		if (property.equals("breakLeaves")) {
			return (getMainConfigValue("breakLeaves") ? "[x] " : "[ ] ") + "breakLeaves";
		}		
		return "";
	}
}
