# Reality Communication

A neat Minecraft in-game chat interacting API with server-side l10n support, suitable for chatbots.

## Usage

This mod runs on a Minecraft server and
establishes a gRPC server. Check
the proto file at `common/src/main/proto/realcomm.proto`.

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
(without metadata `pack.mcmeta`. It doesn't matter) so simply do the
things that the previous paragraph tells you to do.

### Configuration

create a json file like and put it in `config/realcomm/`:
```json
{
  "port": 11451,
  "localeCode": "zh_cn",
  "resourcePacksDir" : "serverlang",
  "certChain": "server_cert.pem",
  "privateKey": "server_pkcs8.key",
  "trustCerts": "client_cert.pem"
}
```

- `port`: The port gRPC server listens
- `localeCode`: See https://minecraft.wiki/w/Language
- `resourcePacksDir`: The folder where resource packs are stored.
Relative path will be resolved by the Minecraft game's root path
(where the game's jar file is)

Next 3 fields are optional. If provided, gRPC server will use (m)TLS
to authenticate.
They are all file paths. Relative path will be resolved by `config/realcomm/`
- `certChain`: The server's certificate
- `privateKey`: The private key in PKCS #8 format
- optional `trustCerts`: Trusted root certificates, used to verify
certificates of clients.
Use normal TLS if not provided, otherwise mutual TLS will be enabled.

### Launch

Start the Minecraft server, and then
use the command `/realcomm start {yourfilename}.json` to launch the
gRPC server.

gRPC server will be launched automatically if there's a `autostart.json`.
