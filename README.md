# World Templates

A Hytale server plugin for creating world instances from YAML template files.

Define your world settings, spawn point, and prefabs in a simple YAML format, then spawn instances on-demand with a single command.

## Features

- **YAML Templates** - Human-readable configuration for world instances
- **Prefab Placement** - Automatically paste prefabs relative to spawn point
- **World Settings** - Configure weather, time, game mode, PvP, and more
- **Hot Reload** - Update templates without restarting the server
- **Auto-Cleanup** - Instances automatically removed when empty

## Installation

1. Build the plugin:
   ```bash
   gradle shadowJar
   ```

2. Copy to your server:
   ```bash
   cp build/libs/WorldTemplates-1.0.0.jar /path/to/server/Server/mods/
   ```

3. Create a `WorldTemplates` directory:
   ```bash
   mkdir -p /path/to/server/Server/WorldTemplates/
   ```

## Quick Start

Create a template file at `Server/WorldTemplates/my-arena.template.yaml`:

```yaml
name: my-arena
displayName: "Battle Arena"

spawnPoint:
  x: 0.0
  y: 65.0
  z: 0.0
  yaw: 180.0
  pitch: 0.0

worldSettings:
  gameMode: Survival
  forcedWeather: Zone1_Sunny
  gameTimePaused: true
  spawningNPC: false
  pvpEnabled: true

prefabs:
  - id: arena
    path: MyArena/arena.prefab.json
    x: 0.0
    y: -1.0
    z: 0.0
    rotation: None
```

Then in-game:
```
/wt:list                  # See available templates
/wt:create my-arena       # Create and teleport to instance
/instance:exit            # Return to origin
```

## Commands

| Command | Description |
|---------|-------------|
| `/wt:create <name>` | Create world from template and teleport |
| `/wt:list` | List available templates |
| `/wt:reload` | Reload templates from disk |

## Template Reference

### Spawn Point

```yaml
spawnPoint:
  x: 0.0      # X coordinate
  y: 65.0     # Y coordinate (height)
  z: 0.0      # Z coordinate
  yaw: 180.0  # Horizontal rotation (0-360)
  pitch: 0.0  # Vertical rotation (-90 to 90)
```

### World Settings

All settings are optional and override VoidTemplate defaults:

| Setting | Type | Description |
|---------|------|-------------|
| `gameMode` | String | `Survival`, `Creative`, `Adventure` |
| `forcedWeather` | String | Weather type (e.g., `Zone1_Sunny`) |
| `gameTimePaused` | Boolean | Freeze day/night cycle |
| `gameTime` | String | Time in `HH:MM` format |
| `spawningNPC` | Boolean | Enable mob spawning |
| `pvpEnabled` | Boolean | Enable PvP combat |
| `fallDamageEnabled` | Boolean | Enable fall damage |
| `ticking` | Boolean | World physics |
| `blockTicking` | Boolean | Block updates |

### Prefabs

```yaml
prefabs:
  - id: my_prefab              # Optional identifier for logging
    path: Folder/file.prefab.json  # Relative to Server/prefabs/
    x: 0.0                     # X offset from spawn
    y: -1.0                    # Y offset from spawn
    z: 0.0                     # Z offset from spawn
    rotation: None             # None, Ninety, OneEighty, TwoSeventy
```

## Requirements

- Hytale Server (2026.01.28 or later)
- `Hytale:Instances` plugin (built-in)

## Building from Source

Requires Java 25 and Gradle:

```bash
git clone https://github.com/zacx-z/world-templates.git
cd world-templates
gradle shadowJar
```

Output: `build/libs/WorldTemplates-1.0.0.jar`

## License

MIT License - See [LICENSE](LICENSE) for details.
