# EasyTPA

EasyTPA is a lightweight server-side teleport request plugin for CraftBukkit, Spigot, Paper, and compatible forks.

It adds a small `/tpa` system for servers that want simple player-to-player teleport requests without extra configuration or heavy features.

## Features

- Request to teleport to another online player.
- Accept or deny incoming teleport requests.
- Requests automatically expire after 10 seconds.
- Clear chat messages for the requester and receiver.
- Tab completion for `/tpa <player>`.
- No configuration required.

## Commands

| Command | Description |
| --- | --- |
| `/tpa <player>` | Request to teleport to another player. |
| `/tpaccept` | Accept your pending teleport request. |
| `/tpdeny` | Deny your pending teleport request. |
| `/easytpa` | Show the plugin help message. |

## How It Works

When a player runs `/tpa <player>`, the target player receives a chat message telling them they have 10 seconds to accept or deny the request.

If the target runs `/tpaccept`, the requester is teleported to them. If they run `/tpdeny`, the request is denied. If they do nothing, the request expires automatically.

## Compatibility

- Compatible with CraftBukkit, Spigot, Paper, and likely compatible forks such as Purpur.
- Tested on Minecraft 1.21.11.
- Expected to work on Minecraft 1.21.x servers.
- Does not support Fabric, Forge, or NeoForge.

## Installation

1. Download `EasyTPA-Spigot.jar`.
2. Stop your server.
3. Put the jar file into your server's `plugins` folder.
4. Start the server.
5. Run `/easytpa` in-game to check the available commands.

## Notes

EasyTPA is intentionally minimal. It does not include cooldowns, permissions, economy support, cross-server teleporting, or configuration files.
