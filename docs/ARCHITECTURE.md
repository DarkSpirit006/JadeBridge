# Architecture

## Layout

```
src/main/java/com/darkspirit69/jadebridge/
├── JadeBridge.java          lifecycle: channels, listeners, shearable blocks
├── JadeBridgeBootstrap.java Brigadier command lifecycle (paper plugins have
│                            no commands section in their descriptor)
├── JadeProtocol.java        channel names, protocol version, size caps
├── Settings.java            config.yml
├── command/                 /jadebridge subcommands
├── protocol/                one class per wire message + request handlers
├── provider/                provider SPI, registry, vanilla implementations
│   ├── block/               13 block providers
│   └── entity/              12 entity providers
├── session/                 handshake state, rate limiting, counters
└── util/                    reflection, reach check, effect times, shearable blocks
```

The NMS surface is deliberately narrow: the request handlers touch
`ServerPlayer`/`ServerLevel`, providers read block entities and entities, and
everything else goes through the Bukkit/Paper API. There is no ProtocolLib and
no packet interception — Jade's channels arrive as plugin messages.

## Message flow

1. Boot: `onEnable` registers the three incoming and two outgoing channels, the
   listeners and the command; `ServerLoadEvent` (and datapack reloads) trigger
   the shearable block collection.
2. A Jade client joins and sends `jade:client_handshake`.
   `HandshakeHandler` validates the version string; on a match it opens a
   session and answers with `ServerHandshake.encode(...)`: config overrides,
   shearable block ids, provider ids.
3. The client sends `jade:request_block` / `jade:request_entity` while the
   player looks around. `BlockRequestHandler` / `EntityRequestHandler`:
   - require a session and a rate-limit token,
   - decode the payload strictly (trailing bytes, unknown providers and
     oversized counts reject the whole request),
   - resolve the target — block via `getBlockState`/`getBlockEntity` (only if
     the chunk is loaded), entity via `level.getEntity(id)` with dragon parts
     resolved through their parent,
   - run the reach and (for entities) the canBeTarget check; failures still
     answer, but with coordinates only,
   - collect data: providers registered for the target's class hierarchy,
     intersected with the providers the client asked for, each writing into a
     fresh scratch buffer copied into the response tag under its uid,
   - send through `ReceiveData`, which applies Jade's 16 KiB trimming.
4. Disconnect (`PlayerQuitEvent`) drops the session.

## Provider registry

`ProviderRegistry` mirrors Jade's `PairHierarchyLookup`: registrations are
matched against the target's class hierarchy (block side: block class and block
entity class, merged), ordered most-specific class first with ties broken by
priority and then registration order. Wire ids are handed out in registration
order; because the client remaps to the server's order during the handshake,
they only need to be deterministic. `VanillaProviders.create()` registers the
vanilla set in the same order Jade's plugins do.

## Security model

Every incoming payload is untrusted:

- messages outside the three incoming channels never reach the plugin; players
  without a handshake are ignored entirely;
- per-player fixed-window rate limits (default 20/s per request type);
- payloads above 32 KiB are dropped before decoding (vanilla's custom payload
  cap);
- decoders validate structure strictly: provider counts capped at 256, unknown
  provider ids and trailing bytes reject the request;
- targets must be in loaded chunks and within `(interactionRange + deviation)²`
  of the player — no chunk loading, no teleporting targets;
- entities resolve only through the level's id lookup, never by scanning;
- provider exceptions are caught per provider and logged once, so one bad
  provider cannot take down a response;
- `command_block` and the two `loot_table` debug providers are additionally
  gated on gamemaster permission, like Jade's.

## Reflection points

Two classes own all reflection, both because Jade reaches these members through
its fabric classtweaker, which a Paper plugin cannot use:

- `Nms` — `Tadpole.getTicksLeftUntilAdult()`, `Armadillo.scuteTime`,
  `Villager.lastRestockGameTime`; lookups happen once at `onLoad` and fail soft.
- `ShearableBlocks` — the private internals of the vanilla loot structures
  (`LootTable.pools`, `LootPool.entries`/`.condition`,
  `LootPoolEntryContainer.condition`, composite `children`/`terms`,
  `NestedLootTable.value`), which changed shape in Minecraft 26.3.

## Testing

`src/test` holds 38 JUnit tests: byte-level round trips for every wire message
(primitive values, handshake, both request types, response trimming), the
provider registry's hierarchy/priority rules, the ViewGroup codec and the rate
limiter. `TestEnv` bootstraps vanilla registries in-process
(`SharedConstants`/`Bootstrap`) against the real server jar, so codecs run
against actual vanilla data. On top of that, the plugin was smoke-tested live
on a Paper 26.3-142 server (enable, shearable blocks, all commands).
