package com.hp.jei_gateways.jei;

import com.hp.jei_gateways.JeiGateways;
import com.hp.jei_gateways.gateway.GatewayEntityRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
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

public class GatewayEntityCategory implements IRecipeCategory<GatewayEntityRecipe> {
    public static final RecipeType<GatewayEntityRecipe> TYPE = RecipeType.create(JeiGateways.MODID, "gateway_entities", GatewayEntityRecipe.class);
    private static final int WIDTH = 210;
    private static final int HEIGHT = 156;
    private static final int HEADER_BOX_X = 4;
    private static final int HEADER_BOX_Y = 4;
    private static final int HEADER_BOX_WIDTH = 202;
    private static final int HEADER_BOX_HEIGHT = 36;
    private static final int WAVE_BOX_X = 4;
    private static final int WAVE_BOX_Y = 48;
    private static final int WAVE_BOX_WIDTH = 202;
    private static final int WAVE_BOX_HEIGHT = 104;
    private static final int CONTENT_X = 6;
    private static final int CONTENT_Y = 50;
    private static final int CONTENT_WIDTH = 198;
    private static final int CONTENT_HEIGHT = 100;
    private static final int HEADER_TEXT_X = 34;
    private static final int HEADER_NAME_Y = 10;
    private static final int HEADER_RECIPE_Y = 24;
    private static final int HEADER_SLOT_X = 10;
    private static final int HEADER_SLOT_Y = 10;
    private static final int EGG_GRID_X = 10;
    private static final int EGG_GRID_Y = 82;
    private static final int EGG_GRID_COLUMNS = 7;
    private static final int SLOT_SPACING = 18;
    private static final int SCROLLBAR_EXTRA_WIDTH = 16;

    private final IDrawableStatic background;
    private final IDrawable icon;

    public GatewayEntityCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<GatewayEntityRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.jei_gateways.gateway_entities");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GatewayEntityRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 10, 12)
                .addItemStack(recipe.pearl())
                .setStandardSlotBackground()
                .setSlotName("pearl")
                .addTooltipCallback((slot, tooltip) -> addPearlTooltip(recipe, tooltip));

        List<ItemStack> spawnEggs = recipe.spawnEggs();
        if (!spawnEggs.isEmpty()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(spawnEggs);
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(spawnEggs);
        }
        if (!recipe.relatedItems().isEmpty()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(recipe.relatedItems());
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(recipe.relatedItems());
        }
    }

    @Override
    public void draw(GatewayEntityRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        drawPanel(guiGraphics, CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
    }

    @Override
    public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder, GatewayEntityRecipe recipe, IFocusGroup focuses) {
        builder.addScrollBoxWidget(CONTENT_WIDTH, CONTENT_HEIGHT, CONTENT_X, CONTENT_Y)
                .setContents(new GatewayEntityContentsDrawable(recipe));
    }

    @Override
    public ResourceLocation getRegistryName(GatewayEntityRecipe recipe) {
        return ResourceLocation.fromNamespaceAndPath(JeiGateways.MODID, recipe.gatewayId().getNamespace() + "/" + recipe.gatewayId().getPath() + "/wave_" + recipe.waveLevel());
    }

    private static void addPearlTooltip(GatewayEntityRecipe recipe, List<Component> tooltip) {
        tooltip.add(Component.translatable("jei.jei_gateways.name", recipe.pearl().getHoverName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.jei_gateways.wave_level", recipe.waveLevel(), recipe.waveCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.jei_gateways.entity_count", recipe.entityCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.jei_gateways.has_recipe_pages", Component.translatable(JeiGatewaysPlugin.hasOtherRecipePages(recipe.pearl()) ? "jei.jei_gateways.yes" : "jei.jei_gateways.no")).withStyle(ChatFormatting.GRAY));
    }

    static void drawScrollableContents(GatewayEntityRecipe recipe, GuiGraphics guiGraphics, int x, int y, int width, int modifierStartY) {
        Font font = Minecraft.getInstance().font;
        drawPanel(guiGraphics, x, y, width, HEADER_BOX_HEIGHT);
        drawPanel(guiGraphics, x, y + 44, width, getContentHeight(recipe) - 44);

        drawSlotBackground(guiGraphics, x + HEADER_SLOT_X, y + HEADER_SLOT_Y);
        guiGraphics.renderItem(recipe.pearl(), x + HEADER_SLOT_X + 1, y + HEADER_SLOT_Y + 1);
        guiGraphics.drawString(font, Component.translatable("jei.jei_gateways.name", recipe.pearl().getHoverName()), x + HEADER_TEXT_X, y + HEADER_NAME_Y, 0xFF1F1F1F, false);
        guiGraphics.drawString(font, Component.translatable("jei.jei_gateways.has_recipe_pages_label"), x + HEADER_TEXT_X, y + HEADER_RECIPE_Y, 0xFF2A2A2A, false);
        Component yesNo = Component.translatable(JeiGatewaysPlugin.hasOtherRecipePages(recipe.pearl()) ? "jei.jei_gateways.yes" : "jei.jei_gateways.no");
        int yesNoX = x + HEADER_TEXT_X + font.width(Component.translatable("jei.jei_gateways.has_recipe_pages_label")) + 2;
        int yesNoColor = JeiGatewaysPlugin.hasOtherRecipePages(recipe.pearl()) ? 0xFF1C8C43 : 0xFFB33A2B;
        guiGraphics.drawString(font, yesNo, yesNoX, y + HEADER_RECIPE_Y, yesNoColor, false);

        guiGraphics.drawString(font, Component.translatable("jei.jei_gateways.wave_level", recipe.waveLevel(), recipe.waveCount()), x + 4, y + 52, 0xFF1F1F1F, false);
        guiGraphics.drawString(font, Component.translatable("jei.jei_gateways.wave_entities"), x + 4, y + 66, 0xFF2A2A2A, false);
        int eggIndex = 0;
        for (GatewayEntityRecipe.LinkedEntity entity : recipe.waveEntities()) {
            int eggX = x + EGG_GRID_X + (eggIndex % EGG_GRID_COLUMNS) * SLOT_SPACING;
            int eggY = y + EGG_GRID_Y + (eggIndex / EGG_GRID_COLUMNS) * SLOT_SPACING;
            drawSlotBackground(guiGraphics, eggX, eggY);
            if (!entity.spawnEgg().isEmpty()) {
                guiGraphics.renderItem(entity.spawnEgg(), eggX + 1, eggY + 1);
            }
            eggIndex++;
        }
        guiGraphics.drawString(font, Component.translatable("jei.jei_gateways.wave_modifiers"), x + 4, y + modifierStartY, 0xFF2A2A2A, false);

        int lineY = y + modifierStartY + 12;
        if (recipe.waveModifiers().isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("jei.jei_gateways.no_wave_modifiers"), x + 4, lineY, 0xFF666666, false);
            return;
        }
        for (Component modifier : recipe.waveModifiers()) {
            guiGraphics.drawString(font, modifier, x + 4, lineY, 0xFF1F1F1F, false);
            lineY += 10;
        }
    }

    static int getContentHeight(GatewayEntityRecipe recipe) {
        int rows = (recipe.waveEntities().size() + EGG_GRID_COLUMNS - 1) / EGG_GRID_COLUMNS;
        int modifierLines = Math.max(1, recipe.waveModifiers().size());
        return getModifierStartY(recipe) + 12 + modifierLines * 10 + 8;
    }

    static int getModifierStartY(GatewayEntityRecipe recipe) {
        int rows = (recipe.waveEntities().size() + EGG_GRID_COLUMNS - 1) / EGG_GRID_COLUMNS;
        return EGG_GRID_Y + rows * SLOT_SPACING + 4;
    }

    static int getEggGridX() {
        return EGG_GRID_X;
    }

    static int getEggGridY() {
        return EGG_GRID_Y;
    }

    static int getEggGridColumns() {
        return EGG_GRID_COLUMNS;
    }

    static int getSlotSpacing() {
        return SLOT_SPACING;
    }

    static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFFE3E3E3);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFF8F8F8);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFF8F8F8);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF8A8A8A);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF8A8A8A);
    }

    private static void drawSlotBackground(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF8A8A8A);
        guiGraphics.fill(x, y, x + 17, y + 17, 0xFFF8F8F8);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF9E9E9E);
    }

    private static class GatewayEntityContentsDrawable implements IDrawable {
        private final GatewayEntityRecipe recipe;
        private final int width;
        private final int height;
        private final int modifierStartY;

        private GatewayEntityContentsDrawable(GatewayEntityRecipe recipe) {
            this.recipe = recipe;
            this.width = CONTENT_WIDTH - SCROLLBAR_EXTRA_WIDTH;
            this.height = getContentHeight(recipe);
            this.modifierStartY = getModifierStartY(recipe);
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
            drawScrollableContents(recipe, guiGraphics, xOffset, yOffset, width, modifierStartY);
        }
    }
}
