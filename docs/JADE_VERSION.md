# Target: Jade 26.3.4

JadeBridge implements the server half of Jade for the Minecraft 26.3 line.

| | |
| --- | --- |
| Jade version | 26.3.4 (fabric) |
| Source | github.com/Snownee/Jade, branch `26.3-fabric`, commit `556f6f8` (= tag `fabric-26.3.4`) |
| Minecraft | 26.3 |
| Protocol version | `"9"`, unchanged from 26.2 |
| Server tested against | Paper 26.3 build 142 (beta channel, latest at the time of writing) |

## How this was verified

- The full `snownee.jade` tree was diffed between `26.2-fabric` (`747effe`, the
  26.2 release the first JadeBridge was written against) and `26.3-fabric`.
  `ClientHandshakePacket`, `ServerHandshakePacket`, `ReceiveDataPacket` and
  `ShowOverlayPacket` are byte-identical; the two request packets only gained a
  per-player throttle call. `Jade.PROTOCOL_VERSION` is still `"9"`.
- Every server-side data provider (`StreamServerDataProvider` /
  `IServerDataProvider` implementations under `addon/`) was diffed individually;
  the outcome is recorded in [JADE_FEATURE_MATRIX.md](JADE_FEATURE_MATRIX.md).
- JadeBridge is compiled against the real mojang-mapped Paper 26.3
  server jar and was smoke-tested live on a Paper 26.3-142 server.

## What changed in Jade 26.3 (server-relevant)

- **New provider `minecraft:villager_restock`** (villagers only): restocks used
  today plus ticks until the next restock, for villagers that used a trade.
  Implemented.
- **`Jade.canBeTarget` gate on entity requests**: removed entities, spectators,
  the player's own vehicle, non-pickable dragon bodies and invisible entities
  without armor now get the position-only response. Implemented.
- **Trial spawner cooldown** is only sent while the spawner block entity is in
  its `COOLDOWN` state. Implemented.
- **Server-side request throttle**: Jade's own server now drops requests less
  than 50 ms apart (per player, shared by both request types). JadeBridge has
  its own per-type rate limits, which is strictly more permissive than Jade's
  throttle and closer to the single-player behavior (the integrated server
  bypasses throttling entirely).
- **Loot API reshuffle**: loot conditions are now a single
  `Optional<Holder<LootItemCondition>>` (field `condition`, formerly a
  `conditions` list), any-of terms are a `HolderSet`, and nested loot table
  entries carry a `HolderSet<LootTable>` (`value`, formerly
  `Either<ResourceKey, LootTable> contents`). The shearable-blocks collector
  was rewritten for these shapes and verified live (36 blocks on vanilla 26.3).
- Cosmetic only: every provider gained a `ConfigIcon` for the client's config
  screen, and Jade-internal `typedBlockEntity()` calls became defensive
  `getBlockEntity() instanceof` checks.

## Compatibility

- Jade 26.3.x clients on Paper 26.3: fully supported.
- The wire protocol is identical to 26.2, so supporting 26.2 servers as well is
  only a matter of building against the 26.2 line; no protocol handling differs.

## Register/handshake ordering

The Jade client can get its `jade:client_handshake` to the server before its
`minecraft:register` — sent right after the vanilla join — does. Bukkit
silently drops plugin messages to channels the receiving player has not
registered, and Jade never retries its handshake, so an eager reply would be
lost: the session would open server-side while the client never received the
config, shearable blocks or provider ids, and none of the server-provided data
would show up.

JadeBridge therefore sends the reply immediately when the channel is already
registered, holds it until registration otherwise, and drops held replies on
disconnect. Verified with a protocol-level bot client that performs a full
vanilla join and speaks the Jade channels: handshake-before-register,
register-first, and block/entity request round trips all pass. The ordering
rule is documented in [JADE_PROTOCOL.md](JADE_PROTOCOL.md).
