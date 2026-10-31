package com.hp.jei_gateways.jei;

import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;

final class GatewayPanelWidget implements IRecipeWidget {
    private final ScreenPosition position;
    private final int width;
    private final int height;

    GatewayPanelWidget(int x, int y, int width, int height) {
        this.position = new ScreenPosition(x, y);
        this.width = width;
        this.height = height;
    }

    @Override
    public ScreenPosition getPosition() {
        return this.position;
    }

    @Override
    public void drawWidget(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        GatewayEntityCategory.drawPanel(guiGraphics, 0, 0, this.width, this.height);
    }
}
