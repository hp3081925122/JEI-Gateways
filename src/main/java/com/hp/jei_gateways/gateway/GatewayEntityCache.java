package com.hp.jei_gateways.gateway;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hp.jei_gateways.JeiGateways;
import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.gate.WaveModifier;
import dev.shadowsoffire.gateways.gate.endless.EndlessGateway;
import dev.shadowsoffire.gateways.gate.endless.EndlessModifier;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class GatewayEntityCache {
    private static final Field STANDARD_WAVE_ENTITY_TYPE = findStandardWaveEntityTypeField();
    private static final Field STANDARD_WAVE_ENTITY_TAG = findStandardWaveEntityTagField();
    private static final Field STANDARD_WAVE_ENTITY_MODIFIERS = findStandardWaveEntityModifiersField();
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.##");
    private static final Map<EntityType<?>, List<GatewayEntityRecipe>> BY_ENTITY = new HashMap<>();
    private static final Map<ItemStackKey, List<GatewayEntityRecipe>> BY_ITEM = new HashMap<>();
    private static List<GatewayEntityRecipe> allRecipes = List.of();

    private GatewayEntityCache() {
    }

    public static List<GatewayEntityRecipe> getRecipes() {
        ensureBuilt();
        return allRecipes;
    }

    public static List<GatewayEntityRecipe> getRecipes(EntityType<?> entityType) {
        ensureBuilt();
        return BY_ENTITY.getOrDefault(entityType, List.of());
    }

    public static List<GatewayEntityRecipe> getRecipes(ItemStack stack) {
        ensureBuilt();
        if (stack.isEmpty()) {
            return List.of();
        }
        return BY_ITEM.getOrDefault(ItemStackKey.of(stack), List.of());
    }

    public static void clear() {
        BY_ENTITY.clear();
        BY_ITEM.clear();
        allRecipes = List.of();
    }

    public static void rebuild() {
        clear();
        ensureBuilt();
    }

    private static void ensureBuilt() {
        if (!allRecipes.isEmpty() || !BY_ENTITY.isEmpty()) {
            return;
        }

        Level level = Minecraft.getInstance().level;
        ResourceManager resourceManager = resolveResourceManager();
        LootTableResolver lootTableResolver = new LootTableResolver(resourceManager);
        GatewayTooltipResolver gatewayTooltipResolver = new GatewayTooltipResolver(resourceManager);
        Map<EntityType<?>, Set<ItemStackKey>> entityLootCache = new IdentityHashMap<>();
        Map<EntityType<?>, List<GatewayEntityRecipe>> byEntity = new HashMap<>();
        Map<ItemStackKey, List<GatewayEntityRecipe>> byItem = new HashMap<>();
        List<GatewayEntityRecipe> recipes = new ArrayList<>();
        List<ItemStack> pearls = sortedPearls();

        JeiGateways.LOGGER.info("Building gateway entity cache. levelPresent={}, pearlCount={}", level != null, pearls.size());

        for (ItemStack pearl : pearls) {
            Gateway gateway = GatePearlItem.getGate(pearl).getOptional().orElse(null);
            if (gateway == null) {
                JeiGateways.LOGGER.debug("Skipping pearl without bound gateway: {}", pearl);
                continue;
            }

            ResourceLocation gatewayId = GatePearlItem.getGate(pearl).getId();
            if (gatewayId == null) {
                JeiGateways.LOGGER.debug("Skipping pearl with null gateway id: {}", pearl);
                continue;
            }

            Component pearlTooltipText = gatewayTooltipResolver.resolveTooltip(gatewayId);

            List<WavePage> wavePages = pagesForGateway(gateway, level);
            if (wavePages.isEmpty()) {
                JeiGateways.LOGGER.info("Gateway {} produced no wave pages. levelPresent={}", gatewayId, level != null);
                continue;
            }

            GatewayRewards gatewayRewards = collectGatewayRewards(gateway, lootTableResolver, entityLootCache);
            LinkedHashSet<ItemStackKey> relatedItems = new LinkedHashSet<>(gatewayRewards.items());
            List<ItemStack> rewardStacks = materializeStacks(gatewayRewards.items());
            List<ItemStack> relatedStacks = materializeStacks(relatedItems);

            for (WavePage wavePage : wavePages) {
                for (GatewayEntityRecipe.LinkedEntity linkedEntity : wavePage.entities()) {
                    relatedItems.addAll(getEntityLootItems(linkedEntity.entityType(), lootTableResolver, entityLootCache));
                }
            }
            relatedStacks = materializeStacks(relatedItems);

            for (WavePage wavePage : wavePages) {
                GatewayEntityRecipe recipe = new GatewayEntityRecipe(
                        gatewayId,
                        pearl.copy(),
                        pearlTooltipText == null ? null : pearlTooltipText.copy(),
                        wavePage.waveLevel(),
                        wavePages.size(),
                        wavePage.entities().size(),
                        List.copyOf(wavePage.entities()),
                        List.copyOf(wavePage.modifiers()),
                        rewardStacks,
                        relatedStacks
                );
                recipes.add(recipe);
                for (GatewayEntityRecipe.LinkedEntity linkedEntity : wavePage.entities()) {
                    byEntity.computeIfAbsent(linkedEntity.entityType(), ignored -> new ArrayList<>()).add(recipe);
                }
                indexRecipeItems(byItem, recipe);
            }
        }

        recipes.sort(Comparator.comparing((GatewayEntityRecipe recipe) -> recipe.pearl().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(recipe -> recipe.gatewayId().toString())
                .thenComparingInt(GatewayEntityRecipe::waveLevel));
        byEntity.replaceAll((type, value) -> value.stream()
                .distinct()
                .sorted(Comparator.comparing(recipe -> recipe.gatewayId().toString()))
                .toList());
        byItem.replaceAll((key, value) -> value.stream()
                .distinct()
                .sorted(Comparator.comparing(recipe -> recipe.gatewayId().toString()))
                .toList());
        BY_ENTITY.putAll(byEntity);
        BY_ITEM.putAll(byItem);
        allRecipes = List.copyOf(recipes);
        JeiGateways.LOGGER.info("Built gateway entity cache with {} recipes, {} entity keys and {} item keys.", allRecipes.size(), BY_ENTITY.size(), BY_ITEM.size());
    }

    private static void indexRecipeItems(Map<ItemStackKey, List<GatewayEntityRecipe>> byItem, GatewayEntityRecipe recipe) {
        indexRecipeItem(byItem, recipe.pearl(), recipe);
        for (ItemStack relatedItem : recipe.relatedItems()) {
            indexRecipeItem(byItem, relatedItem, recipe);
        }
        for (ItemStack spawnEgg : recipe.spawnEggs()) {
            indexRecipeItem(byItem, spawnEgg, recipe);
        }
    }

    private static void indexRecipeItem(Map<ItemStackKey, List<GatewayEntityRecipe>> byItem, ItemStack stack, GatewayEntityRecipe recipe) {
        if (stack.isEmpty()) {
            return;
        }
        byItem.computeIfAbsent(ItemStackKey.of(stack), ignored -> new ArrayList<>()).add(recipe);
    }

    private static List<ItemStack> sortedPearls() {
        PearlOutput output = new PearlOutput();
        GatePearlItem.generateGatePearlStacks(output.list::add);
        return output.list.stream()
                .sorted(Comparator.comparing(stack -> GatePearlItem.getGate(stack).getId().toString()))
                .toList();
    }

    private static ItemStack findSpawnEgg(EntityType<?> entityType) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SpawnEggItem egg && egg.getType(new ItemStack(egg)) == entityType) {
                return new ItemStack(egg);
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<WavePage> pagesForGateway(Gateway gateway, Level level) {
        List<WavePage> pages = new ArrayList<>();
        if (gateway instanceof NormalGateway normalGateway) {
            int waveLevel = 1;
            for (Wave wave : normalGateway.waves()) {
                WavePage page = createWavePage(waveLevel++, wave.entities(), wave.modifiers(), level);
                if (page != null) {
                    pages.add(page);
                }
            }
        }
        else if (gateway instanceof EndlessGateway endlessGateway) {
            int waveLevel = 1;
            WavePage basePage = createWavePage(waveLevel++, endlessGateway.baseWave().entities(), endlessGateway.baseWave().modifiers(), level);
            if (basePage != null) {
                pages.add(basePage);
            }
            for (EndlessModifier modifier : endlessGateway.modifiers()) {
                WavePage page = createWavePage(waveLevel++, modifier.entities(), modifier.modifiers(), level);
                if (page != null) {
                    pages.add(page);
                }
            }
        }
        return List.copyOf(pages);
    }

    private static WavePage createWavePage(int waveLevel, List<WaveEntity> entities, List<WaveModifier> waveModifiers, Level level) {
        Map<EntityType<?>, EntityEntry> entries = new IdentityHashMap<>();
        for (WaveEntity waveEntity : entities) {
            EntityType<?> entityType = resolveEntityType(waveEntity, level);
            if (entityType == null) {
                continue;
            }

            ItemStack spawnEgg = findSpawnEgg(entityType);
            entries.merge(
                    entityType,
                    EntityEntry.from(entityType, spawnEgg, waveEntity, waveModifiers),
                    EntityEntry::merge
            );
        }
        if (entries.isEmpty()) {
            return null;
        }

        List<GatewayEntityRecipe.LinkedEntity> linkedEntities = entries.values().stream()
                .map(entry -> new GatewayEntityRecipe.LinkedEntity(
                        entry.entityType(),
                        entry.spawnEgg().copy(),
                        entry.count(),
                        List.copyOf(entry.modifiers())
                ))
                .sorted(Comparator.comparing(linked -> linked.displayName().getString(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        LinkedHashMap<String, Component> mergedModifiers = new LinkedHashMap<>();
        for (EntityEntry entry : entries.values()) {
            for (Component modifier : entry.modifiers()) {
                mergedModifiers.putIfAbsent(modifier.getString(), modifier);
            }
        }
        return new WavePage(waveLevel, linkedEntities, List.copyOf(mergedModifiers.values()));
    }

    private static GatewayRewards collectGatewayRewards(Gateway gateway, LootTableResolver lootTableResolver, Map<EntityType<?>, Set<ItemStackKey>> entityLootCache) {
        LinkedHashSet<ItemStackKey> items = new LinkedHashSet<>();
        if (gateway instanceof NormalGateway normalGateway) {
            for (Wave wave : normalGateway.waves()) {
                collectRewardItems(items, wave.rewards(), lootTableResolver, entityLootCache);
            }
            collectRewardItems(items, normalGateway.rewards(), lootTableResolver, entityLootCache);
        }
        else if (gateway instanceof EndlessGateway endlessGateway) {
            collectRewardItems(items, endlessGateway.baseWave().rewards(), lootTableResolver, entityLootCache);
            for (EndlessModifier modifier : endlessGateway.modifiers()) {
                collectRewardItems(items, modifier.rewards(), lootTableResolver, entityLootCache);
            }
        }
        return new GatewayRewards(Set.copyOf(items));
    }

    private static EntityType<?> resolveEntityType(WaveEntity waveEntity, Level level) {
        EntityType<?> type = resolveStoredEntityType(waveEntity);
        if (type != null) {
            return type;
        }
        if (level == null) {
            return null;
        }
        try {
            Entity entity = waveEntity.createEntity(level);
            return entity == null ? null : entity.getType();
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    private static EntityType<?> resolveStoredEntityType(WaveEntity waveEntity) {
        if (!isStandardWaveEntity(waveEntity, STANDARD_WAVE_ENTITY_TYPE)) {
            return null;
        }
        try {
            Object value = STANDARD_WAVE_ENTITY_TYPE.get(waveEntity);
            return value instanceof EntityType<?> type ? type : null;
        }
        catch (IllegalAccessException | IllegalArgumentException e) {
            return null;
        }
    }

    private static CompoundTag resolveStoredEntityTag(WaveEntity waveEntity) {
        if (!isStandardWaveEntity(waveEntity, STANDARD_WAVE_ENTITY_TAG)) {
            return null;
        }
        try {
            Object value = STANDARD_WAVE_ENTITY_TAG.get(waveEntity);
            if (value instanceof CompoundTag tag) {
                return tag;
            }
            if (value instanceof Optional<?> optional && optional.orElse(null) instanceof CompoundTag tag) {
                return tag;
            }
            return null;
        }
        catch (IllegalAccessException | IllegalArgumentException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<WaveModifier> resolveStoredEntityModifiers(WaveEntity waveEntity) {
        if (!isStandardWaveEntity(waveEntity, STANDARD_WAVE_ENTITY_MODIFIERS)) {
            return List.of();
        }
        try {
            Object value = STANDARD_WAVE_ENTITY_MODIFIERS.get(waveEntity);
            return value instanceof List<?> list ? (List<WaveModifier>) list : List.of();
        }
        catch (IllegalAccessException | IllegalArgumentException e) {
            return List.of();
        }
    }

    private static boolean isStandardWaveEntity(WaveEntity waveEntity, Field field) {
        return field != null && field.getDeclaringClass().isInstance(waveEntity);
    }

    private static Set<ItemStackKey> getEntityLootItems(EntityType<?> entityType, LootTableResolver lootTableResolver, Map<EntityType<?>, Set<ItemStackKey>> entityLootCache) {
        return entityLootCache.computeIfAbsent(entityType, type -> {
            var lootTableKey = type.getDefaultLootTable();
            if (lootTableKey == null) {
                return Set.of();
            }
            return lootTableResolver.resolveItems(lootTableKey.location());
        });
    }

    private static void collectRewardItems(Set<ItemStackKey> output, List<Reward> rewards, LootTableResolver lootTableResolver, Map<EntityType<?>, Set<ItemStackKey>> entityLootCache) {
        for (Reward reward : rewards) {
            collectRewardItem(output, reward, lootTableResolver, entityLootCache);
        }
    }

    private static void collectRewardItem(Set<ItemStackKey> output, Reward reward, LootTableResolver lootTableResolver, Map<EntityType<?>, Set<ItemStackKey>> entityLootCache) {
        if (reward instanceof Reward.StackReward stackReward) {
            addStack(output, stackReward.stack());
            return;
        }
        if (reward instanceof Reward.StackListReward stackListReward) {
            for (ItemStack stack : stackListReward.stacks()) {
                addStack(output, stack);
            }
            return;
        }
        if (reward instanceof Reward.LootTableReward lootTableReward) {
            output.addAll(lootTableResolver.resolveItems(lootTableReward.table()));
            LootJsCompat.appendLootTableRewards(lootTableReward.table(), output);
            return;
        }
        if (reward instanceof Reward.EntityLootReward entityLootReward) {
            output.addAll(getEntityLootItems(entityLootReward.type(), lootTableResolver, entityLootCache));
            LootJsCompat.appendEntityRewards(entityLootReward.type(), output);
            return;
        }
        if (reward instanceof Reward.CountedReward countedReward) {
            collectRewardItem(output, countedReward.reward(), lootTableResolver, entityLootCache);
            return;
        }
        if (reward instanceof Reward.ChancedReward chancedReward) {
            collectRewardItem(output, chancedReward.reward(), lootTableResolver, entityLootCache);
            return;
        }
    }

    private static ResourceManager resolveResourceManager() {
        Minecraft minecraft = Minecraft.getInstance();
        MinecraftServer singleplayerServer = minecraft.getSingleplayerServer();
        if (singleplayerServer != null) {
            return singleplayerServer.getResourceManager();
        }
        return minecraft.getResourceManager();
    }

    private static String fileName(String path) {
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private static ResourceLocation toGatewayId(ResourceLocation resourceId) {
        String path = resourceId.getPath();
        if (!path.startsWith("gateways/") || !path.endsWith(".json")) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(resourceId.getNamespace(), path.substring("gateways/".length(), path.length() - ".json".length()));
    }

    private static ResourceLocation toLootTableId(ResourceLocation resourceId) {
        String path = resourceId.getPath();
        String prefix;
        if (path.startsWith("loot_table/")) {
            prefix = "loot_table/";
        }
        else if (path.startsWith("loot_tables/")) {
            prefix = "loot_tables/";
        }
        else {
            return null;
        }
        if (!path.endsWith(".json")) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(resourceId.getNamespace(), path.substring(prefix.length(), path.length() - ".json".length()));
    }

    private static LootTableDefinition parseLootTableDefinition(JsonObject root) {
        LinkedHashSet<ItemStackKey> items = new LinkedHashSet<>();
        LinkedHashSet<ResourceLocation> references = new LinkedHashSet<>();
        parseLootElement(root.get("pools"), items, references);
        return new LootTableDefinition(Set.copyOf(items), Set.copyOf(references));
    }

    private static void parseLootElement(JsonElement element, Set<ItemStackKey> items, Set<ResourceLocation> references) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            JsonArray jsonArray = element.getAsJsonArray();
            for (JsonElement child : jsonArray) {
                parseLootElement(child, items, references);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject object = element.getAsJsonObject();
        String type = getString(object, "type");
        if ("minecraft:item".equals(type)) {
            ResourceLocation itemId = parseResourceLocation(getString(object, "name"));
            if (itemId != null) {
                Item item = BuiltInRegistries.ITEM.get(itemId);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    items.add(ItemStackKey.of(new ItemStack(item)));
                }
            }
        }
        else if ("minecraft:loot_table".equals(type)) {
            ResourceLocation lootTableId = parseResourceLocation(getString(object, "name"));
            if (lootTableId != null) {
                references.add(lootTableId);
            }
        }
        else if ("minecraft:tag".equals(type)) {
            ResourceLocation tagId = parseResourceLocation(getString(object, "name"));
            if (tagId != null) {
                addTagItems(items, tagId);
            }
        }

        parseLootElement(object.get("entries"), items, references);
        parseLootElement(object.get("children"), items, references);
        parseLootElement(object.get("pools"), items, references);
    }

    private static void addTagItems(Set<ItemStackKey> items, ResourceLocation tagId) {
        TagKey<Item> itemTag = TagKey.create(Registries.ITEM, tagId);
        Optional<? extends net.minecraft.core.HolderSet.Named<Item>> optionalTag = BuiltInRegistries.ITEM.getTag(itemTag);
        if (optionalTag.isEmpty()) {
            return;
        }
        optionalTag.get().forEach(holder -> items.add(ItemStackKey.of(new ItemStack(holder.value()))));
    }

    private static List<ItemStack> materializeStacks(Collection<ItemStackKey> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .map(ItemStackKey::toStack)
                .filter(stack -> !stack.isEmpty())
                .sorted(Comparator.comparing(stack -> stack.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static void addStack(Set<ItemStackKey> items, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        items.add(ItemStackKey.of(stack));
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static ResourceLocation parseResourceLocation(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(id);
    }

    private static Field findStandardWaveEntityTypeField() {
        try {
            Class<?> standardWaveEntity = Class.forName("dev.shadowsoffire.gateways.gate.StandardWaveEntity");
            Field field = standardWaveEntity.getDeclaredField("type");
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException e) {
            JeiGateways.LOGGER.warn("Failed to resolve StandardWaveEntity.type field", e);
            return null;
        }
    }

    private static Field findStandardWaveEntityTagField() {
        try {
            Class<?> standardWaveEntity = Class.forName("dev.shadowsoffire.gateways.gate.StandardWaveEntity");
            Field field = standardWaveEntity.getDeclaredField("tag");
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException e) {
            JeiGateways.LOGGER.warn("Failed to resolve StandardWaveEntity.tag field", e);
            return null;
        }
    }

    private static Field findStandardWaveEntityModifiersField() {
        try {
            Class<?> standardWaveEntity = Class.forName("dev.shadowsoffire.gateways.gate.StandardWaveEntity");
            Field field = standardWaveEntity.getDeclaredField("modifiers");
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException e) {
            JeiGateways.LOGGER.warn("Failed to resolve StandardWaveEntity.modifiers field", e);
            return null;
        }
    }

    private static List<Component> buildModifiers(List<WaveModifier> waveModifiers, CompoundTag entityTag) {
        LinkedHashMap<String, Component> modifiers = new LinkedHashMap<>();
        for (WaveModifier modifier : waveModifiers) {
            List<Component> tooltipLines = new ArrayList<>();
            modifier.appendHoverText(Item.TooltipContext.of(Minecraft.getInstance().level), tooltipLines::add);
            for (Component tooltipLine : tooltipLines) {
                modifiers.putIfAbsent(tooltipLine.getString(), tooltipLine);
            }
        }
        collectTagModifiers(modifiers, entityTag);
        return List.copyOf(modifiers.values());
    }

    private static void collectTagModifiers(Map<String, Component> modifiers, CompoundTag entityTag) {
        if (entityTag == null || entityTag.isEmpty()) {
            return;
        }

        if (entityTag.getBoolean("Glowing")) {
            putModifier(modifiers, Component.translatable("jei.jei_gateways.modifier.glowing"));
        }

        if (entityTag.contains("Health")) {
            putModifier(modifiers, Component.translatable("jei.jei_gateways.modifier.health", NUMBER_FORMAT.format(entityTag.getFloat("Health"))));
        }

        if (entityTag.contains("ActiveEffects", CompoundTag.TAG_LIST)) {
            entityTag.getList("ActiveEffects", CompoundTag.TAG_COMPOUND).forEach(tag -> {
                if (!(tag instanceof CompoundTag effectTag)) {
                    return;
                }
                String effectId = effectTag.contains("Id") ? effectTag.getString("Id") : effectTag.getString("forge:id");
                if (effectId.isBlank()) {
                    return;
                }
                ResourceLocation effectKey = ResourceLocation.tryParse(effectId);
                MobEffect effect = effectKey == null ? null : BuiltInRegistries.MOB_EFFECT.get(effectKey);
                Component effectName = effect == null ? Component.literal(effectId) : Component.translatable(effect.getDescriptionId());
                int amplifier = effectTag.getInt("Amplifier") + 1;
                putModifier(modifiers, Component.translatable("jei.jei_gateways.modifier.effect", effectName, amplifier));
            });
        }
    }

    private static void putModifier(Map<String, Component> modifiers, Component component) {
        modifiers.putIfAbsent(component.getString(), component);
    }

    private static final class PearlOutput implements CreativeModeTab.Output {
        private final NonNullList<ItemStack> list = NonNullList.create();

        @Override
        public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
            list.add(stack);
        }
    }

    private record EntityEntry(EntityType<?> entityType, ItemStack spawnEgg, List<Component> modifiers, int count) {
        private static EntityEntry from(EntityType<?> entityType, ItemStack spawnEgg, WaveEntity waveEntity, List<WaveModifier> waveModifiers) {
            List<WaveModifier> allModifiers = new ArrayList<>(waveModifiers);
            allModifiers.addAll(resolveStoredEntityModifiers(waveEntity));
            return new EntityEntry(entityType, spawnEgg.copy(), buildModifiers(allModifiers, resolveStoredEntityTag(waveEntity)), waveEntity.getCount());
        }

        private EntityEntry merge(EntityEntry other) {
            List<Component> mergedModifiers = new ArrayList<>(modifiers);
            for (Component modifier : other.modifiers) {
                if (mergedModifiers.stream().noneMatch(existing -> existing.getString().equals(modifier.getString()))) {
                    mergedModifiers.add(modifier);
                }
            }
            ItemStack mergedEgg = spawnEgg.isEmpty() ? other.spawnEgg.copy() : spawnEgg.copy();
            return new EntityEntry(entityType, mergedEgg, List.copyOf(mergedModifiers), count + other.count);
        }
    }

    private record GatewayRewards(Set<ItemStackKey> items) {
    }

    private record WavePage(int waveLevel, List<GatewayEntityRecipe.LinkedEntity> entities, List<Component> modifiers) {
    }

    private record LootTableDefinition(Set<ItemStackKey> directItems, Set<ResourceLocation> references) {
    }

    private record GatewayTooltipDefinition(String tooltipKey, String tooltipText) {
    }

    private static final class LootTableResolver {
        private final ResourceManager resourceManager;
        private final Map<ResourceLocation, ResourceLocation> resourceIdsByLootTableId;
        private final Map<ResourceLocation, LootTableDefinition> definitions = new HashMap<>();
        private final Map<ResourceLocation, Set<ItemStackKey>> resolvedItems = new HashMap<>();
        private final Map<ResourceLocation, Optional<ResourceLocation>> canonicalIds = new HashMap<>();

        private LootTableResolver(ResourceManager resourceManager) {
            this.resourceManager = resourceManager;
            this.resourceIdsByLootTableId = indexLootTableResources(resourceManager);
        }

        private Set<ItemStackKey> resolveItems(ResourceLocation lootTableId) {
            Optional<ResourceLocation> canonical = canonicalIds.computeIfAbsent(lootTableId, this::findCanonicalLootTableId);
            if (canonical.isEmpty()) {
                return Set.of();
            }
            return resolveItems(canonical.get(), new HashSet<>());
        }

        private Set<ItemStackKey> resolveItems(ResourceLocation canonicalLootTableId, Set<ResourceLocation> visiting) {
            Set<ItemStackKey> cached = resolvedItems.get(canonicalLootTableId);
            if (cached != null) {
                return cached;
            }
            if (!visiting.add(canonicalLootTableId)) {
                return Set.of();
            }

            LootTableDefinition definition = definitions.computeIfAbsent(canonicalLootTableId, this::loadDefinition);
            if (definition == null) {
                visiting.remove(canonicalLootTableId);
                return Set.of();
            }

            LinkedHashSet<ItemStackKey> result = new LinkedHashSet<>(definition.directItems());
            for (ResourceLocation child : definition.references()) {
                Optional<ResourceLocation> canonicalChild = canonicalIds.computeIfAbsent(child, this::findCanonicalLootTableId);
                canonicalChild.ifPresent(resourceLocation -> result.addAll(resolveItems(resourceLocation, visiting)));
            }
            visiting.remove(canonicalLootTableId);
            Set<ItemStackKey> immutable = Set.copyOf(result);
            resolvedItems.put(canonicalLootTableId, immutable);
            return immutable;
        }

        private LootTableDefinition loadDefinition(ResourceLocation lootTableId) {
            ResourceLocation resourceId = resourceIdsByLootTableId.get(lootTableId);
            if (resourceId == null) {
                return null;
            }
            Optional<Resource> resource = resourceManager.getResource(resourceId);
            if (resource.isEmpty()) {
                return null;
            }
            try (Reader reader = resource.get().openAsReader()) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                if (!jsonElement.isJsonObject()) {
                    return null;
                }
                return parseLootTableDefinition(jsonElement.getAsJsonObject());
            }
            catch (IOException ignored) {
                return null;
            }
        }

        private Optional<ResourceLocation> findCanonicalLootTableId(ResourceLocation requestedLootTableId) {
            if (resourceIdsByLootTableId.containsKey(requestedLootTableId)) {
                return Optional.of(requestedLootTableId);
            }

            String targetPath = requestedLootTableId.getPath();
            return resourceIdsByLootTableId.keySet().stream()
                    .filter(knownId -> knownId.getNamespace().equals(requestedLootTableId.getNamespace()))
                    .filter(knownId -> {
                        String knownPath = knownId.getPath();
                        return knownPath.equals(targetPath)
                                || knownPath.endsWith("/" + targetPath)
                                || targetPath.endsWith("/" + knownPath)
                                || fileName(knownPath).equals(fileName(targetPath));
                    })
                    .findFirst();
        }

        private static Map<ResourceLocation, ResourceLocation> indexLootTableResources(ResourceManager resourceManager) {
            Map<ResourceLocation, ResourceLocation> indexed = new HashMap<>();
            for (String root : List.of("loot_table", "loot_tables")) {
                Map<ResourceLocation, Resource> resources = resourceManager.listResources(root, path -> path.getPath().endsWith(".json"));
                for (ResourceLocation resourceId : resources.keySet()) {
                    ResourceLocation lootTableId = toLootTableId(resourceId);
                    if (lootTableId != null) {
                        indexed.putIfAbsent(lootTableId, resourceId);
                    }
                }
            }
            return indexed;
        }
    }

    private static final class GatewayTooltipResolver {
        private final ResourceManager resourceManager;
        private final Map<ResourceLocation, ResourceLocation> resourceIdsByGatewayId;
        private final Map<ResourceLocation, GatewayTooltipDefinition> definitions = new HashMap<>();
        private final Map<ResourceLocation, Optional<ResourceLocation>> canonicalIds = new HashMap<>();

        private GatewayTooltipResolver(ResourceManager resourceManager) {
            this.resourceManager = resourceManager;
            this.resourceIdsByGatewayId = indexGatewayResources(resourceManager);
        }

        private Component resolveTooltip(ResourceLocation gatewayId) {
            String dynamicTooltipText = GatewaysJsCompat.getTooltipText(gatewayId);
            if (dynamicTooltipText != null) {
                return Component.literal(dynamicTooltipText);
            }

            String dynamicTooltipKey = GatewaysJsCompat.getTooltipKey(gatewayId);
            if (dynamicTooltipKey != null) {
                return Component.translatable(dynamicTooltipKey);
            }

            GatewayTooltipDefinition definition = getDefinition(gatewayId);
            if (definition == null) {
                return null;
            }
            if (definition.tooltipText() != null) {
                return Component.literal(definition.tooltipText());
            }
            if (definition.tooltipKey() != null) {
                return Component.translatable(definition.tooltipKey());
            }
            return null;
        }

        private GatewayTooltipDefinition getDefinition(ResourceLocation gatewayId) {
            Optional<ResourceLocation> canonical = canonicalIds.computeIfAbsent(gatewayId, this::findCanonicalGatewayId);
            return canonical.map(id -> definitions.computeIfAbsent(id, this::loadDefinition)).orElse(null);
        }

        private GatewayTooltipDefinition loadDefinition(ResourceLocation gatewayId) {
            ResourceLocation resourceId = resourceIdsByGatewayId.get(gatewayId);
            if (resourceId == null) {
                return null;
            }
            Optional<Resource> resource = resourceManager.getResource(resourceId);
            if (resource.isEmpty()) {
                return null;
            }
            try (Reader reader = resource.get().openAsReader()) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                if (!jsonElement.isJsonObject()) {
                    return null;
                }
                JsonObject root = jsonElement.getAsJsonObject();
                String tooltipText = emptyToNull(getString(root, "tooltipText"));
                String tooltipKey = emptyToNull(getString(root, "tooltipKey"));
                return new GatewayTooltipDefinition(tooltipKey, tooltipText);
            }
            catch (IOException ignored) {
                return null;
            }
        }

        private Optional<ResourceLocation> findCanonicalGatewayId(ResourceLocation requestedGatewayId) {
            if (resourceIdsByGatewayId.containsKey(requestedGatewayId)) {
                return Optional.of(requestedGatewayId);
            }

            String targetPath = requestedGatewayId.getPath();
            return resourceIdsByGatewayId.keySet().stream()
                    .filter(knownId -> knownId.getNamespace().equals(requestedGatewayId.getNamespace()))
                    .filter(knownId -> {
                        String knownPath = knownId.getPath();
                        return knownPath.equals(targetPath)
                                || knownPath.endsWith("/" + targetPath)
                                || targetPath.endsWith("/" + knownPath)
                                || fileName(knownPath).equals(fileName(targetPath));
                    })
                    .findFirst();
        }

        private static Map<ResourceLocation, ResourceLocation> indexGatewayResources(ResourceManager resourceManager) {
            Map<ResourceLocation, ResourceLocation> indexed = new HashMap<>();
            Map<ResourceLocation, Resource> resources = resourceManager.listResources("gateways", path -> path.getPath().endsWith(".json"));
            for (ResourceLocation resourceId : resources.keySet()) {
                ResourceLocation gatewayId = toGatewayId(resourceId);
                if (gatewayId != null) {
                    indexed.put(gatewayId, resourceId);
                }
            }
            return indexed;
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public static final class ItemStackKey {
        private final ResourceLocation itemId;
        private final ItemStack stack;
        private final int hash;

        private ItemStackKey(ItemStack stack) {
            this.itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            this.stack = stack.copy();
            this.hash = 31 * itemId.hashCode() + this.stack.getComponents().hashCode();
        }

        private static ItemStackKey of(ItemStack stack) {
            return new ItemStackKey(stack);
        }

        public static ItemStackKey ofStack(ItemStack stack) {
            return of(stack);
        }

        public ItemStack toStack() {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                return ItemStack.EMPTY;
            }
            return stack.copy();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof ItemStackKey other
                    && itemId.equals(other.itemId)
                    && ItemStack.isSameItemSameComponents(stack, other.stack);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return itemId + "|" + stack.getComponents();
        }
    }
}
