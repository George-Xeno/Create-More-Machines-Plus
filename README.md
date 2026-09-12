# CMM Plus

机械动力附属 [更多机械](https://modrinth.com/mod/create-more-machines)（CreateMoreMachines）的扩展，加了四档**鼓风机**和**粉碎轮**：黄铜 x4、下界 x8、末地 x16、超越 x32。每档有自己的方块实体、贴图、应力消耗，写法跟 Create 和 CMM 里的机器一样。

1.21.1 / NeoForge，需要 Create 6.0.9+ 与 CreateMoreMachines 2.7+。

## 档位到底改了什么

**鼓风机**：原版风机的处理速度跟转速没关系——空气流每 tick 只把前方物品的处理计时减 1，转速只决定吹多远、推多猛。所以档位风机不动转速，而是让空气流在一 tick 里把"处理"这一步做 N 次：速度乘 N，射程、推力、应力还是同转速原版风机那台。实现见 `TieredAirCurrent`。

**粉碎轮**：倍率只加在机器对外报的转速上（`getSpeed()`，粉碎控制器、实体碰撞、护目镜读的都是它），网络里跑的转速（`getTheoreticalSpeed()`）一点没动。这两个值必须分开：Create 的传动网络是拿 `getTheoreticalSpeed()` 算邻居转速的，把它乘大之后轮子会被当成更强的动力源，然后方块直接被摧毁。

**应力**：越级不是白吃的。系数 = Create 原值 × 处理倍数 × 2，跟 CMM 对自己机器用的是同一套关系。一对能正常工作的粉碎轮是两个轮子，算账时别忘了乘 2。

| 档位 | 处理速度 | 鼓风机 SU/rpm | 粉碎轮 SU/rpm（每个轮） |
|---|---|---|---|
| 原版 | x1 | 2 | 8 |
| 黄铜 | x4 | 16 | 64 |
| 下界 | x8 | 32 | 128 |
| 末地 | x16 | 64 | 256 |
| 超越 | x32 | 128 | 512 |

顺便说一句，满转对粉碎轮挺浪费：Create 的处理速率被夹在 20/tick，超越轮输入大约 8 rpm（64 个一叠时约 47 rpm）就到顶了，再往上只是多烧应力。

## 安装

1. 装好 Create 6.0.9+ 和 CreateMoreMachines 2.7+（NeoForge 1.21.1）。
2. 把 `CMMPlus-1.21.1-<版本>.jar` 丢进 `mods` 文件夹。

## 配置

首次启动会生成 `config/cmmplus-startup.toml`，结构照 CMM 的分档设置来：

```toml
[TierSettings.BrassTier]
    brass_encased_fan_impact = 16.0
    brass_crushing_wheel_impact = 64.0
```

配方是从原版机器一路升上去的（原版机器 + 黄铜板，再往上换成 CMM 的合金板）。旧版本存档里已经放下的机器，读档时会自动换成对应档位的方块实体，不用拆掉重放。

## 编译

```
gradlew build          # 产出 build/libs/CMMPlus-1.21.1-<版本>.jar
gradlew runClient      # 开发客户端
gradlew runServer      # 开发服务端
gradlew runSelfTest    # 功能自检
```

需要编译依赖的 mod（Create、Flywheel、Ponder、Registrate、CMM）自己放进 `libs/`，清单见 `libs/README.md`——别人的 jar 不放在这个仓库里。

### 自检

`gradlew runSelfTest` 会起一个服务端，在出生点旁边把整套机器搭出来，实际跑一遍，然后自己关服：

```
[self-test] vanilla fan: 154 ticks per raw beef -> steak (tier x1)
[self-test] brass fan: 40 ticks per raw beef -> steak (tier x4)
[self-test] beyond@256 wheels: controller=true valid=true crushingspeed=163.84 networkSpeed=256.00
[self-test] stress: brass wheel 64.0 SU/rpm at 64.0 rpm -> 4096.0 SU per wheel
[self-test] RESULT: PASS
```

覆盖范围：各档风机的实际处理速度、粉碎轮的工作区有没有生成并喂到档位转速（同时确认网络转速没被改）、每台机器从网络里实际扣走多少应力、物品 tooltip、方块实体有效性、旧存档迁移。

## 致谢

- **Create**（MIT）：机器本身是它的 `EncasedFanBlock` / `CrushingWheelBlock` 子类；粉碎轮的工作区配对逻辑和风机物品模型参照了 Create 自己的代码与模型，粉碎速度和 tooltip 格式也跟它保持一致。
- **Create More Machines**：档位机器的写法（每档一个方块实体类型、启动配置里的分档应力、档位前缀的物品名）以及这个附属接入的档位体系，都是跟它学的。

## 许可

MIT，见 [LICENSE](LICENSE)。

---

# CMM Plus (English)

An addon for **[Create: More Machines](https://modrinth.com/mod/create-more-machines)** adding four
tiers of Create's **Encased Fan** and **Crushing Wheel** - Brass, Netherite, End and Beyond - with
their own block entities, textures and stress costs, built the way Create and Create More Machines
build their own machines.

Minecraft 1.21.1 / NeoForge, requires Create 6.0.9+ and CreateMoreMachines 2.7+.

## What a tier changes

**Fan** - a Create fan's throughput has nothing to do with its rotation speed: every tick its air
current decrements the processing timer of each item in front of it exactly once, and the speed only
decides how far it reaches and how hard it pushes. A tier fan therefore does not touch the speed at
all: it performs that one processing step N times per tick, so it smelts, washes, smokes and haunts N
times faster while range, push and stress stay those of a vanilla fan at the same speed. See
`TieredAirCurrent`.

**Crushing Wheel** - the multiplier is applied to the speed the machine *reports*
(`getSpeed()`, which is what the crushing controller, entity collisions and the goggles read), and
deliberately **not** to `getTheoreticalSpeed()`, which is what Create's rotation propagator uses to
decide what the rest of the network runs at. Turning that one up makes the propagator treat the wheel
as a stronger source and destroy the block.

**Stress** - a tier is not a free upgrade: the impact is Create's value x the tier's processing
multiplier x 2, the same relation Create More Machines uses for its own machines. A working crusher
is two wheels, so remember the factor of two.

| Tier | Processing | Fan (SU/rpm) | Crushing Wheel (SU/rpm, per wheel) |
|---|---|---|---|
| Create | x1 | 2 | 8 |
| Brass | x4 | 16 | 64 |
| Netherite | x8 | 32 | 128 |
| End | x16 | 64 | 256 |
| Beyond | x32 | 128 | 512 |

Also worth knowing: running a tier wheel flat out is mostly wasted - Create clamps its processing
rate at 20/tick, which a Beyond wheel already reaches at roughly 8 rpm input (about 47 rpm with a
stack of 64). More rpm past that only burns stress.

## Install

1. Install Create 6.0.9+ and CreateMoreMachines 2.7+ for NeoForge 1.21.1.
2. Drop `CMMPlus-1.21.1-<version>.jar` into your `mods` folder.

## Config

`config/cmmplus-startup.toml` is generated on first launch, mirroring Create More Machines' tier
settings layout:

```toml
[TierSettings.BrassTier]
    brass_encased_fan_impact = 16.0
    brass_crushing_wheel_impact = 64.0
```

Recipes upgrade the machine step by step (Create machine + brass sheet, then Create More Machines'
alloy sheets). Machines placed by older builds of this addon are migrated on load and become the tier
machine they are, no need to break and replace them.

## Building

```
gradlew build          # -> build/libs/CMMPlus-1.21.1-<version>.jar
gradlew runClient      # dev client
gradlew runServer      # dev server
gradlew runSelfTest    # functional self test
```

Put the jars this mod compiles against (Create, Flywheel, Ponder, Registrate, CMM) into `libs/`, see
`libs/README.md` - other people's jars are not redistributed here.

### Self test

`gradlew runSelfTest` starts a server, builds complete machines next to the world spawn, measures
them for real and shuts itself down:

```
[self-test] vanilla fan: 154 ticks per raw beef -> steak (tier x1)
[self-test] brass fan: 40 ticks per raw beef -> steak (tier x4)
[self-test] beyond@256 wheels: controller=true valid=true crushingspeed=163.84 networkSpeed=256.00
[self-test] stress: brass wheel 64.0 SU/rpm at 64.0 rpm -> 4096.0 SU per wheel
[self-test] RESULT: PASS
```

It covers the fan processing rate per tier, the crushing controller being created and fed the tier's
speed while the network keeps its own, the stress each machine draws from its network, the item
tooltips, block entity validity and the legacy save migration.

## Credits

- **Create** (MIT) - the machines are subclasses of its `EncasedFanBlock` / `CrushingWheelBlock`; the
  crushing wheel controller pairing and the fan item model are adapted from Create's own code and
  models, and crushing speed and tooltip formatting follow it.
- **Create More Machines** - the tier machine pattern (one block entity type per tier, per-tier stress
  impacts in a startup config, tier-prefixed item names) and the tier progression this addon plugs
  into.

## License

MIT, see [LICENSE](LICENSE).
