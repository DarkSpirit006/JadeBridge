# Feature matrix

Provider semantics below were taken from the Jade 26.3.4 sources; every server
provider in Jade's vanilla, core, universal and debug plugins is accounted for.
"Wire id" is the uid Jade's client registers the provider under.

## Block data providers

Registered in the same order Jade's plugins register theirs (core, vanilla,
universal, debug). The order defines the wire ids.

| Wire id | Target | Sends | Notes |
| --- | --- | --- | --- |
| `jade:object_name` | any block entity | display name `Component` | renamed containers; double chests merge to the pair name |
| `minecraft:brewing_stand` | brewing stand | fuel + remaining brew time (2 × VarInt) | |
| `minecraft:beehive` | beehive | bee count (byte), negative when the hive is not full | honey level is block state data |
| `minecraft:command_block` | command block | command (string) | gamemaster permission only; truncated to 40 chars like the client displays it |
| `minecraft:hopper_lock` | hopper | BOOL locked | locked = disabled by redstone |
| `minecraft:jukebox` | jukebox | disc `ItemStack` | |
| `minecraft:lectern` | lectern | book `ItemStack` | |
| `minecraft:redstone` | comparator, calibrated sculk sensor | signal strength (VarInt) | levers/repeaters/plain power are block state data |
| `minecraft:furnace` | any furnace | progress, total time, 3 × `ItemStack` | |
| `minecraft:shelf` | chiseled bookshelf, shelf | `ItemStack` in the slot being looked at | |
| `minecraft:mob_spawner.cooldown` | trial spawner | remaining cooldown (VarInt) | only while the spawner is in its `COOLDOWN` state (Jade 26.3 behavior) |
| `minecraft:item_storage` | any block | `ViewGroup` inventory list | chests, barrels, double chests as one view, ... |
| `jade:loot_table` | any block entity | loot table id + seed | gamemaster permission only (debug) |

## Entity data providers

| Wire id | Target | Sends | Notes |
| --- | --- | --- | --- |
| `minecraft:animal_owner` | any entity | owner name `Component` | online owners directly, offline via profile cache |
| `minecraft:potion_effects` | living entities | visible effects + add/update times | times tracked via Bukkit's `EntityPotionEffectEvent` |
| `minecraft:mob_growth` | baby animals, tadpoles | ticks until grown (VarInt) | tadpole age read reflectively |
| `minecraft:mob_breeding` | animals, villagers, allays | breeding cooldown (VarInt), `-1` = in love | allays report their duplication cooldown |
| `minecraft:villager_restock` | villagers | restocks today + ticks until next restock | new in Jade 26.3; only for villagers that used a trade |
| `minecraft:next_entity_drop` | chicken, armadillo, sniffer | raw `NextEggIn` / `NextScuteIn` / `NextSniffIn` ints | capped at two in-game days |
| `minecraft:zombie_villager` | zombie villagers | conversion time (VarInt) | |
| `minecraft:pet_armor` | mobs | body armor `ItemStack` | wolves, horses, happy ghasts, ... |
| `minecraft:waxed` | copper golems | `Unit` marker when waxed | priority 10 |
| `minecraft:entity_health` | living entities | absorption amount (float) | current/max health and armor are synced by vanilla |
| `minecraft:item_storage` | any entity | `ViewGroup` inventory list | horses, llamas, minecarts with chests, armor stands, the viewer's ender chest; priority 1000 |
| `jade:loot_table` | any entity | loot table id | gamemaster permission only (debug) |

Priorities not mentioned in the table are Jade's default 0; `entity_health`
runs first (-8000) and `item_storage` last (1000), matching Jade.

## Server providers in Jade that have nothing to serve in vanilla

`minecraft:fluid_storage`, `minecraft:energy_storage` and the progress
providers are registered for modded capabilities (Fabric transfer API). Vanilla
Minecraft has no fluid/energy containers with that API, so JadeBridge has
nothing to implement — with Jade's own server on vanilla, these providers
produce no data either.

## Client-only features (no server involvement, by design)

Health and armor icons, mob spawner contents, campfires, enchantment power,
crop progress, note blocks, TNT stability, armor stands, item frames,
paintings, player heads, horse stats, villager professions, breaking progress,
harvest tool (it uses the shearable block list from the handshake), signs,
fluid/energy views. All of this is either synced through vanilla entity/block
data or computed on the client; Jade's server half doesn't provide it and
neither does JadeBridge.

## Not implementable on Paper

- The `jade:max_position_deviation` game rule: the `minecraft:game_rule`
  registry is frozen before plugins load. The deviation exists as
  `max-position-deviation` in JadeBridge's config instead (same default, 21).
- Reading private vanilla members Jade reaches through its fabric
  classtweaker: done via reflection instead (`Nms`, `ShearableBlocks`), failing
  soft if a member moves.
