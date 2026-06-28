# Take a Seat

Sit, lounge, and relax. Press a key and Take a Seat plays a **context-aware sitting animation** on your
player — it figures out *what* you're near or holding and picks a fitting pose, then keeps your model
in frame with a smooth camera. Other players see you sitting too (when they also have the mod).

Take a Seat is a from-scratch rebuild of the discontinued **Sitting Plus** for Minecraft **1.21.10**, running
on the [Player Animation Library](https://modrinth.com/mod/player-animation-library) backend and ported to
NeoForge.

> These are *cosmetic* animations. You don't actually mount the block — your model poses in place, and
> moving stands you back up.

---

## Requirements

| Mod | Version |
|---|---|
| Minecraft | 1.21.10 |
| NeoForge | 21.10.64+ |
| Player Animation Library | bundled in this port |

---

## How to use

- Press **`X`** (rebindable in *Options → Controls → Take a Seat*) to sit.
- **Press it again while seated** to cycle to the next variation of the current pose.
- **Move** (WASD / jump / sneak / sprint) to stand back up.
- **Right-click a stair with an empty hand** to snap onto it and sit (can be disabled in the config).
- You **can't sit while airborne, swimming/underwater, or riding** something.

### How the pose is chosen

When you press the key, Take a Seat checks these in order and uses the **first** match:

| Priority | Trigger | Pose (animation variants) | Notes |
|---:|---|---|---|
| 1 | **Looking at a campfire** (within ~2 blocks) | `campfiresit` | Looping idle |
| 2 | **Looking at a furnace** (furnace / blast furnace / smoker) | `furnacesit` | Looping idle |
| 3 | **Holding a sword** | `swordsit`, `swordsit2` | 2 variants |
| 4 | **Holding an axe** | `sittingaxe` | 1 variant |
| 5 | **Holding a shovel** | `sittingshovel` | 1 variant |
| 6 | **Holding a fishing rod** | `fishing` | Looping cast |
| 7 | **Standing on stairs** | `chairsitting` 1–4 | "Sit in a chair" — 4 variants |
| 8 | **Standing on a fence** | `fencesitting` 1–2 | Perched higher, 2 variants |
| 9 | **Standing on a bed** | `bedlyingdown` 1–3 | Lie down, 3 variants |
| 10 | **Standing on a bottom slab** | `chairsitting` 1–4 | Treated as a chair |
| 11 | **Standing on a carpet** | ground poses | Treated as floor |
| 12 | **Standing on a `#takeaseat:sittable` block** | `chairsitting` 1–4 | Datapack-defined chairs (incl. modded) |
| 13 | **Anywhere else (ground)** | `kneesitting`, `buttsit`, `buttsit2`, `kneeleaning` | Default, 4 variants |

Because the list is checked top-down, **what you're looking at or holding wins over what you're standing
on** — e.g. holding a sword while standing on stairs gives you the sword pose, not the chair pose. Set
your hand empty and look away from campfires/furnaces to get the stair/fence/bed/ground poses.

That's **20 hand-made animations** in total, cycled per category by tapping the key again.

---

## Camera

- **Auto third person** — sitting flips you to third person so you can see the pose (toggle:
  `enableThirdPersonOnSit`). Standing restores your previous view.
- **Focus on the model** — the camera eases downward while seated so the sitting body stays centered
  (toggle: `enableSitCameraFocus`, distance: `cameraFocusOffset`).
- **Your F5 is respected** — once you're seated you can change view (e.g. front view) freely; cycling
  through poses will **not** snap the camera back.

---

## Configuration

The old in-game config screen is not wired into this NeoForge port yet, so edit the JSON file directly.

Edit `config/TakeASeatConfig.json` (created on first launch):

| Option | Default | Description |
|---|---|---|
| `enableClickToSit` | `true` | Right-click stairs with an empty hand to sit on them. |
| `enableThirdPersonOnSit` | `true` | Switch to third person while sitting. |
| `enableSitCameraFocus` | `true` | Smoothly lower the camera to focus on the seated model. |
| `cameraFocusOffset` | `0.55` | How far (blocks) to lower the camera while sitting. |
| `onlyLowerCameraInFirstPerson` | `false` | Only apply the camera lower in first person. |
| `enableAfkSit` | `false` | Auto-sit after being idle. *(Off by default — was non-functional in the original Sitting Plus.)* |
| `afkSitDelaySeconds` | `60` | Idle seconds before AFK auto-sit triggers. |

---

## Multiplayer

Take a Seat syncs poses to other players: when you sit, your client tells the server, which relays it so
everyone running Take a Seat mirrors your animation. The mod should be installed on the server and on each
client that wants to *see* the animations. Players who were **already sitting when you join** are synced
to you automatically (no need for them to re-sit).

## For datapack / modpack makers

Make any block sittable (it'll use the chair pose) by adding it to the **`#takeaseat:sittable`** block tag —
create `data/<your_pack>/tags/block/sittable.json`:

```json
{ "replace": false, "values": ["mymod:fancy_chair", "minecraft:lectern"] }
```

Bottom slabs and carpets are recognized automatically, so you only need the tag for extra/modded blocks.

---

## Building

```sh
./gradlew build
```

The mod sources target **Java 21** (Mojang ships Java 21 to players in 1.21.10); the Gradle build will
auto-provision a JDK 21 toolchain via the foojay resolver if one isn't installed. The output jar lands
in `build/libs/`. To test in-game: `./gradlew runClient`.

---

## Credits

- Original concept & animations: **Sitting Plus** by saltywater (ported here with permission).
- Animation backend: **Player Animation Library** by ZigyTheBird (MIT).
