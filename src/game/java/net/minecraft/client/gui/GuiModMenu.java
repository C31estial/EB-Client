package net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.resources.I18n;

public class GuiModMenu extends GuiScreen {
	private GuiScreen parentScreen;
	private final List<ModToggle> modToggles = new ArrayList<>();

	private static class ModToggle {
		public final String name;
		public boolean enabled;

		public ModToggle(String name, boolean enabled) {
			this.name = name;
			this.enabled = enabled;
		}
	}

	public GuiModMenu(GuiScreen parentScreenIn) {
		this.parentScreen = parentScreenIn;
		initializeModToggles();
	}

	private void initializeModToggles() {
		modToggles.add(new ModToggle("Example Mod", true));
		modToggles.add(new ModToggle("Test Feature", false));
		modToggles.add(new ModToggle("Debug Mode", false));
		modToggles.add(new ModToggle("Performance Tweaks", true));
	}

	@Override
	public void initGui() {
		this.buttonList.clear();

		for (int i = 0; i < modToggles.size(); i++) {
			ModToggle toggle = modToggles.get(i);
			String buttonText = toggle.name + ": " + (toggle.enabled ? I18n.format("options.on") : I18n.format("options.off"));
			this.buttonList.add(new GuiButton(i, this.width / 2 - 100, 60 + i * 25, 200, 20, buttonText));
		}

		this.buttonList.add(new GuiButton(100, this.width / 2 - 100, this.height - 50, 200, 20, I18n.format("gui.done")));
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 100) {
			this.mc.displayGuiScreen(this.parentScreen);
		} else if (button.id >= 0 && button.id < modToggles.size()) {
			ModToggle toggle = modToggles.get(button.id);
			toggle.enabled = !toggle.enabled;
			String buttonText = toggle.name + ": " + (toggle.enabled ? I18n.format("options.on") : I18n.format("options.off"));
			button.displayString = buttonText;
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRendererObj, "Mod Menu", this.width / 2, 20, 16777215);
		this.drawCenteredString(this.fontRendererObj, "Toggle mods on/off", this.width / 2, 35, 8421504);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) {
		if (keyCode == 1 || keyCode == this.mc.gameSettings.keyBindModMenu.getKeyCode()) {
			this.mc.displayGuiScreen(this.parentScreen);
		} else {
			super.keyTyped(typedChar, keyCode);
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return true;
	}
}