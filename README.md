# Re:JAOPCA Singularities

[Re:JAOPCA Singularities](https://github.com/TheLMiffy1111/JAOPCASingularities) 的 26.1.2 移植版：
为每个 JAOPCA 材料添加一个 [Re:Avaritia](https://github.com/cnlimiter/Re-Avaritia) 奇点。

对于每个材料（Avaritia 本身已有奇点的、被固定排除的、或被配置排除的材料除外），本模组会：

* 注册奇点 `rejaopcasingularities:<材料名>`，颜色由材料名稳定推导（也可以用材料配置里的
  `general.color` 指定）；
* 添加一条中子压缩机配方，把该材料的物品（锭、宝石、晶体或粉末，与 Avaritia 自带奇点一致）
  压缩成奇点；
* 自动把奇点加入无尽催化剂和永恒奇点配方——Re:Avaritia 会收集所有带输入配料的奇点，不需要额外处理。

## 依赖

| 模组 | 版本 |
| --- | --- |
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.122 或更高 |
| JAOPCA | 26.1.2-6.0.2.2 或更高 |
| Re:Avaritia | 26.1.2-1.4.2 或更高 |

## 固定的排除列表

下面这些材料**永远**不会生成奇点，写死在 `ReAvaritiaSingularityModule.ALWAYS_SKIPPED` 里，并通过
`getDefaultMaterialBlacklist()` 交给 JAOPCA 在材料计算阶段过滤，配置文件覆盖不了：

- Avaritia 自带奇点的材料：`iron`、`gold`、`copper`、`coal`、`diamond`、`emerald`、`quartz`、
  `redstone`、`glowstone`、`netherite`、`obsidian`、`blue_ice`、`aluminum`、`tin`、`bronze`、
  `silver`、`lead`、`steel`、`nickel`、`electrum`、`invar`、`platinum`、`uranium`、`osmium`、
  `refined_obsidian`；
- 名称与奇点 id 不同的两个：`amethyst`（Avaritia 的奇点是 `amethyst_shard`）、`lapis`
  （Avaritia 的奇点是 `lapis_lazuli`）；
- 作者额外排除的：`neutron` / `neutronium`、`netherite_scrap`、`packed_ice`、`ice`、`otherworld_wood`。

`steel` 虽然在 Avaritia 的联动奇点名单里，但当前整合包加载不到官方版，所以放进了下面的强制列表。

## 强制生成列表

下面这些材料**即使命中排除列表也会生成奇点**（`ReAvaritiaSingularityModule.FORCED`），并且允许从全部
JAOPCA 材料里查找、材料类型对应的标签不存在时按 `ingots` / `gems` / `crystals` / `dusts` 顺序再找一次：

| 材料名 | 中文名 |
| --- | --- |
| `resin` | 树脂 |
| `demonite_trim` | 狱锻纹饰 |
| `spirit_attuned` | 魔灵同调 |
| `iesnium` | 艾瑟 |
| `dark_steel` | 玄钢 |
| `steel` | 钢 |
| `bronze` | 青铜 |
| `quartz_enriched_copper` | 富石英铜 |
| `quartz_enriched_iron` | 富石英铁 |

模块配置的 `general.singularityBlacklist` 仍然可以把它们排除掉。找不到材料时日志里会出现
`Forced singularity material ... was not found`。

## 奇点压缩（永恒奇点）

Re:Avaritia 自带的永恒奇点配方要求把**所有**奇点各放一个到无尽合成台，而合成台只有 81 格；奇点数量超过
81 个时这个配方永远合不出来。本模组按 1.12.2 版 Eternal Singularity 的做法补上压缩流程：

1. 奇点按 id 排序，每 9 个一组；
2. 第 N 组用 9 个一级奇点合成 1 个**复合奇点**，配方在无尽合成台的最低等级（tier 1）；
3. 永恒奇点配方被替换为「每个复合奇点各 1 个 → 永恒奇点」，仍在无尽合成台（tier 4）合成。

复合奇点沿用 1.12.2 版的命名，每种一个独立物品，id 是 `<名字>_singularity`（Minecraft 的资源 id 只能
使用小写字母、数字和下划线，因此不是驼峰写法），例如：

| 组号 | 物品 id | 英文名 | 中文名 |
| --- | --- | --- | --- |
| 0 | `nitronic_singularity` | Nitronic Singularity | 氮化奇点 |
| 1 | `psychotic_singularity` | Psychotic Singularity | 癫狂奇点 |
| 2 | `sphaghettic_singularity` | Sphaghettic Singularity | 意面奇点 |
| … | … | … | … |
| 15 | `chronic_singularity` | Chronic Singularity | 惯习奇点 |

共 81 个名字（`COMPOUND_SINGULARITY_NAMES`），中英文各有 81 个翻译；除前 16 个沿用 1.12.2 版的中文名外，
其余中文名由本模组补译。贴图直接使用 1.12.2 版 Eternal Singularity 的素材，按蛇形物品 id 命名（如 `nitronic_singularity.png`、
`nitronic_singularity_overlay.png`，含动画帧），每个复合奇点有独立的模型。

奇点不超过 81 个时不会压缩，Avaritia 原配方保持不变。压缩配方在 `RegisterRecipesEvent` 里按当前奇点
列表生成，所以加装/卸载模组或 `/reload` 后会自动重算。如果启动时发现输入标签为空的奇点而被移除，
模组会自动重载一次数据包，让压缩配方按清理后的列表重新生成。

## 空标签奇点的清理

服务器启动、物品标签绑定之后，模组会检查每个已注册奇点的输入标签。标签为空的奇点既做不出配方，又会被
无尽催化剂和永恒奇点要求，所以会被移除并重建快照，日志形如：

```
Removing singularity black_quartz as its ingredient tag c:gems/black_quartz is empty
Removed 3 singularities with empty ingredient tags
```

如果 `c:` 标签为空但旧约定的 `forge:` 标签有内容，日志会额外提示 `old tag convention`。

## 配置

模块配置写在 `config/jaopca/modules/avaritia_singularity.toml`：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `general.count` | `1000` | 合成一个奇点默认需要的物品数量 |
| `general.timeCost` | `-1` | 默认压缩时间（刻），`-1` 表示沿用 Avaritia 的数值 |
| `general.singularityBlacklist` | 空 | **额外**排除的材料；固定排除列表不受这里影响 |

配置注释是可以本地化的：注释文本放在 `assets/rejaopcasingularities/lang/` 里（目前提供 `en_us`、
`zh_cn`、`zh_tw`），启动时按当前游戏语言写入 TOML；专用服务器等没有加载语言文件的环境回退到英文。
配置在模组构造阶段先写一次，进入 common setup 后会用当前语言重写注释，配置值本身不会改变。

单个材料可以在 `config/jaopca/materials/<材料名>.toml` 里单独覆盖：

| 配置项 | 说明 |
| --- | --- |
| `avaritia_singularity.count` | 该材料的奇点需要的物品数量 |
| `avaritia_singularity.timeCost` | 该材料的奇点压缩时间（刻） |

> 已经存在的配置文件不会被新的默认值覆盖。如果之前跑过旧版本，建议删掉
> `config/jaopca/modules/avaritia_singularity.toml`（以及各材料 TOML 里的 `avaritia_singularity.*` 段），
> 让默认值重新生成。

## 与 1.12.2 版本的差异

Re:Avaritia 现在不再「一个奇点一个物品类」。奇点变成了可重载的数据对象，所有奇点共用
`avaritia:singularity` 物品并靠 `avaritia:singularity_id` 数据组件区分，所以本模组不再注册物品、
模型和贴图。压缩机配方改为以数据包 JSON 的形式注入，而不是写进配方管理器；无尽催化剂配方也会自己
收集所有奇点，1.12.2 里那种修改催化剂配料的逻辑不再需要。

另外，输入从「存储方块」改成了材料本身（锭、宝石等），与 Re:Avaritia 自带奇点保持一致，因此不再
要求材料有存储方块。

配方加载阶段物品标签还没有绑定，Avaritia 只会为「输入配料非空」的奇点生成压缩机配方，因此本模组
自己提供配方 JSON，并给奇点配一个惰性解析的输入配料，避免把空标签缓存下来。

## 构建

```
./gradlew build
```

## 许可

本移植版使用 MIT 协议，与原版 [JAOPCA Singularities](https://github.com/TheLMiffy1111/JAOPCASingularities)
（作者 TheLMiffy1111）一致。
