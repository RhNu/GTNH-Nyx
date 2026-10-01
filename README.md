# Nyx: No Humans Left

![Version](https://img.shields.io/badge/GTNH_Version-2.9.0_RC_1-blue)

English | [中文](README.zh.md)

This is a personal side mod I threw together after burning out on crafting the Star Gate in earlier runs.
After that, I just wanted to mess around, and this absurd mod happened.

This is a **cheat mod**. Balance and technical purity are not the goal here; having fun is.

Unofficial [Pre-Release builds](https://github.com/RhNu/GTNH-Nyx/releases/tag/edge-build) are usually used for
testing, in-progress development, or temporary packaging for GTNH Beta and RC versions before an official GTNH release.

> [!NOTE]
> As stated above, this mod is not built for balance.
> If you are looking for a balanced experience, please do not use this mod.
>
> Have fun!

## Features

Some features may need to be enabled in `config\Nyx\*.cfg`.

- A simple material system that may be expanded in the future.

- Mixin edits: See `MIXIN.cfg` for more details. Mainly targets AE and BartWorks (now in GT5U).

- COPIER: Duplicate items in the controller

  **Config: `MTE_COPIER`**

> The button switches between item mode and fluid mode.
> Item mode copies items in the controller, while fluid mode copies fluids from the cell item in the controller.
>
> The text field sets the amount of items or fluids to copy.
>
> The machine runs once every 5 seconds by default. You can change this with `MTE_COPIER_TICK` in config.

![copier_1](img/copier_1.png)

- PROXY: Run machines through their RecipeMap

  **Config: `MTE_PROXY`**

> Ordinary machines keep their live RecipeMap backend. Dedicated lazy strategies also support current/legacy
> Tree Growth Simulators and Algae Ponds, plus basic Mass Fabricators and Rock Breakers. Mock recipes are private
> to the proxy: upstream display/fake recipes are never made executable globally.
>
> Tree farms use saplings (including Forestry genetics) and mode-appropriate tools in input buses as reusable
> selectors. Tools lose neither durability nor charge, including discharged electric tools; this is an intentional
> proxy perk. Yield and power follow the input-voltage tier; machine count still controls parallelism.
>
> Both algae generations use the input-voltage tier and the newer machine's 90% tier-voltage EU/t without OC.
> Put a water bucket in an input bus to represent pond water; it is not consumed or emptied. Compost is still
> consumed per parallel operation and upgrades the output tier once. Source chances and durations are preserved.
>
> Basic Mass Fabricators prefer UUA when enough is available. Any integrated circuit prevents the unamplified
> branch; an amplified operation does not require circuit 1. Current configuration and the controller's special
> 2x-power overclock rules apply, independently per parallel operation.
>
> Basic Rock Breakers use reusable water/lava buckets and required solid blocks instead of world neighbors.
> The existing mode button or screwdriver selects the source positional-environment variant, distinguishing
> stone from cobblestone without changing original circuit requirements. Redstone/glowstone are still consumed;
> blue ice/magma and environmental items remain reusable. Additional runtime variants are discovered live.
>
> HTGR, Eye of Harmony, Solar Tower, space project managers, LHC, current/legacy large boilers, Decay Warehouse,
> Quantum Computer, Radio Hatch, Forge of Gods upgrade costs, and scanner/research machines are denied.
> Exo Foundry retains solidification while its module-cost map is excluded. BioLab's real recipes and dynamic
> backends such as the industrial chisel remain available. Exotic Module's custom process is not added here.
>
> Modes are instance-owned and safely restored; cached species include copied NBT and current registration
> data. Late recipe registrations, tier/compost/configuration changes and input changes are rechecked. Recipe
> locking is disabled for proxies so saved locks cannot bypass a strategy. An already-paid in-flight cycle from
> an older save may finish once; newly denied controllers cannot start another cycle.

![proxy_1](img/proxy_1.png)

> Parallel limit is controlled by the number of machines in the controller.
> Formula: `amount ^ (log10(Integer.MAX_VALUE) / log10(64))`, simplified as `amount ^ 3.98`.
> So, 1 machine = 1 parallel, and 64 machines = Integer.MAX_VALUE parallel.

- CONVERTER: Convert items between different OreDict entries (e.g. ingotCopper <-> plateCopper).

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

> Put an item representing the target OreDict in the controller.
> Input the item you want to convert, and the machine outputs the converted result.

### Note: About ID Conflicts

I do not know how other personal mods allocate MTE IDs. If conflicts happen, you can adjust the ID offset in
`config\Nyx\MACHINE.cfg`, then restart the game. The log output will also show the exact ID conflict target.

The mods and versions below had no conflicts when I updated Nyx to 2.7.3:

| Mod                                                                              | Version        |
| :------------------------------------------------------------------------------- | -------------- |
| [Twist-Space-Technology-Mod](https://github.com/Nxer/Twist-Space-Technology-Mod) | 0.6.14         |
| [BoxPlusPlus](https://github.com/RealSilverMoon/BoxPlusPlus)                     | 1.9.3          |
| [Programmable-Hatches-Mod](https://github.com/reobf/Programmable-Hatches-Mod)    | v0.1.2p28-beta |
