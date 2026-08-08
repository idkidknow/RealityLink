# RealityLink

A neat Minecraft in-game chat interacting API with server-side l10n support, suitable for chatbots.

## Dependencies

- Minecraft 1.7.10 with Forge: [Unimixins](https://github.com/LegacyModdingMC/UniMixins)
  ([CurseForge](https://www.curseforge.com/minecraft/mc-mods/unimixins))
- Minecraft 1.12.2 with Forge: [MixinBooter](https://github.com/CleanroomMC/MixinBooter)
  ([CurseForge](https://www.curseforge.com/minecraft/mc-mods/mixin-booter))
- Other version: no dependencies

## Getting Started

1. Download the mod and put it into your `mods` folder.
2. Restart your Minecraft server.
3. Run the command `/realitylink download` to download vanilla language assets from Mojang.
4. Check and alter the config file `config/realitylink/server.toml`. For details, see [Configuration](#configuration).
5. Run the command `/realitylink start` to start the WebSocket server.

## Usage

### Chat

This mod runs on a Minecraft server and establishes a WebSocket server at `ws://host:port/minecraft-chat`. The server
sends in-game chat messages as text frame
`{"json": "${raw JSON text format}", "translatedText": "${sever-side translated text}"}`
to clients. ([Raw JSON text format](https://minecraft.wiki/w/Raw_JSON_text_format))

Send `{"type": "literal", "text": "${your message}"}`
to broadcast a literal message in the game.

Send `{"type": "json", "json": "${raw JSON text format}"}`
to broadcast a rich message in the game.

### Statistics

Query a player's statistic:

```
GET http://host:port/stats/{uuid}/{statName}
```

Returns `int | null`.

For example:

```
GET http://host:port/stats/00000000-0000-0000-0000-000000000000/minecraft.custom:minecraft.play_time
```

See [Wiki: Statistics](https://minecraft.wiki/w/Statistics), [Wiki: Statistics for Minecraft below 1.13](https://minecraft.wiki/w/Statistics?oldid=1282014)

### Preparing

Minecraft server don't hold redundant l10n resources which are thought to be at client-side. If you have some resource
packs or mods' jar files inside which language resources (files like `/assets/{namespace}/lang/{locale_code}.json`)
exists, create a folder and copy all these packs into it.

Under normal circumstances, minecraft server don't have language files for vanilla contents. It is required to download
Minecraft's language resources
`assets/minecraft/lang/*.json`, create a zip file of the `assets` folder and then the zip archive is virtually a
resource pack (without metadata `pack.mcmeta`. It doesn't matter.)
All these steps above can be done by a single command `/realitylink download`
result in `serverlang/vanilla.zip`.

#### For Minecraft below 1.13

Those language files' extension is `.lang` instead of `.json`.

### Configuration

create a toml file `config/realitylink/server.toml`:

```toml
host = "0.0.0.0"
port = 39244
localeCode = "en_us"
resourcePackDirs = ["mods", "serverlang"]
autoStart = false
```

- `port`: The port server listens
- `localeCode`: See [wiki](https://minecraft.wiki/w/Language)
- `resourcePacksDirs`: Folders where resource packs are stored. Absolute path, or relative to the Minecraft game's root
  path (commonly the parent of `mods`, `config`, etc.). It is recommended to include `mods` and `serverlang` folder so
  that language files in mods and vanilla Minecraft can be loaded.
- `autoStart`: Start the API server automatically when Minecraft server starting.

#### For Minecraft 1.7.10

localeCode is in the format of `en_US`, not `en_us`.

### Launch

Start the Minecraft server, and then use the command `/realitylink start` to launch the API server.

The server will be launched automatically if `autoStart = true` in
`server.toml`.

## Development

The shared code is written in Scala 3 under `realitylink/`. Each version has a `package.mill` and a standalone Gradle
project under `platform/` that fetches the Minecraft and mod loader things and handles remapping, mixins and dev runs.

For IDE support, enable BSP by adding the module name to
`.enableBsp`. For example, create `.enableBsp` file with

```
neo1_21_1
forge1_7_10
```

which enables IDE support for `neo1_21_1` and `forge1_7_10`.

### Building

Build the mod jar with

```sh
./mill forge1_20_1.jar
```

You can replace `forge1_20_1` with: `forge1_7_10`,
`forge1_12_2`, `forge1_16_5`, `forge1_18_2`, `forge1_19_2`,
`forge1_20_1` or `neo1_21_1`.

### Run (dev mode)

```sh
./mill forge1_20_1.runClient
./mill forge1_20_1.runServer
```
