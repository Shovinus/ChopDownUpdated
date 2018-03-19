package com.shovinus.chopdownupdated.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import javax.annotation.Nullable;
import java.util.Set;

public class GuiConfigFactoryChopDown implements IModGuiFactory {
	@Override
	public void initialize(Minecraft minecraftInstance) {

	}	

	@Nullable
	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}

	@Override
	public GuiScreen createConfigGui(GuiScreen arg0) {
		// TODO Auto-generated method stub
		return new GuiConfigChopDown(arg0);
	}

	@Override
	public boolean hasConfigGui() {
		// TODO Auto-generated method stub
		return true;
	}


}