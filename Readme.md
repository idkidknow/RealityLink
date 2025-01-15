# RealityLink

A neat Minecraft in-game chat interacting API with server-side l10n support, suitable for chatbots.

## Dependencies

- Minecraft 1.7.10 with Forge: [Unimixins](https://github.com/LegacyModdingMC/UniMixins) ([CurseForge](https://www.curseforge.com/minecraft/mc-mods/unimixins))
- Minecraft 1.12.2 with Forge: [MixinBooter](https://github.com/CleanroomMC/MixinBooter) ([CurseForge](https://www.curseforge.com/minecraft/mc-mods/mixin-booter))
- Other version: no dependencies

## Getting Started

1. Download the mod and put it into your `mods` folder.
2. Restart your Minecraft server.
3. Run the command `/reallink download` to download vanilla language assets from Mojang.
4. Check and alter the config file `config/reallink/server.toml`. For details, see [Configuration](#configuration).
5. Run the command `/reallink start` to start the WebSocket server.

## Usage

This mod runs on a Minecraft server and
establishes a WebSocket server at `ws(s)://host:port/minecraft-chat`.
The server sends in-game chat messages as text frame
`{"json": "${raw JSON text format}", "translatedText": "${sever-side translated text}"}`
to clients. ([Raw JSON text format](https://minecraft.wiki/w/Raw_JSON_text_format))

Send `{"type": "literal", "text": "${your message}"}`
to broadcast a literal message in the game.

Send `{"type": "json", "json": "${raw JSON text format}"}`
to broadcast a rich message in the game.

### Preparing

Minecraft server don't hold redundant l10n resources
which are thought to be at client-side. If you have some resource packs
or mods' jar files inside which language resources
(files like `/assets/{namespace}/lang/{locale_code}.json`) exists,
create a folder and copy all these packs into it.

Under normal circumstances,
minecraft server don't have language files for vanilla contents.
It is required to download Minecraft's language resources
`assets/minecraft/lang/*.json`, create a zip file of
the `assets` folder and then the zip archive is virtually a resource pack
(without metadata `pack.mcmeta`. It doesn't matter.)
All these steps above can be done by a single command `/reallink download`
result in `serverlang/vanilla.zip`.

#### For Minecraft below 1.13

Those language files' extension is `.lang` instead of `.json`.

### Configuration

create a toml file `config/reallink/server.toml`:

```toml
host = "0.0.0.0"
port = 39244
localeCode = "en_us"
resourcePackDirs = ["mods", "serverlang"]
autoStart = false
```

- `port`: The port server listens
- `localeCode`: See [wiki](https://minecraft.wiki/w/Language)
- `resourcePacksDirs`: Folders where resource packs are stored.
Absolute path, or relative to the Minecraft game's root path
(commonly the parent of `mods`, `config`, etc.). It is
recommended to include `mods` and `serverlang` folder so that
language files in mods and vanilla Minecraft can be loaded.
- `autoStart`: Start the API server automatically
when Minecraft server starting.

#### For Minecraft below 1.13

localeCode is in the format of `en_US`, not `en_us`.

#### TLS/mTLS (optional)

Append these lines in `server.toml`:

```toml
certChain = "server_cert.pem"
privateKey = "server_pkcs8.key"
# root = "client_cert.pem" # optional
```

Absolute path, or relative to `config/reallink/`

- `certChain`: The server's certificate chain
- `privateKey`: The private key in PKCS #8 format
- optional `root`: Trusted root certificates, used to verify
certificates of clients.
Use normal TLS if not provided, otherwise enable mutual TLS.

### Launch

Start the Minecraft server, and then
use the command `/reallink start` to launch the API server.

The server will be launched automatically if `autoStart = true` in
`server.toml`.
