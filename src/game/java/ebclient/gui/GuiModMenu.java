package ebclient.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class GuiModMenu extends GuiScreen {
	private GuiScreen parentScreen;
	private final List<ModToggle> modToggles = new ArrayList<>();
	private final List<ModCard> modCards = new ArrayList<>();

	// Grid properties
	private static final int GRID_SIZE = 80; // Base grid cell size
	private static final int GRID_SPACING = 10;
	private static final int MARGIN = 40;

	// Dragging state
	private ModCard draggedCard = null;
	private int dragOffsetX = 0;
	private int dragOffsetY = 0;
	private int initialMouseX = 0;
	private int initialMouseY = 0;
	private boolean hasDragged = false;

	// Persistence - static so it persists across menu opens/closes
	private static final Map<String, CardConfig> savedConfigs = new HashMap<>();

	private static class CardConfig {
		public final int gridX, gridY;
		public final CardSize cardSize;

		public CardConfig(int gridX, int gridY, CardSize cardSize) {
			this.gridX = gridX;
			this.gridY = gridY;
			this.cardSize = cardSize;
		}
	}

	private static class ModToggle {
		public final String name;
		public final String description;
		public boolean enabled;

		public ModToggle(String name, String description, boolean enabled) {
			this.name = name;
			this.description = description;
			this.enabled = enabled;
		}
	}

	public enum CardSize {
		SMALL(40, 1, 1),      // 1/4 size, takes 0.5x0.5 grid cells
		NORMAL(80, 1, 1),     // Normal size, takes 1x1 grid cells
		MEDIUM(160, 2, 1),    // Medium rectangle, takes 2x1 grid cells
		LARGE(160, 2, 2);     // Large rectangle, takes 2x2 grid cells

		public final int pixelSize;
		public final int gridWidth;
		public final int gridHeight;

		CardSize(int pixelSize, int gridWidth, int gridHeight) {
			this.pixelSize = pixelSize;
			this.gridWidth = gridWidth;
			this.gridHeight = gridHeight;
		}

		public CardSize next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	private static class ModCard {
		public int gridX, gridY; // Grid position
		public CardSize cardSize;
		public final ModToggle toggle;
		public boolean isHovered;
		public boolean resizeButtonHovered;
		public boolean isDragging;

		public ModCard(int gridX, int gridY, CardSize cardSize, ModToggle toggle) {
			this.gridX = gridX;
			this.gridY = gridY;
			this.cardSize = cardSize;
			this.toggle = toggle;
		}

		public int getPixelX() {
			return MARGIN + gridX * (GRID_SIZE + GRID_SPACING);
		}

		public int getPixelY() {
			return MARGIN + 30 + gridY * (GRID_SIZE + GRID_SPACING); // +30 for title bar
		}

		public int getWidth() {
			return cardSize.gridWidth * GRID_SIZE + (cardSize.gridWidth - 1) * GRID_SPACING;
		}

		public int getHeight() {
			return cardSize.gridHeight * GRID_SIZE + (cardSize.gridHeight - 1) * GRID_SPACING;
		}

		public boolean isMouseOver(int mouseX, int mouseY) {
			return mouseX >= getPixelX() && mouseX <= getPixelX() + getWidth() &&
				   mouseY >= getPixelY() && mouseY <= getPixelY() + getHeight();
		}

		public boolean isResizeButtonOver(int mouseX, int mouseY) {
			int buttonX = getPixelX() + getWidth() - 15;
			int buttonY = getPixelY() + 3;
			return mouseX >= buttonX && mouseX <= buttonX + 12 && mouseY >= buttonY && mouseY <= buttonY + 12;
		}

		public boolean isSettingsButtonOver(int mouseX, int mouseY) {
			if (cardSize == CardSize.SMALL) return false; // No settings button on small cards
			int buttonX = getPixelX() + getWidth() - 30;
			int buttonY = getPixelY() + 3;
			return mouseX >= buttonX && mouseX <= buttonX + 12 && mouseY >= buttonY && mouseY <= buttonY + 12;
		}
	}

	public GuiModMenu(GuiScreen parentScreenIn) {
		this.parentScreen = parentScreenIn;
		initializeModToggles();
	}

	private void initializeModToggles() {
		modToggles.add(new ModToggle("Sprint", "", true));
		modToggles.add(new ModToggle("Bright", "", false));
		modToggles.add(new ModToggle("Speed", "", false));
		modToggles.add(new ModToggle("X-Ray", "", false));
		modToggles.add(new ModToggle("Fly", "", false));
		modToggles.add(new ModToggle("NoFall", "", false));
		modToggles.add(new ModToggle("AutoClicker", "", false));
		modToggles.add(new ModToggle("Reach", "", false));
	}

	@Override
	public void initGui() {
		this.buttonList.clear();
		if (this.modCards.isEmpty()) {
			// Initialize cards - use saved configs or default positions
			for (int i = 0; i < modToggles.size(); i++) {
				ModToggle toggle = modToggles.get(i);
				CardConfig saved = savedConfigs.get(toggle.name);

				int gridX, gridY;
				CardSize cardSize;

				if (saved != null) {
					gridX = saved.gridX;
					gridY = saved.gridY;
					cardSize = saved.cardSize;
				} else {
					// Default grid pattern
					gridX = i % 4; // 4 columns
					gridY = i / 4; // Rows as needed
					cardSize = CardSize.NORMAL;
				}

				modCards.add(new ModCard(gridX, gridY, cardSize, toggle));
			}
		}

		// Transparent X close button - we'll draw it manually
	}

	private void saveCardConfigs() {
		for (ModCard card : modCards) {
			savedConfigs.put(card.toggle.name, new CardConfig(card.gridX, card.gridY, card.cardSize));
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		// No buttons to handle now
	}

	private boolean isCloseButtonOver(int mouseX, int mouseY) {
		int closeX = this.width - 25;
		int closeY = 5;
		return mouseX >= closeX && mouseX <= closeX + 20 && mouseY >= closeY && mouseY <= closeY + 20;
	}

	private void pushOtherCards(ModCard resizedCard) {
		// Find all cards that would collide with the resized card
		boolean anyMoved = false;
		for (ModCard otherCard : modCards) {
			if (otherCard == resizedCard) continue;

			if (wouldCollide(resizedCard, otherCard)) {
				// Find the closest available position
				int[] newPos = findNearestFreePosition(otherCard);
				otherCard.gridX = newPos[0];
				otherCard.gridY = newPos[1];
				anyMoved = true;
			}
		}

		if (anyMoved) {
			saveCardConfigs(); // Save after pushing cards
		}
	}

	private boolean wouldCollide(ModCard card1, ModCard card2) {
		int card1EndX = card1.gridX + card1.cardSize.gridWidth - 1;
		int card1EndY = card1.gridY + card1.cardSize.gridHeight - 1;
		int card2EndX = card2.gridX + card2.cardSize.gridWidth - 1;
		int card2EndY = card2.gridY + card2.cardSize.gridHeight - 1;

		return !(card1.gridX > card2EndX || card1EndX < card2.gridX ||
				 card1.gridY > card2EndY || card1EndY < card2.gridY);
	}

	private int[] findNearestFreePosition(ModCard card) {
		int maxDistance = 20;

		for (int distance = 1; distance <= maxDistance; distance++) {
			for (int dx = -distance; dx <= distance; dx++) {
				for (int dy = -distance; dy <= distance; dy++) {
					if (Math.abs(dx) != distance && Math.abs(dy) != distance) continue;

					int newX = Math.max(0, card.gridX + dx);
					int newY = Math.max(0, card.gridY + dy);

					if (!isGridSpaceOccupied(newX, newY, card.cardSize, card)) {
						return new int[]{newX, newY};
					}
				}
			}
		}

		// Fallback: find any free position
		for (int y = 0; y < 20; y++) {
			for (int x = 0; x < 20; x++) {
				if (!isGridSpaceOccupied(x, y, card.cardSize, card)) {
					return new int[]{x, y};
				}
			}
		}

		return new int[]{card.gridX, card.gridY}; // No free space found
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		super.mouseClicked(mouseX, mouseY, mouseButton);

		if (mouseButton == 0) { // Left click
			// Check close button
			if (isCloseButtonOver(mouseX, mouseY)) {
				this.mc.displayGuiScreen(this.parentScreen);
				return;
			}

			for (ModCard card : modCards) {
				if (card.isResizeButtonOver(mouseX, mouseY)) {
					// Resize card and push others
					card.cardSize = card.cardSize.next();
					pushOtherCards(card);
					saveCardConfigs(); // Save after resize
					return;
				} else if (card.isSettingsButtonOver(mouseX, mouseY)) {
					// Settings button clicked - placeholder for now
					return;
				} else if (card.isMouseOver(mouseX, mouseY)) {
					// Prepare for potential drag or click
					draggedCard = card;
					dragOffsetX = mouseX - card.getPixelX();
					dragOffsetY = mouseY - card.getPixelY();
					initialMouseX = mouseX;
					initialMouseY = mouseY;
					hasDragged = false;
					card.isDragging = false; // Don't start dragging immediately
					return;
				}
			}
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);

		if (draggedCard != null) {
			if (hasDragged && draggedCard.isDragging) {
				// Snap to grid
				int newGridX = Math.max(0, (mouseX - dragOffsetX - MARGIN + (GRID_SIZE + GRID_SPACING) / 2) / (GRID_SIZE + GRID_SPACING));
				int newGridY = Math.max(0, (mouseY - dragOffsetY - MARGIN - 30 + (GRID_SIZE + GRID_SPACING) / 2) / (GRID_SIZE + GRID_SPACING));

				// Check for collisions with other cards
				if (!isGridSpaceOccupied(newGridX, newGridY, draggedCard.cardSize, draggedCard)) {
					draggedCard.gridX = newGridX;
					draggedCard.gridY = newGridY;
					saveCardConfigs(); // Save after drag
				}

				draggedCard.isDragging = false;
			} else {
				// If not dragged significantly, toggle the mod
				draggedCard.toggle.enabled = !draggedCard.toggle.enabled;
			}
			draggedCard = null;
			hasDragged = false;
		}
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

		if (draggedCard != null) {
			// Check if we've moved far enough to start dragging (5 pixel threshold)
			int dragDistance = Math.abs(mouseX - initialMouseX) + Math.abs(mouseY - initialMouseY);
			if (dragDistance > 5 && !hasDragged) {
				hasDragged = true;
				draggedCard.isDragging = true;
			}
		}
	}

	private boolean isGridSpaceOccupied(int gridX, int gridY, CardSize size, ModCard excludeCard) {
		for (ModCard card : modCards) {
			if (card == excludeCard) continue;

			// Check if the new position would overlap with this card
			int cardEndX = card.gridX + card.cardSize.gridWidth - 1;
			int cardEndY = card.gridY + card.cardSize.gridHeight - 1;
			int newEndX = gridX + size.gridWidth - 1;
			int newEndY = gridY + size.gridHeight - 1;

			if (!(gridX > cardEndX || newEndX < card.gridX || gridY > cardEndY || newEndY < card.gridY)) {
				return true; // Collision detected
			}
		}
		return false;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		// Fullscreen dark background
		this.drawGradientRect(0, 0, this.width, this.height, 0xCC000000, 0xDD111111);

		// Title bar
		Gui.drawRect(0, 0, this.width, 30, 0xFF1a1a1a);
		this.drawString(this.fontRendererObj, "EB CLIENT", 10, 9, 0xFF00AAFF);
		this.drawString(this.fontRendererObj, "Mod Menu", 10, 19, 0xFFAAAAAA);

		// Draw transparent X close button
		int closeX = this.width - 25;
		int closeY = 5;
		boolean closeHovered = isCloseButtonOver(mouseX, mouseY);

		// Close button background (subtle)
		if (closeHovered) {
			Gui.drawRect(closeX, closeY, closeX + 20, closeY + 20, 0x33FFFFFF);
		}

		// Draw X
		int xColor = closeHovered ? 0xFFFFFFFF : 0xFFAAAAAA;
		this.drawString(this.fontRendererObj, "X", closeX + 7, closeY + 6, xColor);

		// Update hover states
		for (ModCard card : modCards) {
			if (card != draggedCard || !card.isDragging) {
				card.isHovered = card.isMouseOver(mouseX, mouseY);
				card.resizeButtonHovered = card.isResizeButtonOver(mouseX, mouseY);
			}
		}

		// Draw mod cards (non-dragged first)
		for (ModCard card : modCards) {
			if (card != draggedCard) {
				drawModCard(card, mouseX, mouseY, false);
			}
		}

		// Draw dragged card last (on top)
		if (draggedCard != null) {
			drawModCard(draggedCard, mouseX, mouseY, true);
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}


	private void drawModCard(ModCard card, int mouseX, int mouseY, boolean isDragging) {
		ModToggle toggle = card.toggle;

		// Calculate position (for dragging)
		int cardX, cardY;
		if (isDragging && card.isDragging) {
			cardX = mouseX - dragOffsetX;
			cardY = mouseY - dragOffsetY;
		} else {
			cardX = card.getPixelX();
			cardY = card.getPixelY();
		}

		int cardWidth = card.getWidth();
		int cardHeight = card.getHeight();

		// Card background with opacity if dragging
		int cardColor = card.isHovered ? 0xFF363636 : 0xFF2e2e2e;
		int borderColor = toggle.enabled ? 0xFF00AA00 : 0xFF666666;

		if (isDragging && card.isDragging) {
			cardColor = (cardColor & 0x00FFFFFF) | 0xCC000000; // Make semi-transparent
			borderColor = (borderColor & 0x00FFFFFF) | 0xCC000000;
		}

		// Draw card with border
		Gui.drawRect(cardX - 1, cardY - 1, cardX + cardWidth + 1, cardY + cardHeight + 1, borderColor);
		Gui.drawRect(cardX, cardY, cardX + cardWidth, cardY + cardHeight, cardColor);

		// Resize button in top-right corner
		int resizeButtonX = cardX + cardWidth - 15;
		int resizeButtonY = cardY + 3;
		int resizeButtonColor = card.resizeButtonHovered ? 0xFF555555 : 0xFF444444;
		Gui.drawRect(resizeButtonX, resizeButtonY, resizeButtonX + 12, resizeButtonY + 12, resizeButtonColor);

		// Resize icon
		this.drawString(this.fontRendererObj, "↕", resizeButtonX + 3, resizeButtonY + 2, 0xFFAAAAAA);

		// Settings button (only for non-small cards)
		if (card.cardSize != CardSize.SMALL) {
			int settingsButtonX = cardX + cardWidth - 30;
			int settingsButtonY = cardY + 3;
			int settingsButtonColor = card.isSettingsButtonOver(mouseX, mouseY) ? 0xFF555555 : 0xFF444444;
			Gui.drawRect(settingsButtonX, settingsButtonY, settingsButtonX + 12, settingsButtonY + 12, settingsButtonColor);

			// Settings icon (gear symbol)
			this.drawString(this.fontRendererObj, "⚙", settingsButtonX + 2, settingsButtonY + 2, 0xFFAAAAAA);
		}

		// Mod name (centered horizontally, smaller text)
		String name = toggle.name;

		// Calculate vertical center position for the text
		int textY;
		if (card.cardSize == CardSize.SMALL) {
			textY = cardY + (cardHeight - 16) / 2 - 8; // Center vertically, leaving room for status bar
		} else {
			textY = cardY + (cardHeight - 20) / 2 - 8; // Center vertically, leaving room for status bar
		}

		// Draw centered text with smaller, cleaner appearance
		int nameWidth = this.fontRendererObj.getStringWidth(name);
		int nameX = cardX + (cardWidth - nameWidth) / 2;
		this.drawString(this.fontRendererObj, name, nameX, textY, 0xFFFFFFFF);

		// Combined status indicator at bottom
		int statusBarHeight = card.cardSize == CardSize.SMALL ? 12 : 16;
		int statusY = cardY + cardHeight - statusBarHeight;

		// Status background
		int statusBgColor = toggle.enabled ? 0xFF004400 : 0xFF440000;
		Gui.drawRect(cardX, statusY, cardX + cardWidth, cardY + cardHeight, statusBgColor);

		// Status text (centered, smaller font)
		String statusText = toggle.enabled ? "ON" : "OFF";
		if (card.cardSize == CardSize.MEDIUM || card.cardSize == CardSize.LARGE) {
			statusText = toggle.enabled ? "ENABLED" : "DISABLED";
		}
		int statusTextColor = toggle.enabled ? 0xFF00FF00 : 0xFFFF4444;
		int statusTextWidth = this.fontRendererObj.getStringWidth(statusText);
		int statusTextX = cardX + (cardWidth - statusTextWidth) / 2;
		int statusTextY = statusY + (statusBarHeight - 7) / 2;
		this.drawString(this.fontRendererObj, statusText, statusTextX, statusTextY, statusTextColor);

		// Hover effect
		if (card.isHovered && !card.resizeButtonHovered && !isDragging) {
			this.drawGradientRect(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0x20FFFFFF, 0x10FFFFFF);
		}

		// Dragging effect
		if (isDragging && card.isDragging) {
			this.drawGradientRect(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0x40FFFFFF, 0x20FFFFFF);
		}
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