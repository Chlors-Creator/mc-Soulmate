# NeoForge 26.2 玩家外观生物开发文档

## 1. 项目目标与当前状态

本项目为 Minecraft Java Edition 26.2 与 NeoForge 初版 Mod。它注册一种只通过命令召唤的生物，使用用户提供的玩家皮肤和 Alex 细手臂模型，可穿戴盔甲、持有物品，并在近战与弓箭远程攻击间切换。

初版源码、资源和 MDK 构建配置已建立。当前尚未完成成功打包或游戏内验证：本机只有 JDK 21 和 JDK 8，而官方 26.2 MDK 以 Java 25 为目标；本轮 Gradle 还在启动守护进程时因无法建立回环连接而退出，未进入 Java 编译阶段。

## 2. 已实现的初版行为

| 项目 | 初版实现 |
| --- | --- |
| Minecraft / NeoForge | 26.2 / NeoForge 26.2.0.90，基于官方 ModDevGradle MDK |
| 实体 ID | `mcsoulmate:soulmate` |
| 自然生成 | 未注册群系生成或 biome modifier；没有刷怪蛋 |
| 召唤 | `/summon mcsoulmate:soulmate ~ ~ ~` |
| 皮肤 | 固定使用 `assets/mcsoulmate/textures/entity/soulmate.png` |
| 模型 | 原版 `PlayerModel`，Alex 细手臂层 |
| 装备 | 使用生物标准装备槽，渲染盔甲和手持物品 |
| 近战 | 主手没有弓时使用近战目标行为 |
| 远程 | 主手持弓时发射原版箭；当前不消耗箭矢 |
| 目标 | 初版主动攻击 `Monster` 类敌对生物，不主动选择玩家 |
| 移动 | 移动速度属性初值为 0.13；需在游戏内测量并校准寻路速度 |

这些目标、武器和速度数值是初版默认值，后续可以根据玩法调整。

## 3. 工程结构

```text
src/main/java/com/mcsoulmate/
  McSoulmate.java                    # Mod 初始化、实体与属性注册
  entity/ModEntityTypes.java         # 实体类型注册
  entity/SoulmateEntity.java         # 属性、目标选择、移动和攻击行为
  client/SoulmateClientEvents.java   # 客户端渲染器注册
  client/SoulmateRenderer.java       # 玩家模型、皮肤和装备层
src/main/resources/
  assets/mcsoulmate/lang/            # 中文与英文名称
  assets/mcsoulmate/textures/entity/ # 固定皮肤
src/main/templates/META-INF/
  neoforge.mods.toml                 # NeoForge Mod 元数据模板
```

## 4. 服务器与客户端职责

逻辑服务器负责实体生命值、目标选择、寻路、装备状态、箭矢生成和攻击伤害。客户端根据同步状态绘制模型、皮肤、盔甲、手持物品和实体姿势。单人世界同样包含逻辑服务器。

## 5. 皮肤与模型

皮肤资源为用户提供的 64×64 PNG。渲染器复用 Minecraft 内置 `PlayerModel`，并使用 `ModelLayers.PLAYER_SLIM` 与 `slim = true` 选择 Alex 三像素手臂；纹理固定指向本 Mod 的皮肤文件。盔甲层使用 slim 玩家盔甲模型，手持物品层读取实体装备槽。

初版不按玩家 UUID 加载在线玩家皮肤，也不提供运行时切换皮肤功能。

## 6. 移动和动作

实体的 `MOVEMENT_SPEED` 初值为 0.13，对齐普通玩家冲刺时的基础速度参数。AI 寻路速度倍率、加速度、摩擦、碰撞箱及地形会影响实际位移，因此该属性值本身不能证明每秒距离与玩家完全相同。

模型复用原版玩家模型和人形动作状态。需在游戏内检查静止、移动、近战挥击、持弓瞄准及穿甲时的动作。若实测奔跑速度或动作与玩家冲刺不一致，应在实体移动控制和渲染状态层针对性校准，而不是只调整纹理或模型尺寸。

## 7. 战斗与装备

- 主手持有 `BowItem` 时选择远程目标行为；否则选择近战行为。
- 远程武器当前仅支持原版弓，弹射物为普通箭，原型不消耗库存箭矢。
- 初版攻击目标限定为 `Monster`，避免默认主动攻击玩家或所有生物。
- 实体启用拾取战利品，可通过命令直接设置装备。
- 盔甲和武器使用原版装备槽和物品渲染，不另做自定义物品。

示例命令：

```mcfunction
/item replace entity @e[type=mcsoulmate:soulmate,limit=1,sort=nearest] weapon.mainhand with minecraft:bow
/item replace entity @e[type=mcsoulmate:soulmate,limit=1,sort=nearest] weapon.mainhand with minecraft:iron_sword
/item replace entity @e[type=mcsoulmate:soulmate,limit=1,sort=nearest] armor.chest with minecraft:iron_chestplate
```

## 8. 不自然生成

实体类型注册本身不会将生物加入群系生成表。项目不注册 biome modifier、自然生成权重或刷怪蛋。开发时通过 `/summon` 命令创建实体。

## 9. 构建与验证

官方 26.2 ModDevGradle 模板配置 Java 25 工具链。Windows 下使用：

```powershell
.\gradlew.bat jar
```

成功后 jar 位于 `build/libs/`。当前环境未产出 jar；原因和状态见第 1 节。完成构建后需在客户端验证召唤、固定皮肤、Alex 手臂、盔甲、手持物品、目标选择、近战和弓箭行为，并在多人环境确认服务器权威状态同步。

## 10. 验收清单

- [x] 建立 26.2 NeoForge MDK 工程和 Java 25 工具链配置。
- [x] 注册 `mcsoulmate:soulmate`，未添加自然生成配置或刷怪蛋。
- [x] 加入固定皮肤、Alex PlayerModel、盔甲层和手持物品层代码。
- [x] 加入初版近战、弓箭远程与敌对生物目标逻辑。
- [ ] `gradlew jar` 成功生成 Mod jar。
- [ ] 游戏内验证渲染、装备、攻击和命令召唤。
- [ ] 平地实测并校准玩家冲刺速度及奔跑动作。
- [ ] 多人服务器验证实体状态同步。

## 11. 官方参考

- [NeoForge 26.2 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle)
- [NeoForge 实体文档](https://docs.neoforged.net/docs/entities/)
- [NeoForge 实体渲染文档](https://docs.neoforged.net/docs/entities/renderer/)
- [NeoForge 护甲文档](https://docs.neoforged.net/docs/items/armor/)
- [NeoForge 实体属性文档](https://docs.neoforged.net/docs/entities/attributes/)
- [NeoForge 26.2 迁移说明](https://docs.neoforged.net/primer/docs/26.2/)