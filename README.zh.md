# Nyx: 没有人类了

![Version](https://img.shields.io/badge/GTNH_Version-2.9.0_RC_1-blue)

[English](README.md) | 中文

随便整的私货，主要是因为当年搓星门搓把热情耗尽了，后面玩就纯耍，产生了这个逆天模组。

这玩意儿是**作弊Mod**。技术力和平衡是不存在的，自己玩的爽了就行。

非正式发布的 [Pre-Release 构建](https://github.com/RhNu/GTNH-Nyx/releases/tag/edge-build) 通常用于测试、开发半途或 GTNH 未发布正式版时针对 Beta 和 RC 版本的临时打包。

> [!NOTE]
> 如上所述，这个Mod并不是为了平衡而生的。
> 如果你想要一个平衡的体验，请不要使用这个Mod。
>
> 祝你玩得开心！

## 功能

功能可能需要在`config\Nyx\*.cfg`配置文件中启用。

- 一些简单的材料系统，未来可能会用到。

- Mixin修改：参见配置文件`MIXIN.cfg`以获取更多详细信息。主要用于AE和BartWorks（现在在GT5U中）。

- 物质定增：复制控制器中的物品

  **配置：`MTE_COPIER`**

> 按钮用于切换物品和流体的模式。
> 物品模式可以复制控制器中的物品，流体模式则是复制单元中的流体。
>
> 文本框用于设置要复制的物品或流体的数量。
>
> 每5秒工作一次，可以在配置中通过`MTE_COPIER_TICK`进行更改。

![copier_1](img/copier_1.png)

- 代理执行：通过使用RecipeMap代理机器

  **配置：`MTE_PROXY`**

> 普通机器继续使用原有的动态 RecipeMap backend；新旧原木拟生场、藻场，以及基础物质发生器和
> 基础碎石机使用独立、按需加载的 mock 策略。mock 不会把上游展示表的 fake 标记全局取消。
>
> 原木拟生场：输入总线中的树苗（含 Forestry 基因）与对应工具作为不消耗的选择条件。
> 工具免耐久、免电量，空电工具也可使用，这是 proxy 的明确福利；产量与耗电按输入电压等级计算，
> 并行仍由控制器中的机器数量决定。
>
> 新旧藻场统一按输入电压定级，采用新版 90% 等级电压耗电且不超频。输入总线放水桶表示池水，
> 水桶及其内容均不消耗；堆肥仍按每个并行消耗，并将产物等级提升一级，保留模板时长与概率。
>
> 基础物质发生器优先使用足量 UUA。任意集成电路会阻止无 UUA 分支，但有 UUA 分支不要求电路 1。
> 实时配置与控制器特有的 2 倍耗电超频规则仍有效，每个并行分别遵循该上限。
>
> 基础碎石机：用不消耗的水桶、岩浆桶及对应实体方块表示环境需求。通过原有模式按钮或螺丝刀
> 切换原机的方位环境变体，区分石头与圆石，原有电路要求不变。红石／荧石仍消耗，蓝冰／岩浆块
> 及环境物品不消耗；后续运行时注册的变体也会动态读取。
>
> 禁用 HTGR、鸿蒙之眼、太阳塔、太空项目管理模块、LHC、新旧大型锅炉、衰变仓库、量子计算机、
> 辐射仓、诸神锻炉主机升级成本，以及扫描／研究机器。Exo Foundry 保留真实凝固表并排除模块成本表；
> BioLab 普通真配方和工业凿刻机等动态 backend 保留。Exotic Module 的专用流程本轮未适配。
>
> 模式由每台机器独立保存并安全恢复；树种缓存包含复制的 NBT 和当前注册内容。新增配方及输入、
> 等级、堆肥、配置变化会重新检查。代理禁用配方锁，旧锁不能绕过策略；旧存档已付费启动的工作周期
> 可以完成一次，但被禁用的控制器不能再启动新周期。

![proxy_1](img/proxy_1.png)

> 并行限制由控制器中的机器数量控制。
> 计算方式为 `数量 ^ (log10(Integer.MAX_VALUE) / log10(64))`，简化为 `数量 ^ 3.98`；
> 所以1台机器 = 1个并行，64台机器 = Integer.MAX_VALUE个并行。

- 矿典同步: 将物品在不同矿典之间转换 (e.g. 铜锭 <-> 铜板).

  **Config: `MTE_CONVERTER`**

<table>
  <tr>
    <td><img src="img/converter_1.png" alt="converter_1" width="400"></td>
    <td><img src="img/converter_2.png" alt="converter_2" width="400"></td>
  </tr>
  <tr>
    <td><img src="img/converter_3.png" alt="converter_3" width="400"></td>
    <td><img src="img/converter_4.png" alt="converter_4" width="400"></td>
  </tr>
</table>

> 在控制器中放置代表目标矿辞的物品。
> 输入你想要转换的物品，它将输出转换后的物品。

### 注意：关于ID冲突

我不知道别的私货模组如何使用MTE的ID，可以在 MTE 发生冲突的时候，到 `config\Nyx\MACHINE.cfg` 里调整ID偏移量，
重启游戏。我设置的日志也会显示ID冲突的具体目标。

下面表格中的Mod与对应版本在我更新2.7.3版本的时候没有冲突：

| Mod                                                                              | Version        |
| :------------------------------------------------------------------------------- | -------------- |
| [Twist-Space-Technology-Mod](https://github.com/Nxer/Twist-Space-Technology-Mod) | 0.6.14         |
| [BoxPlusPlus](https://github.com/RealSilverMoon/BoxPlusPlus)                     | 1.9.3          |
| [Programmable-Hatches-Mod](https://github.com/reobf/Programmable-Hatches-Mod)    | v0.1.2p28-beta |
