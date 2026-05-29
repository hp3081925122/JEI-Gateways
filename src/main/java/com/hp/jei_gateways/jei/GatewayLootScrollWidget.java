package com.hp.jei_gateways.jei;

import com.hp.jei_gateways.gateway.GatewayLootRecipe;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.elements.DrawableNineSliceTexture;
import mezz.jei.common.gui.textures.Textures;
import mezz.jei.common.util.ImmutableRect2i;
import mezz.jei.common.util.MathUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Optional;

public class GatewayLootScrollWidget implements ISlottedRecipeWidget, IJeiInputHandler {
    private static final int SCROLLBAR_EXTRA_WIDTH = 16;
    private static final int MIN_SCROLL_MARKER_HEIGHT = 14;
    private static final int SLOT_SPACING = 18;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 8;
    private static final int GRID_COLUMNS = 7;

    private final List<IRecipeSlotDrawable> outputSlots;
    private final ImmutableRect2i area;
    private final ImmutableRect2i scrollArea;
    private final ImmutableRect2i contentsArea;
    private final DrawableNineSliceTexture scrollbarMarker;
    private final DrawableNineSliceTexture scrollbarBackground;
    private final int contentHeight;
    private double dragOriginY = -1.0D;
    private float scrollOffsetY = 0.0F;

    public GatewayLootScrollWidget(GatewayLootRecipe recipe, int x, int y, int width, int height, List<IRecipeSlotDrawable> outputSlots) {
        this.outputSlots = outputSlots;
        this.area = new ImmutableRect2i(x, y, width, height);
        this.scrollArea = new ImmutableRect2i(width - 14, 0, 14, height);
        this.contentsArea = new ImmutableRect2i(0, 0, width - SCROLLBAR_EXTRA_WIDTH, height);
        Textures textures = Internal.getTextures();
        this.scrollbarMarker = textures.getScrollbarMarker();
        this.scrollbarBackground = textures.getScrollbarBackground();
        this.contentHeight = calculateContentHeight(recipe);
    }

    @Override
    public ScreenPosition getPosition() {
        return this.area.getScreenPosition();
    }

    @Override
    public ScreenRectangle getArea() {
        return this.area.toScreenRectangle();
    }

    @Override
    public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.scrollbarBackground.draw(guiGraphics, this.scrollArea);
        ImmutableRect2i scrollbarMarkerArea = this.calculateScrollbarMarkerArea();
        this.scrollbarMarker.draw(guiGraphics, scrollbarMarkerArea);

        PoseStack poseStack = guiGraphics.pose();
        ScreenRectangle scissorArea = MathUtil.transform(this.contentsArea, poseStack.last().pose());
        guiGraphics.enableScissor(scissorArea.left(), scissorArea.top(), scissorArea.right(), scissorArea.bottom());
        poseStack.pushPose();
        int scrollPixels = this.getScrollPixels();
        poseStack.translate(0.0D, -scrollPixels, 0.0D);
        this.drawOutputSlots(guiGraphics);
        poseStack.popPose();
        guiGraphics.disableScissor();
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        double adjustedMouseY = mouseY + this.getScrollPixels();
        for (IRecipeSlotDrawable outputSlot : this.outputSlots) {
            if (outputSlot.isMouseOver(mouseX, adjustedMouseY)) {
                outputSlot.getTooltip(tooltip);
                return;
            }
        }
    }

    @Override
    public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
        double adjustedMouseY = mouseY + this.getScrollPixels();
        for (IRecipeSlotDrawable outputSlot : this.outputSlots) {
            if (outputSlot.isMouseOver(mouseX, adjustedMouseY)) {
                return Optional.of(new RecipeSlotUnderMouse(outputSlot, this.area.x(), this.area.y() - this.getScrollPixels()));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean handleInput(double mouseX, double mouseY, IJeiUserInput userInput) {
        if (!userInput.is(Internal.getKeyMappings().getLeftClick())) {
            return false;
        }
        if (!userInput.isSimulate()) {
            this.dragOriginY = -1.0D;
        }
        if (!this.scrollArea.contains(mouseX, mouseY) || this.getHiddenAmount() == 0) {
            return false;
        }
        if (userInput.isSimulate()) {
            ImmutableRect2i scrollMarkerArea = this.calculateScrollbarMarkerArea();
            if (!scrollMarkerArea.contains(mouseX, mouseY)) {
                this.moveScrollbarCenterTo(scrollMarkerArea, mouseY);
                scrollMarkerArea = this.calculateScrollbarMarkerArea();
            }
            this.dragOriginY = mouseY - scrollMarkerArea.y();
        }
        return true;
    }

    @Override
    public boolean handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaY) {
        if (this.getHiddenAmount() > 0) {
            float scrollAmount = (float) (scrollDeltaY * SLOT_SPACING / Math.max(this.contentHeight, 1));
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
        ImmutableRect2i scrollbarMarkerArea = this.calculateScrollbarMarkerArea();
        double topY = mouseY - this.dragOriginY;
        this.moveScrollbarTo(scrollbarMarkerArea, topY);
        return true;
    }

    private void drawOutputSlots(GuiGraphics guiGraphics) {
        for (int i = 0; i < this.outputSlots.size(); i++) {
            IRecipeSlotDrawable outputSlot = this.outputSlots.get(i);
            int x = GRID_X + (i % GRID_COLUMNS) * SLOT_SPACING + 1;
            int y = GRID_Y + (i / GRID_COLUMNS) * SLOT_SPACING + 1;
            outputSlot.setPosition(x, y);
            outputSlot.draw(guiGraphics);
        }
    }

    private ImmutableRect2i calculateScrollbarMarkerArea() {
        int totalSpace = this.scrollArea.height() - 2;
        int scrollMarkerWidth = this.scrollArea.width() - 2;
        int scrollMarkerHeight = Math.round((float) totalSpace * ((float) this.getVisibleAmount() / (float) (this.getVisibleAmount() + this.getHiddenAmount())));
        scrollMarkerHeight = Math.max(scrollMarkerHeight, MIN_SCROLL_MARKER_HEIGHT);
        int scrollbarMarkerY = Math.round((float) (totalSpace - scrollMarkerHeight) * this.scrollOffsetY);
        return new ImmutableRect2i(this.scrollArea.getX() + 1, this.scrollArea.getY() + 1 + scrollbarMarkerY, scrollMarkerWidth, scrollMarkerHeight);
    }

    private int getVisibleAmount() {
        return this.contentsArea.height();
    }

    private int getHiddenAmount() {
        return Math.max(this.contentHeight - this.contentsArea.height(), 0);
    }

    private int getScrollPixels() {
        return Math.round((float) this.getHiddenAmount() * this.scrollOffsetY);
    }

    private void moveScrollbarCenterTo(ImmutableRect2i scrollMarkerArea, double centerY) {
        double topY = centerY - (double) scrollMarkerArea.height() / 2.0D;
        this.moveScrollbarTo(scrollMarkerArea, topY);
    }

    private void moveScrollbarTo(ImmutableRect2i scrollMarkerArea, double topY) {
        int minY = this.scrollArea.y();
        int maxY = this.scrollArea.y() + this.scrollArea.height() - scrollMarkerArea.height();
        double relativeY = topY - (double) minY;
        int totalSpace = maxY - minY;
        this.scrollOffsetY = Mth.clamp((float) (relativeY / (double) totalSpace), 0.0F, 1.0F);
    }

    private static int calculateContentHeight(GatewayLootRecipe recipe) {
        int rows = Math.max(1, (recipe.outputs().size() + GRID_COLUMNS - 1) / GRID_COLUMNS);
        return GRID_Y + rows * SLOT_SPACING + 4;
    }
}
