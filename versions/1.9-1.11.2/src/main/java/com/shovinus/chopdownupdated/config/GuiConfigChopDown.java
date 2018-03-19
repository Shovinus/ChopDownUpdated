package com.shovinus.chopdownupdated.config;

import com.shovinus.chopdownupdated.ChopDown;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;


public class GuiConfigChopDown extends GuiConfig {
	public GuiConfigChopDown(GuiScreen parentScreen) {
		super(parentScreen, 
				new ConfigElement(
						Config.config.getCategory(Config.CATEGORY))
				.getChildElements(),
				ChopDown.MODID,
				false,
				false, 
				"Configure Chop Down Updated");
		
	}

	
}