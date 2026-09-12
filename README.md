# Create:More Machines Plus
 
机械动力附属 [更多机械](https://modrinth.com/mod/create-more-machines)（CreateMoreMachines）的扩展，加了四档**鼓风机**和**粉碎轮**：黄铜 x4、下界 x8、末地 x16、超越 x32

1.21.1 / NeoForge，需要 Create 6.0.9+ 与 CreateMoreMachines 2.7+。


| 档位 | 处理速度 | 鼓风机 SU/rpm | 粉碎轮 SU/rpm（每个轮） |
|---|---|---|---|
| 原版 | x1 | 2 | 8 |
| 黄铜 | x4 | 16 | 64 |
| 下界 | x8 | 32 | 128 |
| 末地 | x16 | 64 | 256 |
| 超越 | x32 | 128 | 512 |

## 安装

1. 装好 Create 6.0.9+ 和 CreateMoreMachines 2.7+（NeoForge 1.21.1）。
2. 把 `CMMPlus-1.21.1-<版本>.jar` 丢进 `mods` 文件夹。

## 配置

```toml
[TierSettings.BrassTier]
    brass_encased_fan_impact = 16.0
    brass_crushing_wheel_impact = 64.0
```

## 致谢

- **Create**（MIT）
- **Create More Machines**

## 许可

MIT，见 [LICENSE](LICENSE)。
