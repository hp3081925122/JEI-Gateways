package com.hp.jei_gateways.jei;

import com.hp.jei_gateways.JeiGateways;
import com.hp.jei_gateways.gateway.GatewayEntityRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class GatewayEntityCategory implements IRecipeCategory<GatewayEntityRecipe> {
    public static final RecipeType<GatewayEntityRecipe> TYPE = RecipeType.create(JeiGateways.MODID, "gateway_entities", GatewayEntityRecipe.class);

    private static final int WIDTH = 200;
    private static final int HEIGHT = 200;

    private static final int HEADER_BOX_X = 6;
    private static final int HEADER_BOX_Y = 14;
    private static final int HEADER_BOX_WIDTH = 188;
    private static final int HEADER_BOX_HEIGHT = 46;

    private static final int HEADER_TEXT_X = 34;
    private static final int HEADER_NAME_Y = 8;
    private static final int HEADER_TEXT_MAX_WIDTH = HEADER_BOX_WIDTH - HEADER_TEXT_X - 4;
    private static final int HEADER_LINE_HEIGHT = 9;
    private static final int HEADER_MAX_LINES = 3;
    private static final int HEADER_SLOT_X = 10;
    private static final int HEADER_SLOT_Y = 8;

    private static final int CONTENT_X = 6;
    private static final int CONTENT_Y = 64;
    private static final int CONTENT_WIDTH = 188;
    private static final int CONTENT_HEIGHT = 130;

    private static final int INNER_PADDING_X = 10;
    private static final int CONTENT_TEXT_X = 4;
    private static final int WAVE_LEVEL_Y = 6;
    private static final int ENTITY_LABEL_Y = 20;
    private static final int EGG_GRID_Y = 36;
    private static final int EGG_GRID_COLUMNS = 7;
    private static final int SLOT_SPACING = 18;
    private static final int MODIFIER_LABEL_OFFSET = 14;
    private static final int MODIFIER_LINE_HEIGHT = 10;
    private static final int CONTENT_BOTTOM_PADDING = 8;
    private static final int CONTENT_TEXT_WIDTH = CONTENT_WIDTH - 16 - INNER_PADDING_X - CONTENT_TEXT_X - 4;

    private final IDrawable icon;

    public GatewayEntityCategory(IGuiHelper guiHelper, ItemStack iconStack) {
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
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GatewayEntityRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, HEADER_BOX_X + HEADER_SLOT_X, HEADER_BOX_Y + HEADER_SLOT_Y)
                .addItemStack(recipe.pearl())
                .setStandardSlotBackground()
                .setSlotName("pearl")
                .addRichTooltipCallback((slot, tooltip) -> addPearlTooltip(recipe, tooltip));

        for (GatewayEntityRecipe.LinkedEntity entity : recipe.waveEntities()) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, CONTENT_X + INNER_PADDING_X, CONTENT_Y + EGG_GRID_Y)
                    .addItemStack(entity.spawnEgg())
                    .setStandardSlotBackground()
                    .addRichTooltipCallback((slot, tooltip) -> addWaveEntityTooltip(entity, tooltip));
        }

        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(recipe.pearl());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(recipe.pearl());

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
    public void draw(GatewayEntityRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        drawFixedHeader(recipe, guiGraphics, HEADER_BOX_X, HEADER_BOX_Y, HEADER_BOX_WIDTH);
        drawPanel(guiGraphics, CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
    }

    @Override
    public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder, GatewayEntityRecipe recipe, IFocusGroup focuses) {
        List<IRecipeSlotDrawable> eggSlots = builder.getRecipeSlots().getSlots().stream()
                .filter(slot -> !"pearl".equals(slot.getSlotName().orElse("")))
                .toList();
        GatewayEntityScrollWidget widget = new GatewayEntityScrollWidget(recipe, CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT, eggSlots);
        builder.addSlottedWidget(widget, eggSlots);
        builder.addInputHandler(widget);
    }

    @Override
    public Identifier getRegistryName(GatewayEntityRecipe recipe) {
        return Identifier.fromNamespaceAndPath(JeiGateways.MODID, recipe.gatewayId().getNamespace() + "/" + recipe.gatewayId().getPath() + "/wave_" + recipe.waveLevel());
    }

    private static void drawFixedHeader(GatewayEntityRecipe recipe, GuiGraphicsExtractor guiGraphics, int x, int y, int width) {
        Font font = Minecraft.getInstance().font;
        drawPanel(guiGraphics, x, y, width, HEADER_BOX_HEIGHT);
        int lineY = y + HEADER_NAME_Y;
        int linesLeft = HEADER_MAX_LINES;
        int nameLines = JeiTextUtil.drawWrapped(guiGraphics, font, JeiTextUtil.blackName(recipe.pearl()), x + HEADER_TEXT_X, lineY, HEADER_TEXT_MAX_WIDTH, HEADER_LINE_HEIGHT, 0xFF000000, false, linesLeft);
        lineY += nameLines * HEADER_LINE_HEIGHT;
        linesLeft -= nameLines;
        if (recipe.pearlTooltipText() != null && linesLeft > 0) {
            JeiTextUtil.drawWrapped(guiGraphics, font, recipe.pearlTooltipText(), x + HEADER_TEXT_X, lineY, HEADER_TEXT_MAX_WIDTH, HEADER_LINE_HEIGHT, 0xFF000000, false, linesLeft);
        }
    }

    static void drawScrollableContents(GatewayEntityRecipe recipe, GuiGraphicsExtractor guiGraphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        int contentX = x + INNER_PADDING_X + CONTENT_TEXT_X;
        guiGraphics.text(font, Component.translatable("jei.jei_gateways.wave_level", recipe.waveLevel(), recipe.waveCount()), contentX, y + WAVE_LEVEL_Y, 0xFF1F1F1F, false);
        guiGraphics.text(font, Component.translatable("jei.jei_gateways.wave_entities"), contentX, y + ENTITY_LABEL_Y, 0xFF2A2A2A, false);

        int modifierLabelY = getModifierStartY(recipe);
        guiGraphics.text(font, Component.translatable("jei.jei_gateways.wave_modifiers"), contentX, y + modifierLabelY, 0xFF2A2A2A, false);

        int lineY = y + modifierLabelY + MODIFIER_LABEL_OFFSET;
        if (recipe.waveModifiers().isEmpty()) {
            guiGraphics.text(font, Component.translatable("jei.jei_gateways.no_wave_modifiers"), contentX, lineY, 0xFF666666, false);
            return;
        }
        for (Component modifier : recipe.waveModifiers()) {
            int modifierLines = JeiTextUtil.drawWrapped(guiGraphics, font, modifier, contentX, lineY, CONTENT_TEXT_WIDTH, MODIFIER_LINE_HEIGHT, 0xFF5D5DFF, false, Integer.MAX_VALUE);
            lineY += modifierLines * MODIFIER_LINE_HEIGHT;
        }
    }

    private static void addPearlTooltip(GatewayEntityRecipe recipe, ITooltipBuilder tooltip) {
        tooltip.add(Component.translatable("jei.jei_gateways.name", recipe.pearl().getHoverName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.jei_gateways.wave_level", recipe.waveLevel(), recipe.waveCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.jei_gateways.entity_count", recipe.entityCount()).withStyle(ChatFormatting.GRAY));
        if (recipe.pearlTooltipText() != null) {
            tooltip.add(recipe.pearlTooltipText().copy().withStyle(ChatFormatting.GRAY));
        }
    }

    private static void addWaveEntityTooltip(GatewayEntityRecipe.LinkedEntity entity, ITooltipBuilder tooltip) {
        tooltip.add(entity.displayName().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.jei_gateways.entity_stack_count", entity.count()).withStyle(ChatFormatting.GRAY));
        for (Component modifier : entity.modifiers()) {
            tooltip.add(modifier.copy().withStyle(ChatFormatting.GRAY));
        }
    }

    static int getEggGridX() {
        return INNER_PADDING_X;
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

    static int getContentWidth() {
        return CONTENT_WIDTH;
    }

    static int getContentHeight(GatewayEntityRecipe recipe) {
        int rows = Math.max(1, (recipe.waveEntities().size() + EGG_GRID_COLUMNS - 1) / EGG_GRID_COLUMNS);
        int modifierLines = getModifierLineCount(recipe);
        return getModifierStartY(recipe) + MODIFIER_LABEL_OFFSET + modifierLines * MODIFIER_LINE_HEIGHT + CONTENT_BOTTOM_PADDING;
    }

    private static int getModifierLineCount(GatewayEntityRecipe recipe) {
        if (recipe.waveModifiers().isEmpty()) {
            return 1;
        }
        Font font = Minecraft.getInstance().font;
        return recipe.waveModifiers().stream()
                .mapToInt(modifier -> JeiTextUtil.lineCount(font, modifier, CONTENT_TEXT_WIDTH))
                .sum();
    }

    static int getModifierStartY(GatewayEntityRecipe recipe) {
        int rows = Math.max(1, (recipe.waveEntities().size() + EGG_GRID_COLUMNS - 1) / EGG_GRID_COLUMNS);
        return EGG_GRID_Y + rows * SLOT_SPACING + 4;
    }

    static void drawPanel(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFFE3E3E3);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFF8F8F8);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFF8F8F8);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF8A8A8A);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF8A8A8A);
    }
}
