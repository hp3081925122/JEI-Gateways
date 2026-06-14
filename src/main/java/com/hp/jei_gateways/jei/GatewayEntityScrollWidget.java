package com.hp.jei_gateways.jei;

import com.hp.jei_gateways.gateway.GatewayEntityRecipe;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Optional;

public class GatewayEntityScrollWidget implements ISlottedRecipeWidget, IJeiInputHandler {
    private static final int SCROLLBAR_EXTRA_WIDTH = 16;
    private static final int MIN_SCROLL_MARKER_HEIGHT = 14;
    private static final int SCROLLBAR_WIDTH = 14;
    private static final int BORDER_LIGHT = 0xFFF8F8F8;
    private static final int BORDER_DARK = 0xFF8A8A8A;
    private static final int PANEL_FILL = 0xFFE3E3E3;
    private static final int MARKER_FILL = 0xFFB6B6B6;

    private final GatewayEntityRecipe recipe;
    private final List<IRecipeSlotDrawable> eggSlots;
    private final ScreenPosition position;
    private final ScreenRectangle area;
    private final Rect2i scrollArea;
    private final Rect2i contentsArea;
    private final int contentHeight;
    private double dragOriginY = -1.0D;
    private float scrollOffsetY = 0.0F;

    public GatewayEntityScrollWidget(GatewayEntityRecipe recipe, int x, int y, int width, int height, List<IRecipeSlotDrawable> eggSlots) {
        this.recipe = recipe;
        this.eggSlots = eggSlots;
        this.position = new ScreenPosition(x, y);
        this.area = new ScreenRectangle(this.position, width, height);
        this.scrollArea = new Rect2i(width - SCROLLBAR_WIDTH, 0, SCROLLBAR_WIDTH, height);
        this.contentsArea = new Rect2i(0, 0, width - SCROLLBAR_EXTRA_WIDTH, height);
        this.contentHeight = GatewayEntityCategory.getContentHeight(recipe);
    }

    @Override
    public ScreenPosition getPosition() {
        return this.position;
    }

    @Override
    public ScreenRectangle getArea() {
        return this.area;
    }

    @Override
    public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        drawPanel(guiGraphics, this.scrollArea);
        Rect2i scrollbarMarkerArea = this.calculateScrollbarMarkerArea();
        drawMarker(guiGraphics, scrollbarMarkerArea);

        PoseStack poseStack = guiGraphics.pose();
        var pose = poseStack.last().pose();
        int absoluteX = Math.round(pose.m30());
        int absoluteY = Math.round(pose.m31());
        int scissorLeft = absoluteX + this.contentsArea.getX();
        int scissorTop = absoluteY + this.contentsArea.getY();
        int scissorRight = scissorLeft + this.contentsArea.getWidth();
        int scissorBottom = scissorTop + this.contentsArea.getHeight();
        guiGraphics.enableScissor(scissorLeft, scissorTop, scissorRight, scissorBottom);
        poseStack.pushPose();
        int scrollPixels = this.getScrollPixels();
        poseStack.translate(0.0D, -scrollPixels, 0.0D);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GatewayEntityCategory.drawScrollableContents(this.recipe, guiGraphics, 0, 0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawEggSlots(guiGraphics);
        poseStack.popPose();
        guiGraphics.disableScissor();
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        double adjustedMouseY = mouseY + this.getScrollPixels();
        for (IRecipeSlotDrawable eggSlot : this.eggSlots) {
            if (eggSlot.isMouseOver(mouseX, adjustedMouseY)) {
                eggSlot.getTooltip(tooltip);
                return;
            }
        }
    }

    @Override
    public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
        double adjustedMouseY = mouseY + this.getScrollPixels();
        for (IRecipeSlotDrawable eggSlot : this.eggSlots) {
            if (eggSlot.isMouseOver(mouseX, adjustedMouseY)) {
                return Optional.of(new RecipeSlotUnderMouse(eggSlot, this.position.x(), this.position.y() - this.getScrollPixels()));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean handleInput(double mouseX, double mouseY, IJeiUserInput userInput) {
        if (userInput.getKey().getValue() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!userInput.isSimulate()) {
            this.dragOriginY = -1.0D;
        }
        if (!contains(this.scrollArea, mouseX, mouseY) || this.getHiddenAmount() == 0) {
            return false;
        }
        if (userInput.isSimulate()) {
            Rect2i scrollMarkerArea = this.calculateScrollbarMarkerArea();
            if (!contains(scrollMarkerArea, mouseX, mouseY)) {
                this.moveScrollbarCenterTo(scrollMarkerArea, mouseY);
                scrollMarkerArea = this.calculateScrollbarMarkerArea();
            }
            this.dragOriginY = mouseY - scrollMarkerArea.getY();
        }
        return true;
    }

    @Override
    public boolean handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (this.getHiddenAmount() > 0) {
            float scrollAmount = (float) (scrollDeltaY * GatewayEntityCategory.getSlotSpacing() / Math.max(this.contentHeight, 1));
            this.scrollOffsetY = Mth.clamp(this.scrollOffsetY - scrollAmount, 0.0F, 1.0F);
        } else {
            this.scrollOffsetY = 0.0F;
        }
        return true;
    }

    @Override
    public boolean handleMouseDragged(double mouseX, double mouseY, InputConstants.Key mouseKey, double dragX, double dragY) {
        if (this.dragOriginY < 0.0D || mouseKey.getValue() != 0) {
            return false;
        }
        Rect2i scrollbarMarkerArea = this.calculateScrollbarMarkerArea();
        double topY = mouseY - this.dragOriginY;
        this.moveScrollbarTo(scrollbarMarkerArea, topY);
        return true;
    }

    private void drawEggSlots(GuiGraphics guiGraphics) {
        for (int i = 0; i < this.eggSlots.size(); i++) {
            IRecipeSlotDrawable eggSlot = this.eggSlots.get(i);
            int x = GatewayEntityCategory.getEggGridX() + (i % GatewayEntityCategory.getEggGridColumns()) * GatewayEntityCategory.getSlotSpacing() + 1;
            int y = GatewayEntityCategory.getEggGridY() + (i / GatewayEntityCategory.getEggGridColumns()) * GatewayEntityCategory.getSlotSpacing() + 1;
            eggSlot.setPosition(x, y);
            eggSlot.draw(guiGraphics);
        }
    }

    private Rect2i calculateScrollbarMarkerArea() {
        int totalSpace = this.scrollArea.getHeight() - 2;
        int scrollMarkerWidth = this.scrollArea.getWidth() - 2;
        int scrollMarkerHeight = Math.round((float) totalSpace * ((float) this.getVisibleAmount() / (float) (this.getVisibleAmount() + this.getHiddenAmount())));
        scrollMarkerHeight = Math.max(scrollMarkerHeight, MIN_SCROLL_MARKER_HEIGHT);
        int scrollbarMarkerY = Math.round((float) (totalSpace - scrollMarkerHeight) * this.scrollOffsetY);
        return new Rect2i(this.scrollArea.getX() + 1, this.scrollArea.getY() + 1 + scrollbarMarkerY, scrollMarkerWidth, scrollMarkerHeight);
    }

    private int getVisibleAmount() {
        return this.contentsArea.getHeight();
    }

    private int getHiddenAmount() {
        return Math.max(this.contentHeight - this.contentsArea.getHeight(), 0);
    }

    private int getScrollPixels() {
        return Math.round((float) this.getHiddenAmount() * this.scrollOffsetY);
    }

    private void moveScrollbarCenterTo(Rect2i scrollMarkerArea, double centerY) {
        double topY = centerY - (double) scrollMarkerArea.getHeight() / 2.0D;
        this.moveScrollbarTo(scrollMarkerArea, topY);
    }

    private void moveScrollbarTo(Rect2i scrollMarkerArea, double topY) {
        int minY = this.scrollArea.getY();
        int maxY = this.scrollArea.getY() + this.scrollArea.getHeight() - scrollMarkerArea.getHeight();
        double relativeY = topY - (double) minY;
        int totalSpace = maxY - minY;
        if (totalSpace <= 0) {
            this.scrollOffsetY = 0.0F;
            return;
        }
        this.scrollOffsetY = Mth.clamp((float) (relativeY / (double) totalSpace), 0.0F, 1.0F);
    }

    private static boolean contains(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX()
                && mouseX < rect.getX() + rect.getWidth()
                && mouseY >= rect.getY()
                && mouseY < rect.getY() + rect.getHeight();
    }

    private static void drawPanel(GuiGraphics guiGraphics, Rect2i rect) {
        int x = rect.getX();
        int y = rect.getY();
        int width = rect.getWidth();
        int height = rect.getHeight();
        guiGraphics.fill(x, y, x + width, y + height, PANEL_FILL);
        guiGraphics.fill(x, y, x + width, y + 1, BORDER_LIGHT);
        guiGraphics.fill(x, y, x + 1, y + height, BORDER_LIGHT);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, BORDER_DARK);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, BORDER_DARK);
    }

    private static void drawMarker(GuiGraphics guiGraphics, Rect2i rect) {
        int x = rect.getX();
        int y = rect.getY();
        int width = rect.getWidth();
        int height = rect.getHeight();
        guiGraphics.fill(x, y, x + width, y + height, MARKER_FILL);
        guiGraphics.fill(x, y, x + width, y + 1, BORDER_LIGHT);
        guiGraphics.fill(x, y, x + 1, y + height, BORDER_LIGHT);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, BORDER_DARK);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, BORDER_DARK);
    }
}
