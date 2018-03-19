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
	
	@Override
	public Class<? extends GuiScreen> mainConfigGuiClass() {
		return GuiConfigChopDown.class;
	}

	@Nullable
	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}

	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
		return null;
	}
}