# NeoForge 26.2 玩家外观生物开发文档

## 1. 项目目标与当前状态

本项目为 Minecraft Java Edition 26.2 与 NeoForge 初版 Mod。它注册一种玩家外观生物，并为每位玩家在世界出生点旁放置一个装有该生物生物蛋、一颗钻石和一个命名牌的补给箱。玩家可用钻石签订契约，使生物成为该玩家的伙伴，跟随并保护唯一主人；保留近战与弓箭远程攻击切换、装备和固定皮肤。

初版源码、资源和 MDK 构建配置已建立。构建脚本使用工作区内的 Java 25 和 Gradle 缓存，并通过 NeoForged 官方 CDN 获取依赖。`build-package.ps1` 已针对 NeoForge 26.2.0.84 成功完成 Java 编译并生成 `build/libs/Soulmate-v0.2.0.jar`；游戏内行为仍待验证。

## 2. 已实现的初版行为

| 项目 | 初版实现 |
| --- | --- |
| Minecraft / NeoForge | 26.2 / NeoForge 26.2.0.84，基于官方 ModDevGradle MDK |
| 实体 ID | `mcsoulmate:soulmate` |
| 自然生成 | 未注册群系生成或 biome modifier |
| 生物蛋与补给箱 | 注册 Soulmate 生物蛋并加入刷怪蛋创造栏；每位玩家首次成功领取时，在世界出生点相邻的空地放置一个箱子，内含生物蛋、一颗钻石和一个命名牌 |
| 补给箱重试 | 出生点周围八个相邻位置都不满足空位和坚实地面条件时记录警告，并在该玩家下次登录时重试 |
| 召唤 | 使用 Soulmate 生物蛋或 `/summon mcsoulmate:soulmate ~ ~ ~` |
| 皮肤 | 固定使用 `assets/mcsoulmate/textures/entity/soulmate.png` |
| 模型 | 原版 `PlayerModel`，Alex 细手臂层 |
| 装备 | 使用生物标准装备槽，渲染盔甲和手持物品 |
| 契约 | 未驯服实体手持一颗钻石右击即可签约；生存模式消耗一颗钻石，主人状态由原版驯服实体机制保存和同步 |
| 成就 | 首次签约触发 `mcsoulmate:contract` advancement，标题为“签订契约”，描述为“一颗永流传” |
| 近战 | 主手没有弓时，近战攻击主人主动攻击或伤害主人的目标 |
| 远程 | 主手持弓时，对主人的保护目标发射原版箭；当前不消耗箭矢 |
| 目标 | 采用狼式主人受击、主人攻击目标和自身受击目标；不再自动寻找所有 `Monster`，只绑定一个主人 |
| 耕种 | 没有战斗目标时，在附近耕地收获成熟 `CropBlock`、用库存种子补种；收成保存在实体库存 |
| 种子补充 | 签约后手持可种植作物右击 Soulmate，可向耕种库存加入一件；生存模式会消耗该物品 |
| 箱子存储 | 20 格内有可用普通箱子或陷阱箱时前往存放；优先合并同类堆叠，再使用空槽 |
| 钓鱼 | 安全且主手持钓鱼竿时，前往附近水岸、抛竿、等待咬钩并收线；鱼获进入独立存档库存，累计 10 件鱼类物品后暂停钓鱼并前往箱子存放 |
| API 配置入口 | 在 Minecraft「选项」界面添加 API 配置按钮；编辑的服务商、地址、密钥与模型保存在客户端配置文件 |
| 移动 | 签约后跟随主人；移动速度属性初值为 0.13，需在游戏内测量并校准寻路速度 |

这些目标、武器和速度数值是初版默认值，后续可以根据玩法调整。

## 3. 工程结构

```text
src/main/java/com/mcsoulmate/
  McSoulmate.java                    # Mod 初始化、实体与属性注册
  entity/ModEntityTypes.java         # 实体类型注册
  entity/ModItems.java               # Soulmate 生物蛋注册
  entity/SoulmateEntity.java         # 契约、主人关系、目标选择、移动和攻击行为
  entity/FarmInventory.java          # 可保存的农作物、种子和钓获物品库存
  entity/ai/FarmingGoal.java         # 安全状态下的耕作与箱子存放调度
  entity/ai/FishingGoal.java         # 使用原版钓竿行为、咬钩后收线并整理鱼获
  entity/ai/FishingSpotSearch.java   # 搜索可站立的水岸钓鱼位置
  entity/ai/FarmActions.java         # 作物查找、收获、补种和内部库存
  entity/ai/FarmStorage.java         # 搜索附近箱子并合并/转移库存物品
  server/StarterChestEvents.java     # 玩家首次领取时在世界出生点旁放置补给箱
  client/SoulmateClientEvents.java   # 客户端渲染器注册
  client/SoulmateOptionsEvents.java # 在 Minecraft 选项页添加 API 配置入口
  client/ApiConfigurationScreen.java # API 配置编辑界面
  client/ApiConfigurationStore.java # 客户端本地 API 配置读写
  client/SoulmateRenderer.java       # 玩家模型、皮肤和装备层
  client/FishingLineLayer.java       # 根据同步鱼钩位置绘制鱼线
src/main/resources/
  META-INF/accesstransformer.cfg       # 读取原版钓鱼钩的咬钩同步状态
  assets/mcsoulmate/lang/            # 中文与英文名称
  assets/mcsoulmate/items/           # 生物蛋物品模型定义
  assets/mcsoulmate/textures/entity/ # 固定皮肤
  data/mcsoulmate/advancement/       # 契约成就
src/main/templates/META-INF/
  neoforge.mods.toml                 # NeoForge Mod 元数据模板
```

## 4. 服务器与客户端职责

逻辑服务器负责契约和主人归属、目标选择、寻路、装备状态、箭矢生成、攻击伤害、耕种、钓鱼与箱子库存操作及 advancement 进度。鱼钩由服务器创建并按原版钓竿生命周期更新；农作物、种子与钓获物品分别进入随实体保存的库存。钓鱼库存累计 10 件鱼类物品后触发存箱，鱼类物品降到 10 件以下后恢复钓鱼。客户端根据同步的鱼钩实体 ID 绘制从手持钓竿到浮标的鱼线，并渲染原版浮标、手持物品和实体姿势。驯服状态与主人 UUID 使用原版 TamableAnimal 同步和保存。单人世界同样包含逻辑服务器。

API 配置入口在客户端监听 Minecraft 选项界面的初始化事件。配置页只在本地读写服务商、API 地址、API 密钥和模型字段；当前没有 API 请求逻辑，密钥以纯文本保存在客户端 `config/mcsoulmate-api.properties` 中。

## 5. 皮肤与模型

皮肤资源为用户提供的 64×64 PNG。渲染器复用 Minecraft 内置 `PlayerModel`，并使用 `ModelLayers.PLAYER_SLIM` 与 `slim = true` 选择 Alex 三像素手臂；纹理固定指向本 Mod 的皮肤文件。盔甲层使用 slim 玩家盔甲模型，手持物品层读取实体装备槽。

初版不按玩家 UUID 加载在线玩家皮肤，也不提供运行时切换皮肤功能。

## 6. 移动和动作

实体的 `MOVEMENT_SPEED` 初值为 0.13，对齐普通玩家冲刺时的基础速度参数。AI 寻路速度倍率、加速度、摩擦、碰撞箱及地形会影响实际位移，因此该属性值本身不能证明每秒距离与玩家完全相同。

模型复用原版玩家模型和人形动作状态。需在游戏内检查静止、移动、近战挥击、持弓瞄准及穿甲时的动作。若实测奔跑速度或动作与玩家冲刺不一致，应在实体移动控制和渲染状态层针对性校准，而不是只调整纹理或模型尺寸。

钓鱼时，原版钓鱼钩实体负责浮标和咬钩生命周期；鱼钩 ID 通过 Soulmate 同步数据传给客户端，自定义渲染层将鱼线从持竿手绘制到浮标。钓鱼竿的手持姿势沿用玩家模型的物品姿势，抛竿和收线会触发手臂挥动。

## 7. 战斗与装备

- 主手持有 `BowItem` 时选择远程目标行为；否则选择近战行为。
- 未驯服的实体可由玩家用一颗钻石右击签约；每个实体只能有一个主人，签约后使用狼式主人目标行为并跟随主人。
- Soulmate 保护唯一主人：攻击伤害主人的目标、主人主动攻击的目标，以及攻击 Soulmate 自身的目标；不会像原型一样自动攻击所有敌对怪物。
- 首次签约触发 advancement `mcsoulmate:contract`，显示标题“签订契约”和描述“一颗永流传”。
- 远程武器当前仅支持原版弓，弹射物为普通箭，原型不消耗库存箭矢。
- 没有战斗目标时，Soulmate 会在周围耕地收获成熟作物并尝试补种，也会使用内部库存中的种子种植空置耕地。
- 主手装备钓鱼竿且处于安全状态时，Soulmate 会寻找可站立的水岸并钓鱼；咬钩标记触发原版收线行为，鱼获进入独立保存的钓鱼库存。
- 钓鱼库存中属于原版 `#minecraft:fishes` 标签的物品累计达到 10 件后，钓鱼会暂停；如果 20 格内有能接收鱼获的箱子，Soulmate 前往存放钓鱼库存，鱼类物品降到 10 件以下后恢复钓鱼。
- 签约后可手持作物种子、胡萝卜或马铃薯右击 Soulmate，向耕种库存补充一件种植材料；生存模式会消耗物品。
- 20 格内存在可存放物品的普通箱子或陷阱箱时，Soulmate 会走到箱子边存入内部库存；优先合并同类堆叠，再依次使用空槽。
- 没有可用箱子或箱子无空位时，收成保留在随实体保存的内部库存中，之后检测到有空间的箱子再存放。
- 实体启用拾取战利品，可通过命令直接设置装备。
- 盔甲和武器使用原版装备槽和物品渲染，不另做自定义物品。

示例命令：

```mcfunction
/item replace entity @e[type=mcsoulmate:soulmate,limit=1,sort=nearest] weapon.mainhand with minecraft:bow
/item replace entity @e[type=mcsoulmate:soulmate,limit=1,sort=nearest] weapon.mainhand with minecraft:iron_sword
/item replace entity @e[type=mcsoulmate:soulmate,limit=1,sort=nearest] armor.chest with minecraft:iron_chestplate
```

## 8. 出生点补给与自然生成

实体类型注册本身不会将生物加入群系生成表。项目不注册 biome modifier 或自然生成权重。每位玩家首次领取时，服务器在主世界共享出生点周围最多八个相邻位置中查找一个空位，要求位置为空且下方方块顶面坚实；补给箱内放置 Soulmate 生物蛋、一颗钻石和一个命名牌。成功后用玩家 scoreboard tag 记录已发放状态，重登不会再次生成。若附近没有合适位置，服务器记录警告并在下次登录重试。玩家也可使用生物蛋或 `/summon mcsoulmate:soulmate ~ ~ ~` 召唤。

## 9. 构建与验证

官方 26.2 ModDevGradle 模板配置 Java 25 工具链。Windows 下使用：

```powershell
.\build-package.ps1
```

成功后 jar 位于 `build/libs/Soulmate-v0.2.0.jar`，可放入 NeoForge 26.2 的 `mods` 目录。构建成功只验证了编译和打包；仍需在客户端验证召唤、固定皮肤、Alex 手臂、盔甲、手持物品、目标选择、近战和弓箭行为，并在多人环境确认服务器权威状态同步。

## 10. 验收清单

- [x] 建立 26.2 NeoForge MDK 工程和 Java 25 工具链配置。
- [x] 注册 `mcsoulmate:soulmate`，未添加自然生成配置。
- [x] 注册 Soulmate 生物蛋，并在玩家首次登录时于世界出生点旁发放含生物蛋、钻石和命名牌的补给箱。
- [x] 加入固定皮肤、Alex PlayerModel、盔甲层和手持物品层代码。
- [x] 加入初版近战与弓箭远程攻击逻辑。
- [x] 加入钻石契约、主人保存/同步、狼式护主目标与签约成就。
- [x] 加入安全状态下的耕种、补种和 20 格内箱子存储逻辑。
- [x] `build-package.ps1` 成功生成 `Soulmate-v0.2.0.jar`。
- [ ] 游戏内验证渲染、装备、攻击、耕种、钓鱼、箱子整理和命令召唤。
- [ ] 平地实测并校准玩家冲刺速度及奔跑动作。
- [ ] 多人服务器验证实体状态同步。

## 11. 官方参考

- [NeoForge 26.2 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle)
- [NeoForge 实体文档](https://docs.neoforged.net/docs/entities/)
- [NeoForge 实体渲染文档](https://docs.neoforged.net/docs/entities/renderer/)
- [NeoForge 护甲文档](https://docs.neoforged.net/docs/items/armor/)
- [NeoForge 实体属性文档](https://docs.neoforged.net/docs/entities/attributes/)
- [NeoForge 26.2 迁移说明](https://docs.neoforged.net/primer/docs/26.2/)
