# RealityLink

A neat Minecraft in-game chat interacting API with server-side l10n support, suitable for chatbots.

## 1.16.5 defect

Minecraft 1.16.5 uses Log4j 2.8.1 but there's no such thing as
a bridge between newest SLF4J and Log4j 2.8.1 (log4j-slf4j2-impl:2.8.1)
so we use slf4j-simple as a workaround.

defect: All logs are sent to stderr and shown with a verbose prefix in Minecraft's log.
Logging level defaults to INFO and is not easy to change.

## Usage

This mod runs on a Minecraft server and
establishes a WebSocket server.
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
inside which language resources
(files like `/assets/{namespace}/lang/{locale_code}.json`) exists,
create a folder and copy all these resource packs into it.

Under normal circumstances,
minecraft server don't have language files for vanilla contents
other than `en_us.json`. For users that speak other languages,
download Minecraft's language resources `assets/minecraft/lang/*.json`
on https://mcasset.cloud/, create a zip file of the `assets` folder.
Now the zip archive is virtually a resource pack
(without metadata `pack.mcmeta`. It doesn't matter.) so simply do the
things that the previous paragraph tells you to do.

### Configuration

create a toml file `config/reallink/server.toml`:
```toml
port = 39244
localeCode = "en_us"
resourcePackDir = "serverlang"
autoStart = true
```

- `port`: The port server listens
- `localeCode`: See https://minecraft.wiki/w/Language
- `resourcePacksDir`: The folder where resource packs are stored.
Absolute path, or relative to the Minecraft game's root path
(where the game's jar file is)
- `autoStart`: Start the API server automatically
when Minecraft server starting

#### TLS/mTLS

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
