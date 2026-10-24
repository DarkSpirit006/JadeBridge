# The Jade wire protocol

Everything below was read out of the Jade 26.3.4 sources (`snownee.jade.network`
and `snownee.jade.impl`, branch `26.3-fabric`); the byte-level layouts are
covered by JadeBridge's unit tests. Jade's protocol version for the 26.x line
is `"9"`.

## Channels

| Channel | Direction | Purpose |
| --- | --- | --- |
| `jade:client_handshake` | client → server | announce protocol version |
| `jade:server_handshake` | server → client | config overrides, shearable blocks, provider ids |
| `jade:request_block` | client → server | ask for block data |
| `jade:request_entity` | client → server | ask for entity data |
| `jade:receive_data` | server → client | the answer |

`jade:show_overlay` also exists in Jade but only travels between Jade components
on one client, so a server never sees it. Outgoing plugin messages are only
delivered to players whose client registered the channel — that is how Jade
itself gates delivery, and why non-Jade clients never receive anything.

## Handshake

The client sends its protocol version as a UTF string (max 16 chars). On a
version mismatch the server sends the translatable chat message
`jade.protocolMismatch` and opens no session. Otherwise the server opens a
session and answers on `jade:server_handshake` with, in order:

1. **Config map** — VarInt entry count, then per entry an `Identifier` key and a
   primitive value: tag byte `0`/`1` = boolean, `2` = VarInt, `3` = float,
   `4` = UTF string, `20 + n` = small integer `n`. JadeBridge only sends entries
   configured under `config-overrides`.
2. **Shearable blocks** — VarInt count, then the raw `minecraft:block` registry
   ids (VarInt each) of every block whose loot table drops something when broken
   with shears, plus tripwire. Jade uses this list for its harvest-tool feature
   (`LootTableMineableCollector`).
3. **Block provider ids** — VarInt count, then `Identifier`s.
4. **Entity provider ids** — same layout.

The provider id lists are in the server's own registration order. The client
remaps its provider `IdMapper` to that order (`IHierarchyLookup.remapIds`), so
the order only needs to stay deterministic; it does not have to match Jade's own
numbering.

**Ordering.** The client can get its handshake to the server before its
`minecraft:register` does. Bukkit silently drops plugin messages to channels the
receiving player has not registered, and Jade does not retry its handshake, so
the reply goes out immediately only if `jade:server_handshake` is already among
the player's listening channels — otherwise it is held until the registration
arrives (`PlayerRegisterChannelEvent`) and then sent. A held reply is dropped if
the player disconnects first.

## Requests

Both request types end with a VarInt provider count followed by one VarInt wire
id per provider the client wants data from. JadeBridge caps the count at 256;
requests with unknown ids or trailing bytes are dropped.

`jade:request_block`:

| Field | Codec |
| --- | --- |
| showDetails | BOOL |
| hit | `BlockHitResult.STREAM_CODEC` |
| serversideRep | `ItemStack.OPTIONAL_STREAM_CODEC` — the client's pick-block item |
| data | `COMPOUND_TAG` — client-side state, contains the `SortItems` marker |
| providers | VarInt count + VarInt ids |

`jade:request_entity`:

| Field | Codec |
| --- | --- |
| showDetails | BOOL |
| entityId | VAR_INT |
| partIndex | VAR_INT — ender dragon part, `-1` otherwise |
| hitVec | VECTOR3F |
| data | `COMPOUND_TAG` |
| providers | VarInt count + VarInt ids |

## Responses

Answers travel on `jade:receive_data` as a single `COMPOUND_TAG`:

- block answers carry `x` / `y` / `z` (ints) and `BlockId` (the raw block
  registry id);
- entity answers carry `EntityId` (int);
- every data provider that produced data appends its value **under its uid
  string key** as a `ByteArrayTag` holding the raw codec bytes — Jade's
  `StreamServerDataProvider` convention. The vanilla `next_entity_drop`
  provider writes plain int tags instead (`NextEggIn`, `NextScuteIn`,
  `NextSniffIn`);
- `minecraft:item_storage` uses Jade's `ViewGroup` layout: a map entry keyed by
  a container id whose value is a list of `(icon stacks, count)` groups; counts
  above 99 travel as a 99-count stack carrying the real count in
  `CUSTOM_DATA.__JadeCount`.

If the compound exceeds 16 KiB, Jade's trimming runs before sending: at most ten
rounds of removing the largest child tag, descending one level into compounds
(`ReceiveDataPacket.removeLargest`). JadeBridge ports that logic verbatim so
oversized answers degrade the same way they do with Jade's own server.

## Behavior rules

- **Sessions.** Only players that completed a handshake get answers; sessions
  are dropped on disconnect.
- **Ordering.** The server handshake may have to wait for the client's
  `minecraft:register`; see *Handshake* above.
- **Reach.** Targets are validated against `(interactionRange + deviation)²`
  measured from the player's block position. Out-of-reach or unloaded targets
  still get an answer, but with coordinates only — same as Jade's server. The
  deviation defaults to 21 blocks.
- **Targetable entities (Jade 26.3).** Removed entities, spectators, the
  player's own vehicle, non-pickable dragon bodies and invisible entities
  without armor also get the coordinates-only answer.
- **Missing entities** get no answer at all.
- **Entity resolution** goes through `level.getEntity(id)` — never a scan — with
  dragon part indices resolved via `EnderDragon.getSubEntities()`.
- **Rate limits.** JadeBridge allows 20 block and 20 entity requests per player
  per second by default (configurable). Jade 26.3's own server throttles all
  requests at 20/s combined; the client normally asks a few times per second.
- **Size caps.** Incoming payloads over 32 KiB (vanilla's custom payload limit)
  and anything that does not decode cleanly are dropped silently.
