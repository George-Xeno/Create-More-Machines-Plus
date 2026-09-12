# CMM Plus

An addon for **[Create: More Machines](https://modrinth.com/mod/create-more-machines)** (CreateMoreMachines)
that adds four tiers of Create's **Encased Fan** and **Crushing Wheel** - Brass, Netherite, End and
Beyond - built the same way Create and Create More Machines build their own machines, with their own
block entity types, their own tier stress costs and their own tier textures.

*For Minecraft 1.21.1 / NeoForge, requires Create 6.0.9+ and CreateMoreMachines 2.1.1-2.7+.*

## What a tier actually does

| Tier | Processing | Encased Fan (SU/rpm) | Crushing Wheel (SU/rpm, per wheel) |
|---|---|---|---|
| Create | x1 | 2 | 8 |
| **Brass** | **x4** | 16 | 64 |
| **Netherite** | **x8** | 32 | 128 |
| **End** | **x16** | 64 | 256 |
| **Beyond** | **x32** | 128 | 512 |

* **Fan** - a Create fan's throughput does not come from its rotation speed: every tick its air
  current decrements the processing timer of each item in front of it exactly once. A tier fan
  simply performs that processing step *N* times per tick, so it smelts, washes, smokes and haunts
  *N* times faster while its range, its push and the stress it draws stay those of a 1x fan of the
  same speed. `TieredAirCurrent` has the details.
* **Crushing Wheel** - the tier multiplier is applied to the speed the wheel *reports*
  (`getSpeed()`), which is what the crushing controller, entity collisions and the goggles read, and
  deliberately **not** to `getTheoreticalSpeed()`, which is what Create's rotation propagator uses
  to decide what the rest of the network runs at. Scaling that one instead makes the propagator treat
  the wheel as a stronger source and destroy the block.
* **Stress** - a tier is not a free upgrade: its impact is Create's value × the tier's processing
  multiplier × 2, the same relation Create More Machines uses for its own tier machines. Every value
  is a startup config entry, so a pack can retune the whole table.

## Install

1. Install Create 6.0.9+ and CreateMoreMachines 2.7+ for NeoForge 1.21.1.
2. Drop `CMMPlus-1.21.1-<version>.jar` into your `mods` folder.

## Config

`config/cmmplus-startup.toml`, generated on first launch, mirroring Create More Machines' tier
settings layout:

```toml
[TierSettings.BrassTier]
    brass_encased_fan_impact = 16.0
    brass_crushing_wheel_impact = 64.0
```

Recipes upgrade a machine step by step (Create machine + brass sheet, then Create More Machines'
alloy sheets for the higher tiers), and worlds saved by older builds of this mod are migrated
automatically: a fan or wheel placed back when the machines still reused Create's block entities is
restored as the tier machine it is.

## Building

```
gradlew build          # -> build/libs/CMMPlus-1.21.1-<version>.jar
gradlew runClient      # dev client
gradlew runServer      # dev server
gradlew runSelfTest    # functional self test, see below
```

The mod compiles against the jars in `libs/` (see `libs/README.md`) - they are the mods it integrates
with and are not redistributed here.

### Self test

`gradlew runSelfTest` starts a server that builds complete machines next to the world spawn, measures
them for real and shuts itself down again:

```
[self-test] vanilla fan: 154 ticks per raw beef -> steak (tier x1)
[self-test] brass fan: 40 ticks per raw beef -> steak (tier x4)
[self-test] beyond@256 wheels: controller=true valid=true crushingspeed=163.84 networkSpeed=256.00
[self-test] stress: brass wheel 64.0 SU/rpm at 64.0 rpm -> 4096.0 SU per wheel
[self-test] RESULT: PASS
```

It covers the fan processing rate per tier, the crushing controller being created and fed the tier's
speed while the network keeps its own, the stress each machine draws from its network, the item
tooltips, the block entity validity and the legacy save migration.

## Credits

* **Create** (MIT) - the machines are subclasses of its `EncasedFanBlock`/`CrushingWheelBlock` and
  their block entities; the crushing wheel controller pairing and the fan item model geometry are
  adapted from Create's own code and models, and the crushing speed/tooltip formatting follows it.
* **Create More Machines** (CreateMoreMachines) - the tier machine pattern (own block entity type per
  tier via `IBE#getBlockEntityType`, per-tier stress impacts in a startup config, tier-prefixed item
  names) and the tier progression this addon plugs into.

## License

MIT, see [LICENSE](LICENSE).

---

# 中文说明

**CMM Plus** 是 [机械动力：更多机械](https://modrinth.com/mod/create-more-machines) 的附属，加入四档
**鼓风机** 与 **粉碎轮**（黄铜 x4 / 下界 x8 / 末地 x16 / 超越 x32），实现方式与 Create、CMM 自身一致：
每档有独立的方块实体类型、独立的应力消耗和独立贴图。

- **鼓风机**：Create 的鼓风机吞吐量不来自转速 —— 空气流每 tick 只把前方物品的处理计时器减 1。档位鼓风机
  就是把这个"处理步骤"每 tick 做 N 次，所以处理速度 ×N，而射程、推力、应力仍等同于同转速的原版鼓风机。
- **粉碎轮**：倍率只作用于机器**对外报告的转速**（`getSpeed()`，粉碎控制器/护目镜/实体碰撞读的是它），
  **不动** `getTheoreticalSpeed()`（Create 的传动网络靠它决定邻居转速；动它会让机器被网络判定为过强信号源
  并直接摧毁）。
- **应力**：越级不是白吃 —— 系数 = Create 原值 × 处理倍数 × 2（与 CMM 对其机器的做法同一关系），
  全部写在启动配置 `config/cmmplus-startup.toml` 里，整合包可自行调整。

编译需要把整合的 jar 放进 `libs/`（见 `libs/README.md`，出于许可考虑不随仓库分发）。
`gradlew runSelfTest` 会实际搭机器、实测倍速/应力/粉碎并在结束时自动关服，输出 `RESULT: PASS`。
