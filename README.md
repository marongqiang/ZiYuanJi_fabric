# 资源鸡 Fabric（ZiYuanJi_fabric）

基于经典 **Chickens / 更多鸡** 思路的 **Minecraft 1.20.1 Fabric** 移植与扩展：用数据驱动的「资源鸡」产出各类资源，配合 **鸡舍**、**繁殖机** 等方块自动化养殖与繁殖。

> 模组 ID：`chickens` · 当前版本见 [`gradle.properties`](gradle.properties) 中的 `mod_version`

---

## 功能概览

- **资源鸡**：在 `data/chickens/chickens/*.json` 中定义鸡种、掉落与属性（生长、下蛋间隔、繁殖等）。
- **物品鸡**：用 **击鸡棒**（`chicken_catcher`）将实体鸡收成物品，可放入机器或参与繁殖；-tooltip 与部分信息展示与原版风格对齐。
- **鸡舍（Henhouse）**：收容物品鸡并随时间产出鸡蛋/资源；输出槽优先与已有堆叠合并。
- **繁殖机（Breeder）**：两只物品鸡繁殖，心形进度与繁殖冷却关联。
- **可选集成**
  - **EMI**：配方与繁殖/产蛋类信息（开发环境可通过 `run` 目录下的 EMI 配置启用界面）。
  - **Jade**：方块与实体信息展示（可选模组）。

---

## 环境要求

| 项目 | 版本 |
|------|------|
| Minecraft | **1.20.1** |
| Fabric Loader | **≥ 0.15**（工程内见 `gradle.properties`） |
| Fabric API | 与 `fabric_api_version` 一致 |
| Java | **17** |

---

## 从源码构建

```bash
cd fabric-1.20.1
./gradlew build
```

- Windows：`gradlew.bat build`
- 构建产物：`build/libs/chickens-fabric-<version>.jar`
- 本地运行客户端（含可选 EMI / Jade 运行时依赖）：`./gradlew runClient`

---

## 安装到游戏

1. 安装 **Fabric Loader**（对应 1.20.1）。
2. 放入 **Fabric API** 与本模组的 `chickens-fabric-*.jar`。
3. 按需安装 **EMI**、**Jade**（均为可选）。

---

## 数据与资源

- 鸡种数据：`src/main/resources/data/chickens/chickens/`
- 配方：`src/main/resources/data/chickens/recipes/`
- 语言：`src/main/resources/assets/chickens/lang/`（含 `zh_cn`）
- 收容鸡物品模型与 CMD 由 Gradle 任务 **`generateCapturedChickenAssets`** 在构建时生成（见 `build.gradle` 注释）。

---

## 致谢与许可

- 玩法与代码脉络继承自 **Chickens**（SetyCz、Wyvi 等原作者）；本仓库为 **Fabric 1.20.1** 方向的维护与改动版本。
- 许可证：**MIT**（见 `fabric.mod.json` 与上游约定）。

---

## ZiYuanJi_fabric (English)

**Resource Chickens** on **Fabric 1.20.1**: JSON-defined chicken types, **henhouse** automation, **breeder**, and **chicken catcher** items. **EMI** and **Jade** are optional.

**Build:** Java 17, `./gradlew build`. **Artifact:** `build/libs/chickens-fabric-*.jar`.

**Upstream inspiration:** [Chickens](https://www.curseforge.com/minecraft/mc-mods/chickens) (concept and original authors listed in `fabric.mod.json`).
