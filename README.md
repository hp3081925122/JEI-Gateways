# JEI Gateways

## 中文介绍

`JEI Gateways` 是一个为 `Gateways To Eternity` 与 `JEI` 提供联动的附属模组，主要目标是把传送门珍珠、波次实体、奖励物品和战利品查询整合到同一套 JEI 交互里，让玩家不用翻配置文件，也不用额外记忆每颗珍珠对应的内容。

这个模组最核心的用途，是帮助玩家快速回答两类问题：

1. 这个实体会出现在什么传送门珍珠里？
2. 这个奖励物品、掉落物或战利品，可能来自哪些传送门珍珠？

---

### 主要功能

#### 1. 实体珍珠查询

当你在 JEI 中查询某个刷怪蛋，或从相关物品反查时，模组会展示包含该实体的 Gateway 传送门珍珠页面。  
每个页面对应一颗珍珠，并显示该珍珠的基础信息，方便你快速判断目标是否来自你想找的传送门。

#### 2. 传送门战利品页面

模组新增了独立的 `永恒之门战利品` JEI 页面，不依赖 `AdvancedLootInfo`。  
你可以直接查看某颗传送门珍珠可能提供的奖励物品，并在同一颗珍珠存在多页奖励时使用 JEI 原生分页继续翻页查看。

#### 3. 奖励物品反查珍珠

当你在 JEI 中点击某个奖励物品时，模组会尝试反查并显示可能产出该物品的传送门珍珠。  
这让“从掉落反推来源”变得非常直接，特别适合大型整合包或大量 KubeJS 自定义 Gateway 场景。

#### 4. Loot Table 支持

模组支持读取 `Gateways To Eternity` 奖励中的：

- 直接物品奖励
- 物品列表奖励
- `gateways:loot_table`
- `gateways:entity_loot`

同时也会递归解析 loot table 对其他 loot table 的引用，以及常见的物品标签条目。

#### 5. LootJS 可选兼容

如果环境中安装了 `LootJS`，模组会额外尝试读取 LootJS 的运行时掉落修改信息，用于补充传送门奖励与战利品反查结果。  
这意味着在大量使用 KubeJS 或 LootJS 修改掉落的整合包中，查询结果会比单纯的静态资源扫描更接近实际游戏表现。

#### 6. 缓存与性能优化

模组包含专门的缓存层，用于避免在每次打开 JEI 时重复构建同样的数据。  
当前实现会尽量把解析与索引构建集中到资源更新后，并在物品查询时直接走缓存索引，从而减少不必要的遍历和卡顿。

---

### 设计目标

这个模组并不是想取代 `Gateways To Eternity` 本体，也不是想做一个完整的掉落百科。  
它更偏向于一个“JEI 内的传送门查询工具”，专注解决下面这些实际问题：

- 某个怪物属于哪颗珍珠？
- 某个战利品是否来自某个 Gateway？
- 某颗珍珠大概会给什么奖励？
- 在大量 KubeJS / LootJS 自定义内容下，如何更快定位对应传送门？

---

### 适用场景

这个模组尤其适合以下环境：

- 使用了大量 `Gateways To Eternity` 自定义传送门的整合包
- 使用了 `KubeJS` 编写 Gateway 与 loot table 的整合包
- 希望通过 JEI 统一查询传送门实体与奖励来源的服务器或单机环境
- 不想额外依赖 `AdvancedLootInfo`，但仍然希望拥有传送门奖励查询能力的玩家

---

### 当前依赖

运行本模组至少需要：

- Minecraft 1.20.1
- Forge
- JEI
- Gateways To Eternity
- Placebo

`LootJS` 兼容是可选的，不是硬前置。  
如果未安装 `LootJS`，模组仍然可以正常工作，只是不会额外读取 LootJS 的运行时掉落修改结果。

---

### 总结

`JEI Gateways` 的目标很明确：  
把 `Gateways To Eternity` 里最不好查、最容易忘、最依赖配置和脚本的那部分信息，尽量变成玩家在 JEI 里就能直接看到、直接反查、直接定位的内容。

如果你的整合包里有大量自定义传送门、复杂奖励池和脚本化掉落逻辑，这个模组会明显降低玩家理解和检索这些内容的成本。

---

## English Description

`JEI Gateways` is an addon that connects `Gateways To Eternity` with `JEI`.  
Its main purpose is to make gateway pearls, wave entities, reward items, and loot-source lookup available directly inside JEI, so players do not need to inspect config files or remember what each pearl contains.

The mod focuses on answering two very practical questions:

1. Which gateway pearls can contain this entity?
2. Which gateway pearls may provide this reward item, drop, or loot-table result?

---

### Main Features

#### 1. Entity-to-Pearl Lookup

When you query a spawn egg in JEI, or reverse-search from related items, the mod can show gateway pearl pages that include that entity.  
Each page represents one pearl, making it much easier to locate the right gateway in large modpacks.

#### 2. Dedicated Gateway Loot Page

The mod adds its own `Gateway Loot` JEI category and does not require `AdvancedLootInfo`.  
You can directly inspect the potential rewards of a gateway pearl, and if a pearl has multiple reward pages, you can continue browsing through normal JEI pagination.

#### 3. Reward Item Reverse Lookup

Clicking a reward item in JEI can return the gateway pearls that may produce it.  
This is especially useful in heavily customized packs where players want to reverse-search loot sources instead of manually checking scripts and datapacks.

#### 4. Loot Table Support

The mod supports gateway rewards based on:

- direct item rewards
- stack list rewards
- `gateways:loot_table`
- `gateways:entity_loot`

It also recursively resolves loot table references and common item-tag entries.

#### 5. Optional LootJS Compatibility

If `LootJS` is installed, the mod additionally attempts to read LootJS runtime loot modifications in order to improve reward lookup and reverse-search accuracy.  
This makes the result closer to actual in-game behavior in packs that heavily rely on KubeJS or LootJS.

#### 6. Cache and Performance Optimization

The mod includes a dedicated cache layer to avoid rebuilding the same lookup data every time JEI is opened.  
It tries to rebuild indexes during resource updates and then serve item queries directly from cache, which helps reduce repeated scanning and unnecessary lag.

---

### Design Goals

This mod is not meant to replace `Gateways To Eternity`, and it is not intended to be a full loot encyclopedia.  
Instead, it is designed as a focused JEI-side query tool for gateway-related content, solving problems like:

- Which pearl contains this monster?
- Does this reward item come from a specific gateway?
- What kind of loot can this pearl provide?
- How can players navigate complex KubeJS or LootJS gateway content more easily?

---

### Best Use Cases

`JEI Gateways` is especially useful for:

- modpacks with many custom `Gateways To Eternity` pearls
- packs that define gateways and loot tables through `KubeJS`
- servers or singleplayer setups where players need a unified JEI workflow for gateway entities and rewards
- players who want gateway reward lookup without requiring `AdvancedLootInfo`

---

### Required Dependencies

This mod requires:

- Minecraft 1.20.1
- Forge
- JEI
- Gateways To Eternity
- Placebo

`LootJS` support is optional and not a hard dependency.  
If LootJS is not installed, the mod still works normally, but it will not include LootJS runtime loot modifications in its lookup results.

---

### Summary

`JEI Gateways` aims to turn the most script-heavy, config-heavy, and otherwise hard-to-track parts of `Gateways To Eternity` into something players can inspect directly inside JEI.

If your pack contains many custom gateway pearls, complex reward pools, and scripted loot behavior, this mod helps players understand and locate that content much more quickly.
