# Mod Whitelist

**A Fabric server-side mod that enforces a strict modpack whitelist — players running unauthorized mods are automatically kicked.**

---

## What it does

Minecraft doesn't natively expose a client's installed mod list to the server. Mod Whitelist fixes that gap: every time a player joins, their client (which must also have this mod installed) sends the server a list of every mod it has loaded. The server checks that list against an admin-defined whitelist and disconnects the player if anything doesn't match.

This is useful for server owners who want to guarantee every connected player is running the *exact* approved modpack — no extra performance mods that break balance, no client-side exploits.

---

## Installation

### Server
1. Place `mod-whitelist-1.0.0.jar` in your server's `mods/` folder.
2. Start the server once. This auto-generates the config file at `config/mod-whitelist.json`.
3. Edit that file to list every mod ID your modpack uses (see [Configuration](#configuration) below).
4. Run `/modwhitelist reload` in-game/console, or restart the server, to apply changes.

### Client
Add `mod-whitelist-1.0.0.jar` to the `mods/` folder of every client instance, alongside the rest of the modpack. If you distribute your modpack via Modrinth, CurseForge, or packwiz, simply include this mod in the pack definition so it installs automatically for everyone.

---

## How the whitelist check works

This is a **strict whitelist** — anything not explicitly listed is treated as unauthorized, even if it's harmless. There are two distinct scenarios that result in a kick:

### 1. Player has the mod, but an unauthorized mod is detected
On join, the client sends its full mod list. The server compares every entry against `allowed_mods` in the config. If even one mod isn't on the list, the player is disconnected immediately with a customizable kick message listing exactly which mod(s) caused the failure.

### 2. Player doesn't have Mod Whitelist installed at all
This covers vanilla clients, or anyone who removed the mod itself to dodge detection. Since they can never send the required packet, the server waits **10 seconds** after they join and then kicks them with a message explaining that the mod is required.

> **Important:** Every time you add a new mod to your pack, you must also add its mod ID to `allowed_mods` and reload the config — otherwise every player will be kicked the next time they connect with the updated pack.

---

## Configuration

File location: `config/mod-whitelist.json` (auto-created on first launch with sensible defaults).

```json
{
  "enabled": true,
  "kick_message": "§cUnauthorized mods detected:\n§f%mods%\n§7Please install the official modpack.",
  "allowed_mods": [
    "minecraft",
    "fabricloader",
    "fabric-api",
    "java",
    "modwhitelist",
    "sodium",
    "iris",
    "lithium",
    "ferrite-core",
    "modmenu"
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `enabled` | boolean | Turns the whole check on/off without removing the mod. |
| `kick_message` | string | Message shown on kick. Use `%mods%` as a placeholder — it's replaced with the comma-separated list of offending mods. Supports `\n` for line breaks. |
| `allowed_mods` | array of strings | The exact mod IDs permitted to connect. **Not** display names — use the `id` field from each mod's `fabric.mod.json`, or check in-game via Mod Menu. |

### Color codes in `kick_message`

Standard Minecraft formatting codes work: `§a` green, `§c` red, `§e` yellow, `§f` white, `§7` gray, etc.

---

## Commands

| Command | Effect | Required permission |
|---|---|---|
| `/modwhitelist reload` | Reloads `mod-whitelist.json` from disk without restarting the server. | Admin (OP level 3 equivalent) |
| `/modwhitelist status` | Shows whether the check is enabled, how many mods are whitelisted, and the current timeout duration. | Admin (OP level 3 equivalent) |

---

## Server console logging

The mod logs every join attempt so admins can audit activity at a glance:

```
[ModWhitelist] Ready — 12 allowed mods, timeout=10s
[ModWhitelist] ✔ Steve — 12 mods verified
[ModWhitelist] ✘ Hacker kicked — unauthorized mods: wurst, xaeroplus
[ModWhitelist] ✘ VanillaPlayer kicked — ModWhitelist missing (timeout)
```

---

## Tips & best practices

- **Keep the whitelist in sync with your modpack.** Every update to the pack (adding/removing a mod) requires a matching update to `allowed_mods`.
- **Always include the mod's own ID** (`modwhitelist`) in the whitelist — otherwise the server will flag its own required mod as unauthorized.
- **Test changes on a small scale first.** After editing the config, reload it and have a test account rejoin before rolling out to your whole community, to avoid mass-kicking legitimate players over a typo.
- **Performance/QoL mods** (Sodium, Lithium, Iris, ModMenu, etc.) are common to whitelist alongside your gameplay mods, since they don't affect game balance but greatly improve client experience.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Everyone gets kicked for "ModWhitelist missing" even though they have the mod | Server and client mod versions mismatch, or the server isn't running Fabric API at the required version — the packet never registers correctly. |
| A legitimate player gets kicked for an "unauthorized mod" | The mod ID in `allowed_mods` doesn't exactly match the ID in that mod's `fabric.mod.json`. IDs are case-sensitive. |
| Config changes don't apply | Run `/modwhitelist reload`, or confirm you edited the correct file under `config/mod-whitelist.json` on the server (not the client). |
