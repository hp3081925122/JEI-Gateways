package com.hp.jei_gateways.jei;

import com.hp.jei_gateways.JeiGateways;
import com.hp.jei_gateways.gateway.GatewayLootRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class GatewayLootCategory implements IRecipeCategory<GatewayLootRecipe> {
    public static final RecipeType<GatewayLootRecipe> TYPE = RecipeType.create(JeiGateways.MODID, "gateway_loot", GatewayLootRecipe.class);
    private static final int WIDTH = 180;
    private static final int SLOT_SIZE = 18;
    private static final int PEARL_X = 8;
    private static final int PEARL_Y = 8;
    private static final int HEADER_TEXT_X = 30;
    private static final int HEADER_NAME_Y = 10;
    private static final int HEADER_TEXT_MAX_WIDTH = WIDTH - HEADER_TEXT_X - 4;
    private static final int HEADER_LINE_HEIGHT = 9;
    private static final int HEADER_NAME_MAX_LINES = 2;
    private static final int GRID_COLUMNS = 7;
    private static final int MAX_VISIBLE_ROWS = 4;
    private static final int SCROLLBAR_EXTRA_WIDTH = 16;
    private static final int PANEL_PADDING = 5;
    private static final int PANEL_X = 8;
    private static final int PANEL_Y = 44;
    private static final int GRID_X = PANEL_X + PANEL_PADDING;
    private static final int GRID_Y = PANEL_Y + PANEL_PADDING;
    private static final int PANEL_WIDTH = GRID_COLUMNS * SLOT_SIZE + SCROLLBAR_EXTRA_WIDTH + PANEL_PADDING * 2;
    private static final int BOTTOM_MARGIN = 6;
    private static final int HEIGHT_WITHOUT_ROWS = PANEL_Y + PANEL_PADDING * 2 + BOTTOM_MARGIN;

    private final IDrawable icon;

    public GatewayLootCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<GatewayLootRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.jei_gateways.gateway_loot");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT_WITHOUT_ROWS + getVisibleRows() * SLOT_SIZE;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GatewayLootRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, PEARL_X, PEARL_Y)
                .addItemStack(recipe.pearl())
                .setSlotName("pearl")
                .addTooltipCallback((slot, tooltip) -> addPearlTooltip(recipe, tooltip));

        for (ItemStack output : recipe.outputs()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, GRID_X, GRID_Y)
                    .setStandardSlotBackground()
                    .addItemStack(output);
        }
    }

    @Override
    public void draw(GatewayLootRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        drawSlotFrame(guiGraphics, PEARL_X, PEARL_Y);
        int nameLines = JeiTextUtil.drawWrapped(guiGraphics, font, Component.translatable("jei.jei_gateways.name", recipe.pearl().getHoverName()), HEADER_TEXT_X, HEADER_NAME_Y, HEADER_TEXT_MAX_WIDTH, HEADER_LINE_HEIGHT, 0xFF1F1F1F, HEADER_NAME_MAX_LINES);
        guiGraphics.drawString(font, Component.translatable("jei.jei_gateways.loot_total", recipe.totalOutputCount()), HEADER_TEXT_X, HEADER_NAME_Y + nameLines * HEADER_LINE_HEIGHT + 5, 0xFF2A2A2A, false);
    }

    @Override
    public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder, GatewayLootRecipe recipe, IFocusGroup focuses) {
        List<mezz.jei.api.gui.ingredient.IRecipeSlotDrawable> outputSlots = builder.getRecipeSlots().getSlots().stream()
                .filter(slot -> !"pearl".equals(slot.getSlotName().orElse("")))
                .toList();
        int visibleRows = getVisibleRows();
        builder.addWidget(new GatewayPanelWidget(PANEL_X, PANEL_Y, PANEL_WIDTH, visibleRows * SLOT_SIZE + PANEL_PADDING * 2));
        builder.addScrollGridWidget(outputSlots, GRID_COLUMNS, visibleRows)
                .setPosition(GRID_X, GRID_Y);
    }

    @Override
    public ResourceLocation getRegistryName(GatewayLootRecipe recipe) {
        return ResourceLocation.fromNamespaceAndPath(
                JeiGateways.MODID,
                "loot/" + recipe.gatewayId().getNamespace() + "/" + recipe.gatewayId().getPath()
        );
    }

    private static int getVisibleRows() {
        int maxHeight = HEIGHT_WITHOUT_ROWS + MAX_VISIBLE_ROWS * SLOT_SIZE;
        int minHeight = HEIGHT_WITHOUT_ROWS + SLOT_SIZE;
        return (GatewayJeiLayout.fitHeight(maxHeight, minHeight) - HEIGHT_WITHOUT_ROWS) / SLOT_SIZE;
    }

    private static void addPearlTooltip(GatewayLootRecipe recipe, List<Component> tooltip) {
        tooltip.add(Component.translatable("jei.jei_gateways.name", recipe.pearl().getHoverName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.jei_gateways.loot_total", recipe.totalOutputCount()).withStyle(ChatFormatting.GRAY));
        if (recipe.pearlTooltipText() != null) {
            tooltip.add(recipe.pearlTooltipText().copy().withStyle(ChatFormatting.GRAY));
        }
    }

    private static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF6F6F6F);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFFCACACA);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 2, y + 2, 0xFFE7E7E7);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + SLOT_SIZE - 2, 0xFFE7E7E7);
        guiGraphics.fill(x + 2, y + SLOT_SIZE - 2, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);
        guiGraphics.fill(x + SLOT_SIZE - 2, y + 2, x + SLOT_SIZE - 1, y + SLOT_SIZE - 2, 0xFF8B8B8B);
    }
}
