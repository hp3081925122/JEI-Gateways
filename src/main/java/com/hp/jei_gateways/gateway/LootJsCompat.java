package com.hp.jei_gateways.gateway;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

final class LootJsCompat {
    private static final String MODID = "lootjs";

    private LootJsCompat() {
    }

    static void appendLootTableRewards(ResourceLocation lootTableId, Set<GatewayEntityCache.ItemStackKey> output) {
        if (!ModList.get().isLoaded(MODID)) {
            return;
        }

        try {
            for (Object action : getActions()) {
                if (!isTableModification(action)) {
                    continue;
                }
                if (!matchesLootTable(action, lootTableId)) {
                    continue;
                }
                collectActionOutputs(action, output);
            }
        }
        catch (ReflectiveOperationException ignored) {
        }
    }

    static void appendEntityRewards(EntityType<?> entityType, Set<GatewayEntityCache.ItemStackKey> output) {
        if (!ModList.get().isLoaded(MODID)) {
            return;
        }

        try {
            for (Object action : getActions()) {
                if (!isEntityModification(action)) {
                    continue;
                }
                if (!matchesEntity(action, entityType)) {
                    continue;
                }
                collectActionOutputs(action, output);
            }
        }
        catch (ReflectiveOperationException ignored) {
        }
    }

    private static List<?> getActions() throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName("com.almostreliable.lootjs.LootModificationsAPI");
        Field actionsField = apiClass.getDeclaredField("actions");
        actionsField.setAccessible(true);
        Object value = actionsField.get(null);
        return value instanceof List<?> list ? list : List.of();
    }

    private static boolean isTableModification(Object action) {
        return action.getClass().getName().equals("com.almostreliable.lootjs.core.LootModificationByTable");
    }

    private static boolean isEntityModification(Object action) {
        return action.getClass().getName().equals("com.almostreliable.lootjs.core.LootModificationByEntity");
    }

    @SuppressWarnings("unchecked")
    private static boolean matchesLootTable(Object action, ResourceLocation lootTableId) throws ReflectiveOperationException {
        Field filtersField = action.getClass().getDeclaredField("filters");
        filtersField.setAccessible(true);
        Object filters = filtersField.get(action);
        int length = Array.getLength(filters);
        for (int i = 0; i < length; i++) {
            Object filter = Array.get(filters, i);
            if (filter instanceof Predicate<?> predicate && ((Predicate<ResourceLocation>) predicate).test(lootTableId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesEntity(Object action, EntityType<?> entityType) throws ReflectiveOperationException {
        Field entitiesField = action.getClass().getDeclaredField("entities");
        entitiesField.setAccessible(true);
        Object value = entitiesField.get(action);
        return value instanceof Set<?> set && set.contains(entityType);
    }

    private static void collectActionOutputs(Object action, Set<GatewayEntityCache.ItemStackKey> output) throws ReflectiveOperationException {
        Class<?> compositeClass = Class.forName("com.almostreliable.lootjs.loot.action.CompositeLootAction");
        if (!compositeClass.isInstance(action)) {
            return;
        }

        Field handlersField = compositeClass.getDeclaredField("handlers");
        handlersField.setAccessible(true);
        Object handlers = handlersField.get(action);
        if (!(handlers instanceof Collection<?> collection)) {
            return;
        }

        for (Object handler : collection) {
            collectHandlerOutputs(handler, output);
        }
    }

    private static void collectHandlerOutputs(Object handler, Set<GatewayEntityCache.ItemStackKey> output) throws ReflectiveOperationException {
        if (handler == null) {
            return;
        }

        String className = handler.getClass().getName();
        switch (className) {
            case "com.almostreliable.lootjs.loot.action.AddLootAction" -> collectEntriesArray(handler, "entries", output);
            case "com.almostreliable.lootjs.loot.action.WeightedAddLootAction" -> collectWeightedEntries(handler, output);
            case "com.almostreliable.lootjs.loot.action.ReplaceLootAction" -> collectSingleEntry(handler, "lootEntry", output);
            case "com.almostreliable.lootjs.loot.action.GroupedLootAction" -> collectCompositeChildren(handler, output);
            default -> {
                if (isCompositeHandler(handler)) {
                    collectCompositeChildren(handler, output);
                }
            }
        }
    }

    private static boolean isCompositeHandler(Object handler) {
        Class<?> current = handler.getClass();
        while (current != null) {
            if (current.getName().equals("com.almostreliable.lootjs.loot.action.CompositeLootAction")) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static void collectCompositeChildren(Object handler, Set<GatewayEntityCache.ItemStackKey> output) throws ReflectiveOperationException {
        Class<?> compositeClass = Class.forName("com.almostreliable.lootjs.loot.action.CompositeLootAction");
        Field handlersField = compositeClass.getDeclaredField("handlers");
        handlersField.setAccessible(true);
        Object children = handlersField.get(handler);
        if (children instanceof Collection<?> collection) {
            for (Object child : collection) {
                collectHandlerOutputs(child, output);
            }
        }
    }

    private static void collectEntriesArray(Object handler, String fieldName, Set<GatewayEntityCache.ItemStackKey> output) throws ReflectiveOperationException {
        Field entriesField = handler.getClass().getDeclaredField(fieldName);
        entriesField.setAccessible(true);
        Object entries = entriesField.get(handler);
        int length = Array.getLength(entries);
        for (int i = 0; i < length; i++) {
            collectLootEntry(Array.get(entries, i), output);
        }
    }

    private static void collectSingleEntry(Object handler, String fieldName, Set<GatewayEntityCache.ItemStackKey> output) throws ReflectiveOperationException {
        Field entryField = handler.getClass().getDeclaredField(fieldName);
        entryField.setAccessible(true);
        collectLootEntry(entryField.get(handler), output);
    }

    private static void collectWeightedEntries(Object handler, Set<GatewayEntityCache.ItemStackKey> output) throws ReflectiveOperationException {
        Field weightedListField = handler.getClass().getDeclaredField("weightedRandomList");
        weightedListField.setAccessible(true);
        Object weightedList = weightedListField.get(handler);
        if (weightedList == null) {
            return;
        }

        Class<?> weightedListClass = weightedList.getClass();
        Field itemsField = findField(weightedListClass, "items");
        if (itemsField == null) {
            return;
        }
        itemsField.setAccessible(true);
        Object items = itemsField.get(weightedList);
        if (!(items instanceof List<?> list)) {
            return;
        }

        for (Object weightedEntry : list) {
            if (weightedEntry == null) {
                continue;
            }
            Field dataField = findField(weightedEntry.getClass(), "data");
            if (dataField == null) {
                continue;
            }
            dataField.setAccessible(true);
            collectLootEntry(dataField.get(weightedEntry), output);
        }
    }

    private static void collectLootEntry(Object lootEntry, Set<GatewayEntityCache.ItemStackKey> output) throws ReflectiveOperationException {
        if (lootEntry == null) {
            return;
        }

        Field generatorField = findField(lootEntry.getClass(), "generator");
        if (generatorField == null) {
            return;
        }
        generatorField.setAccessible(true);
        Object generator = generatorField.get(lootEntry);
        if (generator == null) {
            return;
        }

        Map<Item, ItemStack> seenItems = new IdentityHashMap<>();
        for (Field field : generator.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(generator);
            if (value instanceof ItemStack stack && !stack.isEmpty()) {
                seenItems.put(stack.getItem(), stack.copy());
            }
            else if (value instanceof net.minecraft.world.item.Item item) {
                seenItems.putIfAbsent(item, new ItemStack(item));
            }
        }

        for (ItemStack stack : seenItems.values()) {
            output.add(GatewayEntityCache.ItemStackKey.ofStack(stack));
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            }
            catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
