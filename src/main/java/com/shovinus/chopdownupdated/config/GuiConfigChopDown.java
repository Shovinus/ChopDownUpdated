package com.shovinus.chopdownupdated.config;

import java.util.ArrayList;
import java.util.List;

import com.shovinus.chopdownupdated.ChopDown;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

public class GuiConfigChopDown extends GuiConfig {
	public GuiConfigChopDown(GuiScreen parentScreen) {
		super(parentScreen,
				getElements(), ChopDown.MODID, false, false, "Configure Chop Down Updated");

	}

	public static List<IConfigElement> getElements() {
		List<IConfigElement> sections = new ArrayList<IConfigElement>();
		sections.add(new ConfigElement(Config.config.getCategory(Config.CATEGORY)));
		sections.add(new ConfigElement(Config.config.getCategory(Config.MOD_CATEGORY)));
		return sections;
	}

}