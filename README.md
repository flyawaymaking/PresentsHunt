# 🎁 PresentsHunt - Gift Hunt

A Minecraft Paper server plugin that adds an exciting gift hunt (event items) with various themed modes. Players search for hidden heads (event items) across the world and earn rewards for collecting them.

* Русский перевод конфига расположен [ЗДЕСЬ](/src/main/resources/ru_config.yml)

## 🧩 Version Compatibility

| **Plugin Version** | **Supported Paper**  | **Java** |
|--------------------|----------------------|----------|
| `1.3.0`            | `1.20.1` – `26.2`    | 25       |
| `1.2.1`            | `1.20.1` – `1.21.11` | 21       |

## ✨ Features

- 🎄 **Themed Modes**: Christmas, Halloween, Easter, and Custom mode
- 🏆 **Achievement System**: Players collect event items to earn rewards
- 📊 **Leaderboard**: Player rankings with PlaceholderAPI integration
- 🎯 **Admin Tools**: Easy management and cleanup of event items (no need to remember exact locations)
- 🔊 **Effects**: Particles and sounds when interacting with event items
- 📝 **Configurable Messages**: MiniMessage format support
- 🔧 **API Integration**: PlaceholderAPI support for integration with other plugins

## 📥 Installation

1. Download the latest version of the plugin from [Releases](../../releases)
2. Place the `PresentsHunt.jar` file into the `plugins/` folder
3. Restart the server
4. Configure the `plugins/PresentsHunt/config.yml` file and apply changes with `/presentshunt reload`
5. Use the `/presentshunt give` command to obtain an event head

## ⚙️ Configuration

Main settings in `config.yml`:

```yaml
# Mode Selection (HALLOWEEN, CHRISTMAS, EASTER, CUSTOM)
presentsMode: CHRISTMAS

# How many event items need to be found to receive a reward
totalPresents: 30

# Commands for each found event item and when the player finds all
# You can use %player%, %found%, and %total% placeholders in these commands
commands:
  foundCommands: [ ]
  rewardCommands:
    - "give %player% diamond 10"

leaderboard:
  maxPlayersCount: 100 # Number of players stored in the leaderboard for position display

# Music when interacting with a head
sounds:
  found: "block.pumpkin.carve"
  alreadyFound: "entity.zombie.ambient"
  complete: "entity.firework_rocket.blast"

# Effects when interacting with a head
particles:
  found: "SWEEP_ATTACK"
  alreadyFound: "SQUID_INK"
```

### Head Textures
Preset textures are available for each mode:
- **CHRISTMAS**: Christmas gift
- **HALLOWEEN**: Halloween pumpkin
- **EASTER**: Easter egg
- **CUSTOM**: Custom head (configure your own texture)

## 🎮 Usage

### For Players
1. Find hidden event items throughout the world
2. Right-click a head to collect it
3. Collect event items to earn rewards
4. Use `/presentshunt stats` to view your statistics

### For Administrators
```
/presentshunt give - Get a head (event item)
/presentshunt stats - Display plugin statistics
/presentshunt reload - Reload the configuration
/presentshunt locate [radius] - Find event items within the radius
/presentshunt cleanup [radius] - Remove event items within the radius
/presentshunt resetplayer <player> - Reset a player's data
/presentshunt resetall - Reset all players' data
/presentshunt setmode [mode] - Set a new event mode
/presentshunt replace [mode] [radius] - Replace all event items of the selected mode with the current one within the radius
```

### Placing Event Items
1. Obtain a head using the `/presentshunt give` command
2. Place the head anywhere in the world
3. The head is automatically marked as an event item for collection

## 📊 PlaceholderAPI

The plugin supports PlaceholderAPI with the following placeholders:

```
%presentshunt_found% - Number of event items found
%presentshunt_total% - Total number of event items
%presentshunt_mode% - Current hunt mode
%presentshunt_completed% - Number of players who completed the hunt
%presentshunt_players% - Number of players with data
%presentshunt_position% - Position in the leaderboard
%presentshunt_status% - Completion status (Completed/In progress/Not started)
%presentshunt_top_1_status% - Status of the player in 1st place
%presentshunt_top_2_status% - Status of the player in 2nd place
... and so on up to 10th place
```

## 🔧 Permissions

```
presentshunt.use - Collect event items (default: true)
presentshunt.admin - Administrative commands (default: op)
```

## 🐛 Bugs and Suggestions

Found a bug or have a suggestion for improvement? Create an [Issue](../../issues) on GitHub.

## 🤝 Contributing

Want to help with plugin development?
1. Fork the repository
2. Create a branch for your feature (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
