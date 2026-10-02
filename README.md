# JadeBridge

Server-side support for the [Jade](https://github.com/Snownee/Jade) client mod on
[Paper](https://papermc.io/) / Purpur servers for Minecraft 26.3.

Jade shows you what you are looking at. Half of that data only exists on the
server — brewing stands, beehives, animal owners, breeding cooldowns, restock
timers and so on — which is why Jade ships with a small server half. JadeBridge
is that server half as a Paper plugin: players keep the normal Jade client and
its UI, and it behaves like it does in single-player.

- Targets **Jade 26.3.x** (protocol version `9`)
- Requires **Paper or Purpur 26.3** and **Java 25**
- Players without Jade are never touched

## How it works

The plugin registers Jade's five custom payload channels. When a Jade client
joins it sends a handshake; JadeBridge answers with config overrides, the list
of shearable blocks and its data provider ids. While you look around, the client
requests block and entity data a few times per second, and JadeBridge answers
with an NBT compound built by its vanilla data providers. Everything the client
can compute itself (health, spawner contents, progress bars, ...) is left to the
client, exactly like Jade's own server half.

Documentation:

- [docs/JADE_PROTOCOL.md](docs/JADE_PROTOCOL.md) — the wire format
- [docs/JADE_FEATURE_MATRIX.md](docs/JADE_FEATURE_MATRIX.md) — every provider and what it sends
- [docs/JADE_VERSION.md](docs/JADE_VERSION.md) — the exact Jade version targeted and how that was verified
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — code layout, security model, testing

## Commands

All subcommands require the `jadebridge.admin` permission.

| Command | Description |
| --- | --- |
| `/jadebridge status` | version, sessions, provider and request counters |
| `/jadebridge providers` | registered block/entity provider ids |
| `/jadebridge reload` | reload `config.yml`, reset counters |
| `/jadebridge debug [on\|off]` | verbose request/handshake logging |

## Configuration

```yaml
enabled: true
debug: false
rate-limit:
  block-requests: 20
  entity-requests: 20
max-position-deviation: 21
config-overrides: {}
```

`config-overrides` syncs server-side values into the client's Jade config during
the handshake (keys are Jade config ids like `minecraft:potion_effects.limit`,
values are booleans, numbers or strings).

## Building

```sh
./gradlew build
```

The build uses [paperweight-userdev](https://docs.papermc.io/paper/dev/userdev/),
which downloads the mojang-mapped Paper 26.3 server and generates the dev
runtime, so the first build takes a few minutes and a few GB of RAM. The test
suite bootstraps vanilla registries in-process and needs no running server.

## What was verified

- Compiled directly against the real mojang-mapped Paper 26.3 server jar (build 142)
- 41 unit tests pass, covering every wire message, the provider registry and
  the handshake deferral
- Live smoke test on a Paper 26.3-142 server: plugin enable, shearable block
  collection and all `/jadebridge` subcommands
- End-to-end verification with a protocol-level bot client that performs a full
  vanilla join and speaks the Jade channels: handshake, block requests and
  entity requests, in both the register-first and handshake-first orders (the
  latter is what a real Jade client produces)
- All protocol and provider behavior was read out of the Jade 26.3.4 sources; see
  [docs/JADE_VERSION.md](docs/JADE_VERSION.md)

## Limitations

- Jade's own server registers a `jade:max_position_deviation` game rule; the
  game rule registry is frozen before Paper plugins load, so the deviation is a
  config value here instead (`max-position-deviation`, same default of 21).
- Paper 26.3 is currently only published on the beta channel; JadeBridge is
  built and tested against build 142 and needs no beta-specific APIs.
- The client-side parts of Jade (progress bars, fluids, energy, ...) are out of
  scope by nature; see the feature matrix for what is server data and what is not.

## License

MIT — see [LICENSE](LICENSE).
