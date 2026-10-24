<div align="center">

<img src="assets/JadeBridge.png" alt="JadeBridge logo" width="160">

# JadeBridge

Server-side support for the [Jade](https://github.com/Snownee/Jade) client mod on
[Paper](https://papermc.io/) and Purpur for Minecraft 26.3.

[![Build][build-badge]][build-link]
[![Code Quality][codefactor-badge]][codefactor-link]
[![Release][release-badge]][release-link]
[![Downloads][downloads-badge]][downloads-link]
[![Stars][stars-badge]][stars-link]
[![Java][java-badge]][java-link]
[![Gradle][gradle-badge]][gradle-link]
[![Paper API][paper-api-badge]][paper-link]
[![License][license-badge]][license-link]

[![bStats Statistics](https://bstats.org/signatures/bukkit/JadeBridge.svg)](https://bstats.org/plugin/bukkit/JadeBridge/34905)

</div>

JadeBridge supplies the server-side data that the Jade client cannot read on its own,
including container contents, brewing progress, beehive contents, animal ownership,
breeding cooldowns and villager restock timers. Players still need the Jade client mod;
players without Jade are ignored.

JadeBridge uses bStats to collect anonymous, aggregate plugin usage statistics. Set
`bstats.enabled` to `false` in the plugin configuration to opt out.

## Compatibility

| Component | Target |
| --- | --- |
| Minecraft | 26.3 |
| Server | Paper 26.3, development bundle build 142 |
| Jade client | 26.3.x, protocol version `9` |
| Java | 25 |

Purpur 26.3 is intended to be compatible but has not been tested separately. The build is pinned
to Paper's 26.3 build 142 development bundle. When changing the Minecraft or Jade target, review
the protocol notes and feature matrix before updating the build configuration.

## Installation

1. Download the JadeBridge JAR from [Modrinth][modrinth-link] or [GitHub Releases][release-link],
   or build it from source.
2. Put the JAR in the server's `plugins` directory.
3. Start or restart the server.
4. Make sure players have the matching Jade 26.3.x client mod installed.

On first startup, JadeBridge creates `plugins/JadeBridge/config.yml`.

## Commands

Every command requires the `jadebridge.admin` permission, which defaults to operators. Grant it
to other roles through your permissions plugin.

| Command | Description |
| --- | --- |
| `/jadebridge status` | Show the protocol version, active sessions and request counters |
| `/jadebridge providers` | List registered block and entity providers |
| `/jadebridge reload` | Reload the configuration and reset request counters |
| `/jadebridge update` | Check the latest stable Modrinth release and show the install link |
| `/jadebridge debug` | Show the current debug setting |
| `/jadebridge debug on` | Enable debug logging |
| `/jadebridge debug off` | Disable debug logging |

## Configuration

```yaml
enabled: true
debug: false
bstats:
  enabled: true
rate-limit:
  block-requests: 20
  entity-requests: 20
max-position-deviation: 21
config-overrides: {}
```

- `enabled` controls whether Jade messages are handled.
- `debug` enables additional request and provider error logging.
- `bstats.enabled` controls anonymous aggregate metrics. Set it to `false` to disable
  JadeBridge's bStats reporting. Restart the server for this setting to take effect.
- `rate-limit.block-requests` and `rate-limit.entity-requests` set per-player request limits per
  second. New Jade sessions use the configured values; existing sessions keep their current limits.
- `max-position-deviation` is the extra distance allowed by the server-side reach check, in blocks.
  Jade registers this as a game rule in its own server implementation, but Paper plugins cannot add
  that game rule after registry initialization. This value takes effect when the configuration is
  reloaded.
- `config-overrides` sends values to the Jade client during its handshake. Keys are Jade config
  IDs; supported values are booleans, numbers and strings.

For example:

```yaml
config-overrides:
  'minecraft:entity_health': true
  'minecraft:potion_effects.limit': 4
```

## Build and test

Use JDK 25. From the repository root, run:

```bash
./gradlew build
```

On Windows, run `gradlew.bat build` in PowerShell or Command Prompt. The build uses
[Paperweight Userdev](https://docs.papermc.io/paper/dev/userdev/) and downloads the mapped Paper
development bundle on the first run. No running Minecraft server is needed for the unit tests.

The test suite covers protocol decoding and encoding, handshake handling, response-size trimming,
provider lookup, inventory view encoding and request rate limiting. The built plugin JAR is written
to `build/libs/`.

To publish a GitHub release, run the **Release** workflow manually. It reads the version from
Gradle and publishes the JAR with the matching `v`-prefixed tag.

## Documentation

- [Architecture](docs/ARCHITECTURE.md) — lifecycle, request flow and security checks
- [Protocol notes](docs/JADE_PROTOCOL.md) — channels, payloads and handshake ordering
- [Feature matrix](docs/JADE_FEATURE_MATRIX.md) — server-side providers and the data they return
- [Jade version notes](docs/JADE_VERSION.md) — source version and compatibility details

## Limitations

- The plugin implements server-side data only. Jade's client-side overlays and features that can be
  derived from vanilla client data remain the client's responsibility.
- Paper plugins cannot register Jade's `jade:max_position_deviation` game rule after the game-rule
  registry is frozen, so JadeBridge exposes the value through its own configuration instead.
- Access to a small number of private Minecraft members uses reflection. Those lookups may need
  updates when Minecraft changes its internal implementation.

## License

JadeBridge is released under the MIT License. See [LICENSE](LICENSE).

[build-badge]: https://img.shields.io/github/actions/workflow/status/DarkSpirit006/JadeBridge/build.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=Build
[build-link]: https://github.com/DarkSpirit006/JadeBridge/actions/workflows/build.yml
[codefactor-badge]: https://img.shields.io/codefactor/grade/github/DarkSpirit006/JadeBridge?style=for-the-badge&logo=codefactor&logoColor=white&label=Code%20Quality
[codefactor-link]: https://www.codefactor.io/repository/github/darkspirit006/jadebridge
[release-badge]: https://img.shields.io/github/v/release/DarkSpirit006/JadeBridge?style=for-the-badge&logo=github&logoColor=white&label=Release
[release-link]: https://github.com/DarkSpirit006/JadeBridge/releases/latest
[downloads-badge]: https://img.shields.io/modrinth/dt/jadebridge?style=for-the-badge&logo=modrinth&logoColor=white&label=Downloads
[downloads-link]: https://modrinth.com/plugin/jadebridge
[modrinth-link]: https://modrinth.com/project/jadebridge
[stars-badge]: https://img.shields.io/github/stars/DarkSpirit006/JadeBridge?style=for-the-badge&logo=github&logoColor=white&label=Stars
[stars-link]: https://github.com/DarkSpirit006/JadeBridge/stargazers
[java-badge]: https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fraw.githubusercontent.com%2FDarkSpirit006%2FJadeBridge%2Frefs%2Fheads%2Fmain%2Fbuild.gradle&search=JavaLanguageVersion%5C.of%5C%28%28%5Cd%2B%29%5C%29&replace=%241%2B&style=for-the-badge&logo=openjdk&logoColor=white&label=Java&color=f89820&cacheSeconds=300
[java-link]: https://adoptium.net/temurin/
[gradle-badge]: https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fraw.githubusercontent.com%2FDarkSpirit006%2FJadeBridge%2Frefs%2Fheads%2Fmain%2Fgradle%2Fwrapper%2Fgradle-wrapper.properties&search=gradle-%28%5B0-9.%5D%2B%29-bin&replace=%241&style=for-the-badge&logo=gradle&logoColor=white&label=Gradle&color=0f6b78&cacheSeconds=300
[gradle-link]: https://gradle.org/
[paper-api-badge]: https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fraw.githubusercontent.com%2FDarkSpirit006%2FJadeBridge%2Frefs%2Fheads%2Fmain%2Fbuild.gradle&search=paperweight%5C.paperDevBundle%5C%28%27%28%5B0-9.%5D%2B%29%5C.build%5C.%5Cd%2B&replace=%241&style=for-the-badge&logo=papermc&logoColor=white&label=Paper%20API&color=33b5e5&cacheSeconds=300
[paper-link]: https://papermc.io/
[license-badge]: https://img.shields.io/github/license/DarkSpirit006/JadeBridge?style=for-the-badge&logo=opensourceinitiative&logoColor=white&label=License
[license-link]: LICENSE
