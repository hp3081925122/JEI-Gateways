package com.hp.jei_gateways.jei;

import com.hp.jei_gateways.JeiGateways;
import com.hp.jei_gateways.gateway.GatewayEntityRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class GatewayEntityCategory implements IRecipeCategory<GatewayEntityRecipe> {
    public static final RecipeType<GatewayEntityRecipe> TYPE = RecipeType.create(JeiGateways.MODID, "gateway_entities", GatewayEntityRecipe.class);
    private static final int WIDTH = 210;
    private static final int HEIGHT = 156;
    private static final int HEADER_BOX_X = 6;
    private static final int HEADER_BOX_Y = 18;
    private static final int HEADER_BOX_WIDTH = 198;
    private static final int HEADER_BOX_HEIGHT = 34;
    private static final int CONTENT_X = 6;
    private static final int CONTENT_Y = 56;
    private static final int CONTENT_WIDTH = 198;
    private static final int CONTENT_HEIGHT = 96;
    private static final int HEADER_TEXT_X = 34;
    private static final int HEADER_NAME_Y = 8;
    private static final int HEADER_RECIPE_Y = 22;
    private static final int HEADER_SLOT_X = 10;
    private static final int HEADER_SLOT_Y = 8;
    private static final int EGG_GRID_X = 10;
    private static final int EGG_GRID_Y = 34;
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
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, HEADER_BOX_X + HEADER_SLOT_X, HEADER_BOX_Y + HEADER_SLOT_Y)
                .addItemStack(recipe.pearl())
                .setStandardSlotBackground()
                .setSlotName("pearl")
                .addTooltipCallback((slot, tooltip) -> addPearlTooltip(recipe, tooltip));

        int eggIndex = 0;
        for (GatewayEntityRecipe.LinkedEntity entity : recipe.waveEntities()) {
            int x = EGG_GRID_X + (eggIndex % EGG_GRID_COLUMNS) * SLOT_SPACING;
            int y = EGG_GRID_Y + (eggIndex / EGG_GRID_COLUMNS) * SLOT_SPACING;
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, x, y)
                    .addItemStack(entity.spawnEgg())
                    .setStandardSlotBackground()
                    .addTooltipCallback((slot, tooltip) -> addWaveEntityTooltip(entity, tooltip));
            eggIndex++;
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
    public void draw(GatewayEntityRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        drawFixedHeader(recipe, guiGraphics, HEADER_BOX_X, HEADER_BOX_Y, HEADER_BOX_WIDTH);
        drawPanel(guiGraphics, CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
    }

    @Override
    public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder, GatewayEntityRecipe recipe, IFocusGroup focuses) {
        List<IRecipeSlotDrawable> eggSlots = builder.getRecipeSlots().getSlots().stream()
                .filter(slot -> !"pearl".equals(slot.getSlotName().orElse("")))
                .toList();
        GatewayEntityWaveScrollWidget widget = new GatewayEntityWaveScrollWidget(recipe, CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT, eggSlots);
        builder.addSlottedWidget(widget, eggSlots);
        builder.addInputHandler(widget);
    }

    @Override
    public ResourceLocation getRegistryName(GatewayEntityRecipe recipe) {
        return ResourceLocation.fromNamespaceAndPath(JeiGateways.MODID, recipe.gatewayId().getNamespace() + "/" + recipe.gatewayId().getPath() + "/wave_" + recipe.waveLevel());
    }

    private static void drawFixedHeader(GatewayEntityRecipe recipe, GuiGraphics guiGraphics, int x, int y, int width) {
        Font font = Minecraft.getInstance().font;
        drawPanel(guiGraphics, x, y, width, HEADER_BOX_HEIGHT);
        guiGraphics.drawString(font, Component.translatable("jei.jei_gateways.name", recipe.pearl().getHoverName()), x + HEADER_TEXT_X, y + HEADER_NAME_Y, 0xFF1F1F1F, false);
        guiGraphics.drawString(font, Component.translatable("jei.jei_gateways.has_recipe_pages_label"), x + HEADER_TEXT_X, y + HEADER_RECIPE_Y, 0xFF2A2A2A, false);
        Component yesNo = Component.translatable(JeiGatewaysPlugin.hasOtherRecipePages(recipe.pearl()) ? "jei.jei_gateways.yes" : "jei.jei_gateways.no");
        int yesNoX = x + HEADER_TEXT_X + font.width(Component.translatable("jei.jei_gateways.has_recipe_pages_label")) + 2;
        int yesNoColor = JeiGatewaysPlugin.hasOtherRecipePages(recipe.pearl()) ? 0xFF1C8C43 : 0xFFB33A2B;
        guiGraphics.drawString(font, yesNo, yesNoX, y + HEADER_RECIPE_Y, yesNoColor, false);
    }

    private static void addPearlTooltip(GatewayEntityRecipe recipe, List<Component> tooltip) {
        tooltip.add(Component.translatable("jei.jei_gateways.name", recipe.pearl().getHoverName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.jei_gateways.wave_level", recipe.waveLevel(), recipe.waveCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.jei_gateways.entity_count", recipe.entityCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.jei_gateways.has_recipe_pages", Component.translatable(JeiGatewaysPlugin.hasOtherRecipePages(recipe.pearl()) ? "jei.jei_gateways.yes" : "jei.jei_gateways.no")).withStyle(ChatFormatting.GRAY));
    }

    private static void addWaveEntityTooltip(GatewayEntityRecipe.LinkedEntity entity, List<Component> tooltip) {
        tooltip.add(entity.displayName().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.jei_gateways.entity_stack_count", entity.count()).withStyle(ChatFormatting.GRAY));
        for (Component modifier : entity.modifiers()) {
            tooltip.add(modifier.copy().withStyle(ChatFormatting.GRAY));
        }
    }

    static void drawScrollableContents(GatewayEntityRecipe recipe, GuiGraphics guiGraphics, int x, int y, int width, int modifierStartY) {
        Font font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, Component.translatable("jei.jei_gateways.wave_level", recipe.waveLevel(), recipe.waveCount()), x + 4, y + 6, 0xFF1F1F1F, false);
        guiGraphics.drawString(font, Component.translatable("jei.jei_gateways.wave_entities"), x + 4, y + 20, 0xFF2A2A2A, false);
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

    static int getContentWidth() {
        return CONTENT_WIDTH;
    }

    static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFFE3E3E3);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFF8F8F8);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFF8F8F8);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF8A8A8A);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF8A8A8A);
    }
}
