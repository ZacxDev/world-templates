# World Templates

Create world instances from YAML template files with prefab placement and custom settings.

## Build & Install

```bash
gradle -p world-templates shadowJar
cp world-templates/build/libs/WorldTemplates-1.0.0.jar server/Server/mods/
```

The VoidTemplate instance is bundled in the JAR (requires `IncludesAssetPack: true`).

## Commands

| Command | Description |
|---------|-------------|
| `/wt:create <name>` | Create world from template and teleport into it |
| `/wt:list` | List all available templates |
| `/wt:reload` | Reload templates from disk (admin only) |

## Template Format

Templates are YAML files in `Server/WorldTemplates/*.template.yaml`

```yaml
# Required
name: my-template
displayName: "My Template"

# Spawn point - where players appear
spawnPoint:
  x: 0.0
  y: 65.0
  z: 0.0
  yaw: 180.0
  pitch: 0.0

# World settings (all optional)
worldSettings:
  gameMode: Survival          # Survival, Creative, Adventure
  forcedWeather: Zone1_Sunny  # Weather type
  gameTimePaused: true        # Freeze day/night cycle
  gameTime: "12:00"           # Time of day (HH:MM)
  spawningNPC: false          # Mob spawning
  pvpEnabled: true            # PvP combat
  fallDamageEnabled: true     # Fall damage
  ticking: true               # World physics
  blockTicking: true          # Block updates

# Prefabs to place (positions relative to spawn)
prefabs:
  - id: floor                 # Optional identifier
    path: Custom/floor.prefab.json  # Relative to Server/prefabs/
    x: 0.0
    y: -1.0
    z: 0.0
    rotation: None            # None, Ninety, OneEighty, TwoSeventy
```

## Project Structure

```
world-templates/
├── src/main/java/dev/zacx/worldtemplates/
│   ├── WorldTemplatesPlugin.java     # Plugin lifecycle
│   ├── template/
│   │   ├── WorldTemplate.java        # Template data model
│   │   ├── PrefabPlacement.java      # Prefab config
│   │   └── TemplateLoader.java       # YAML parsing
│   ├── command/
│   │   ├── WTCreateCommand.java      # /wt:create
│   │   ├── WTListCommand.java        # /wt:list
│   │   └── WTReloadCommand.java      # /wt:reload
│   └── world/
│       └── WorldSpawner.java         # Instance creation + prefab pasting
└── src/main/resources/
    ├── manifest.json
    └── Server/Instances/VoidTemplate/
        └── instance.bson             # Base void world template
```

## How It Works

1. **Template Loading**: On startup, loads all `*.template.yaml` files from `Server/WorldTemplates/`
2. **Instance Creation**: Uses `InstancesPlugin.spawnInstance()` with VoidTemplate as base
3. **Settings Applied**: Template settings override VoidTemplate defaults via `WorldConfig`
4. **Prefab Pasting**: Prefabs placed on world thread using `PrefabUtil.paste()`
5. **Teleportation**: Player teleported to spawn point with return transform saved

## World Settings Reference

| Setting | Type | Description |
|---------|------|-------------|
| `gameMode` | String | `Survival`, `Creative`, `Adventure` |
| `forcedWeather` | String | Weather type (e.g., `Zone1_Sunny`) |
| `gameTimePaused` | Boolean | Freeze day/night cycle |
| `gameTime` | String | Time in `HH:MM` format |
| `spawningNPC` | Boolean | Enable mob spawning |
| `spawnMarkersEnabled` | Boolean | Enable spawn markers |
| `allNPCFrozen` | Boolean | Freeze all NPCs |
| `pvpEnabled` | Boolean | Enable PvP combat |
| `fallDamageEnabled` | Boolean | Enable fall damage |
| `ticking` | Boolean | World physics ticking |
| `blockTicking` | Boolean | Block update ticking |
| `gameplayConfig` | String | Gameplay preset name |

## Prefab Placement

- **Path**: Relative to `Server/prefabs/` (lowercase)
- **Position**: Relative to spawn point (x, y, z offsets)
- **Rotation**: `None`, `Ninety` (90°), `OneEighty` (180°), `TwoSeventy` (270°)

```yaml
prefabs:
  - id: lobby
    path: HytaleGarage/spawn.prefab.json
    x: 0.0
    y: -2.0   # 2 blocks below spawn
    z: 0.0
    rotation: None
```

## VoidTemplate Base Settings

The bundled VoidTemplate (`Server/Instances/VoidTemplate/instance.bson`) provides:

- Void world generation (no terrain)
- Empty chunk storage (no persistence)
- Sunny weather (`Zone1_Sunny`)
- Frozen time (`IsGameTimePaused: true`)
- No mob spawning
- Auto-delete on removal

Templates can override these via `worldSettings`.

## Dependencies

- `Hytale:Instances` - Instance world management
- SnakeYAML 2.2 - YAML parsing (bundled)

## Common Gotchas

- Template files must end with `.template.yaml`
- Prefab paths are relative to `Server/prefabs/` (lowercase `p`)
- Prefab positions are relative to the spawn point
- Use `/wt:reload` after editing templates (no server restart needed)
- Exit instances with `/instance:exit` to return to origin

## Example: Lobby Template

```yaml
# Server/WorldTemplates/lobby.template.yaml
name: lobby
displayName: "Main Lobby"

spawnPoint:
  x: 0.0
  y: 66.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0

worldSettings:
  gameMode: Adventure
  forcedWeather: Zone1_Sunny
  gameTimePaused: true
  gameTime: "12:00"
  spawningNPC: false
  pvpEnabled: false

prefabs:
  - id: lobby_structure
    path: MyPack/lobby.prefab.json
    x: 0.0
    y: -2.0
    z: 0.0
    rotation: None
```

## Troubleshooting

**"No world templates available"**
- Check templates are in `Server/WorldTemplates/`
- Verify files end with `.template.yaml`
- Run `/wt:reload` to reload

**"Template not found"**
- Template names are case-insensitive
- Use the filename without `.template.yaml` extension

**"Failed to load prefab"**
- Prefabs go in `Server/prefabs/` (lowercase)
- Path in template is relative to that directory

**Foggy/dark world**
- Check `forcedWeather: Zone1_Sunny` is set
- Verify `gameTimePaused: true` and `gameTime: "12:00"`
