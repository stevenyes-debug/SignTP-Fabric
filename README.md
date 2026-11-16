# SignTP (Fabric 1.21.11)

右键告示牌执行原版 `/tp` 指令或预设指令的 Fabric 服务端模组。

## 安装

1. 准备 Fabric 1.21.11 服务端，并安装 Fabric Loader 0.20.1+ 和 Fabric API（0.141.6+1.21.11 或更高）。
2. 把 `signtp-1.0.0.jar` 放入服务器的 `mods/` 目录。
3. 启动服务器。首次启动会生成 `config/signtp.json`。

## 告示牌格式

### 1. `[tp]` 告示牌（所有玩家可用）

第一行写 `[tp]`，其余行写 `/tp` 的参数：

```text
[tp]
0 64 0
```

右键后执行：`/tp 0 64 0`

```text
[tp]
Steve
0 64 0
```

右键后执行：`/tp Steve 0 64 0`

如果第二~四行组成的文本正好等于某个预设名，则改为执行该预设指令。

### 2. `[preset]` 告示牌（所有玩家可用）

第一行写 `[preset]`，第二行写预设名：

```text
[preset]
spawn
```

右键后执行 `config/signtp.json` 中 `presets` 里 `spawn` 对应的指令。

### 3. `[cmd]` 告示牌（默认仅管理员可用）

第一行写 `[cmd]` 或 `[command]`，其余行写完整指令（不加 `/`）：

```text
[cmd]
say hello
```

或第一行以 `/` 开头：

```text
/say hello
```

默认只有 OP 能使用 `[cmd]` 告示牌，可在配置中关闭限制。

## 配置

配置文件：`config/signtp.json`

```json
{
  "allowTpSigns": true,
  "allowPresetSigns": true,
  "rawCommands": {
    "enabled": true,
    "requireOp": true
  },
  "presets": {
    "spawn": "tp @p 0 64 0"
  }
}
```

- `allowTpSigns`：是否启用 `[tp]` 告示牌。
- `allowPresetSigns`：是否启用 `[preset]` 告示牌。
- `rawCommands.enabled`：是否启用 `[cmd]` 和 `/` 开头的原版指令告示牌。
- `rawCommands.requireOp`：为 `true` 时，`[cmd]`/`/` 告示牌仅 OP 可用；为 `false` 时所有玩家可用（风险自负）。
- `presets`：预设名到指令的映射。指令由服务器以管理员权限执行，相对坐标以点击玩家位置为基准。

修改配置后重启服务器生效。

## 说明与安全

- `[tp]` 告示牌只执行 `tp` 指令，因此所有玩家都可以使用，请勿在需要严格限制传送的服务器上放任玩家建造。
- `[cmd]` 和 `/` 告示牌会执行任意指令，默认仅 OP 可用。只有在完全信任所有玩家时才建议把 `requireOp` 设为 `false`。
- 指令以点击告示牌的玩家为执行者（`@p` 指向点击玩家），但权限提升为全部权限，因此可执行 `/tp` 等需要 OP 的指令。
- 玩家右键命令告示牌时，服务端会取消默认的告示牌编辑界面。
- 潜行（Shift）+ 右键可以打开告示牌编辑界面，不会触发指令。

## 构建

需要 JDK 21+ 和 Gradle 9.x，或使用项目内已有的本地依赖：

```bash
cd signtp
gradle build
```

产物在 `build/libs/signtp-1.0.0.jar`。

构建时使用 `libs/` 下的本地 Fabric Loader 与 Fabric API 模块 jar 作为编译依赖，不会打包进最终 jar。运行时仍需在服务器 `mods/` 安装 Fabric API。
