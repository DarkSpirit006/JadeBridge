# Contributing

Bug reports, compatibility fixes and focused improvements are welcome.

## Set up the project

- Install JDK 25.
- Clone the repository.
- Run `./gradlew build` (or `gradlew.bat build` on Windows).

The first build downloads Paper's mapped development bundle. The test suite does not require a
running Minecraft server.

## Before opening a pull request

- Run `./gradlew build` and include any relevant test changes.
- Keep protocol changes aligned with the targeted Jade release. Update `docs/JADE_PROTOCOL.md`,
  `docs/JADE_VERSION.md` and the feature matrix when their information changes.
- Keep provider behavior consistent with Jade's server-side implementation unless a deliberate
  difference is documented.
- Avoid adding dependencies when the Paper API or JDK already provides what is needed.
- Do not commit generated files from `build/`, local server data or IDE workspace settings.

For bug reports, include the Minecraft version, Paper/Purpur build, Java version and relevant
server log output. Remove IP addresses, tokens and other private data before posting logs.
