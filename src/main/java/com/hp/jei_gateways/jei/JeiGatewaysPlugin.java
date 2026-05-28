package com.hp.jei_gateways.jei;

import com.hp.jei_gateways.JeiGateways;
import com.hp.jei_gateways.gateway.GatewayEntityCache;
import com.hp.jei_gateways.gateway.GatewayEntityRecipe;
import com.hp.jei_gateways.gateway.GatewayLootCache;
import com.hp.jei_gateways.gateway.GatewayLootRecipe;
import dev.shadowsoffire.gateways.GatewayObjects;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@JeiPlugin
public class JeiGatewaysPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(JeiGateways.MODID, "jei_plugin");
    private static int registeredRecipeCount = 0;
    private static IJeiRuntime jeiRuntime;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new GatewayEntityCategory(registration.getJeiHelpers().getGuiHelper(), new ItemStack(GatewayObjects.GATE_PEARL.get())));
        registration.addRecipeCategories(new GatewayLootCategory(registration.getJeiHelpers().getGuiHelper(), new ItemStack(GatewayObjects.GATE_PEARL.get())));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<GatewayEntityRecipe> recipes = GatewayEntityCache.getRecipes();
        registeredRecipeCount = recipes.size();
        JeiGateways.LOGGER.info("JEI Gateways 注册了 {} 条 Gateway 实体珍珠配方。", registeredRecipeCount);
        registration.addRecipes(GatewayEntityCategory.TYPE, recipes);
        registration.addRecipes(GatewayLootCategory.TYPE, GatewayLootCache.getRecipes());
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addTypedRecipeManagerPlugin(GatewayEntityCategory.TYPE, new GatewayItemLookupPlugin());
        registration.addTypedRecipeManagerPlugin(GatewayLootCategory.TYPE, new GatewayLootLookupPlugin());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JeiGatewaysPlugin.jeiRuntime = jeiRuntime;
        if (registeredRecipeCount > 0) {
            return;
        }

        GatewayEntityCache.clear();
        GatewayLootCache.clear();
        List<GatewayEntityRecipe> recipes = GatewayEntityCache.getRecipes();
        if (!recipes.isEmpty()) {
            JeiGateways.LOGGER.info("JEI Runtime 可用后补充注册了 {} 条 Gateway 实体珍珠配方。", recipes.size());
            jeiRuntime.getRecipeManager().addRecipes(GatewayEntityCategory.TYPE, recipes);
            jeiRuntime.getRecipeManager().addRecipes(GatewayLootCategory.TYPE, GatewayLootCache.getRecipes());
            registeredRecipeCount = recipes.size();
        }
        else {
            JeiGateways.LOGGER.warn("JEI Gateways 未找到可展示的 Gateway 实体珍珠配方。");
        }
    }

    public static boolean hasOtherRecipePages(ItemStack stack) {
        if (jeiRuntime == null) {
            return false;
        }

        IIngredientManager ingredientManager = jeiRuntime.getIngredientManager();
        Optional<IFocus<ItemStack>> focus = ingredientManager.createTypedIngredient(stack)
                .map(typed -> jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, typed))
                .flatMap(created -> created.checkedCast(VanillaTypes.ITEM_STACK));

        if (focus.isEmpty()) {
            return false;
        }

        Set<ResourceLocation> categoryIds = jeiRuntime.getRecipeManager()
                .createRecipeCategoryLookup()
                .limitFocus(List.of(focus.get()))
                .includeHidden()
                .get()
                .map(category -> category.getRecipeType().getUid())
                .filter(id -> !GatewayEntityCategory.TYPE.getUid().equals(id))
                .collect(Collectors.toSet());
        return !categoryIds.isEmpty();
    }

    private static final class GatewayItemLookupPlugin implements ISimpleRecipeManagerPlugin<GatewayEntityRecipe> {
        @Override
        public boolean isHandledInput(ITypedIngredient<?> ingredient) {
            return ingredient.getItemStack().isPresent();
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> ingredient) {
            return ingredient.getItemStack().isPresent();
        }

        @Override
        public List<GatewayEntityRecipe> getRecipesForInput(ITypedIngredient<?> ingredient) {
            return findMatchingRecipes(ingredient);
        }

        @Override
        public List<GatewayEntityRecipe> getRecipesForOutput(ITypedIngredient<?> ingredient) {
            return findMatchingRecipes(ingredient);
        }

        @Override
        public List<GatewayEntityRecipe> getAllRecipes() {
            return GatewayEntityCache.getRecipes();
        }

        private static List<GatewayEntityRecipe> findMatchingRecipes(ITypedIngredient<?> ingredient) {
            ItemStack stack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                return List.of();
            }
            List<GatewayEntityRecipe> directMatches = GatewayEntityCache.getRecipes(stack);
            if (directMatches.isEmpty()) {
                return List.of();
            }

            LinkedHashSet<GatewayEntityRecipe> expanded = new LinkedHashSet<>();
            for (GatewayEntityRecipe directMatch : directMatches) {
                for (GatewayEntityRecipe recipe : GatewayEntityCache.getRecipes()) {
                    if (recipe.gatewayId().equals(directMatch.gatewayId())
                            && ItemStack.isSameItemSameTags(recipe.pearl(), directMatch.pearl())) {
                        expanded.add(recipe);
                    }
                }
            }
            return List.copyOf(expanded);
        }
    }

    private static final class GatewayLootLookupPlugin implements ISimpleRecipeManagerPlugin<GatewayLootRecipe> {
        @Override
        public boolean isHandledInput(ITypedIngredient<?> ingredient) {
            return ingredient.getItemStack().isPresent();
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> ingredient) {
            return ingredient.getItemStack().isPresent();
        }

        @Override
        public List<GatewayLootRecipe> getRecipesForInput(ITypedIngredient<?> ingredient) {
            return findMatchingRecipes(ingredient);
        }

        @Override
        public List<GatewayLootRecipe> getRecipesForOutput(ITypedIngredient<?> ingredient) {
            return findMatchingRecipes(ingredient);
        }

        @Override
        public List<GatewayLootRecipe> getAllRecipes() {
            return GatewayLootCache.getRecipes();
        }

        private static List<GatewayLootRecipe> findMatchingRecipes(ITypedIngredient<?> ingredient) {
            ItemStack stack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                return List.of();
            }
            return GatewayLootCache.getRecipes(stack);
        }
    }
}
