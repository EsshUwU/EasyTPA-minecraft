package com.simpletpa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class SimpleTpaPlugin extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
    private static final long REQUEST_TIMEOUT_TICKS = 20L * 10L;

    private final Map<UUID, TeleportRequest> requestsByTarget = new HashMap<>();

    @Override
    public void onEnable() {
        registerCommand("tpa");
        registerCommand("tpaccept");
        registerCommand("tpdeny");
        registerCommand("simpletpa");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (TeleportRequest request : requestsByTarget.values()) {
            request.expiryTask.cancel();
        }
        requestsByTarget.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);

        return switch (commandName) {
            case "tpa" -> handleTpa(sender, args);
            case "tpaccept" -> handleTpAccept(sender);
            case "tpdeny" -> handleTpDeny(sender);
            case "simpletpa" -> handleHelp(sender);
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("tpa") || args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(sender)) {
                continue;
            }

            String name = player.getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                completions.add(name);
            }
        }
        return completions;
    }

    private boolean handleTpa(CommandSender sender, String[] args) {
        if (!(sender instanceof Player requester)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /tpa.");
            return true;
        }

        if (args.length != 1) {
            requester.sendMessage(ChatColor.RED + "Usage: /tpa <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            requester.sendMessage(ChatColor.RED + "That player is not online.");
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage(ChatColor.RED + "You cannot send a teleport request to yourself.");
            return true;
        }

        clearRequest(target.getUniqueId());

        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(this, () -> expireRequest(target.getUniqueId()), REQUEST_TIMEOUT_TICKS);
        requestsByTarget.put(target.getUniqueId(), new TeleportRequest(requester.getUniqueId(), target.getUniqueId(), expiryTask));

        requester.sendMessage(ChatColor.GREEN + "Teleport request sent to " + target.getName() + ".");
        target.sendMessage(ChatColor.YELLOW + requester.getName() + " wants to teleport to you.");
        target.sendMessage(ChatColor.YELLOW + "You have 10 seconds to use " + ChatColor.GREEN + "/tpaccept" + ChatColor.YELLOW + " or " + ChatColor.RED + "/tpdeny" + ChatColor.YELLOW + ".");
        return true;
    }

    private boolean handleTpAccept(CommandSender sender) {
        if (!(sender instanceof Player target)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /tpaccept.");
            return true;
        }

        TeleportRequest request = clearRequest(target.getUniqueId());
        if (request == null) {
            target.sendMessage(ChatColor.RED + "You do not have a pending teleport request.");
            return true;
        }

        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requester == null) {
            target.sendMessage(ChatColor.RED + "The player who requested teleport is no longer online.");
            return true;
        }

        requester.teleport(target.getLocation());
        requester.sendMessage(ChatColor.GREEN + "Teleport request accepted. Teleporting to " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + "Accepted teleport request from " + requester.getName() + ".");
        return true;
    }

    private boolean handleTpDeny(CommandSender sender) {
        if (!(sender instanceof Player target)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /tpdeny.");
            return true;
        }

        TeleportRequest request = clearRequest(target.getUniqueId());
        if (request == null) {
            target.sendMessage(ChatColor.RED + "You do not have a pending teleport request.");
            return true;
        }

        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requester != null) {
            requester.sendMessage(ChatColor.RED + target.getName() + " denied your teleport request.");
        }
        target.sendMessage(ChatColor.YELLOW + "Teleport request denied.");
        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "SimpleTPA commands:");
        sender.sendMessage(ChatColor.YELLOW + "/tpa <player>" + ChatColor.WHITE + " - Request to teleport to a player.");
        sender.sendMessage(ChatColor.YELLOW + "/tpaccept" + ChatColor.WHITE + " - Accept your pending teleport request.");
        sender.sendMessage(ChatColor.YELLOW + "/tpdeny" + ChatColor.WHITE + " - Deny your pending teleport request.");
        sender.sendMessage(ChatColor.YELLOW + "Requests expire after 10 seconds.");
        return true;
    }

    private void expireRequest(UUID targetId) {
        TeleportRequest request = requestsByTarget.remove(targetId);
        if (request == null) {
            return;
        }

        Player target = Bukkit.getPlayer(request.targetId);
        if (target != null) {
            target.sendMessage(ChatColor.RED + "Teleport request expired.");
        }

        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requester != null) {
            requester.sendMessage(ChatColor.RED + "Your teleport request expired.");
        }
    }

    private TeleportRequest clearRequest(UUID targetId) {
        TeleportRequest request = requestsByTarget.remove(targetId);
        if (request != null) {
            request.expiryTask.cancel();
        }
        return request;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        clearRequest(playerId);

        requestsByTarget.entrySet().removeIf(entry -> {
            TeleportRequest request = entry.getValue();
            if (!request.requesterId.equals(playerId)) {
                return false;
            }
            request.expiryTask.cancel();
            Player target = Bukkit.getPlayer(request.targetId);
            if (target != null) {
                target.sendMessage(ChatColor.RED + "Teleport request expired because the requester left.");
            }
            return true;
        });
    }

    private record TeleportRequest(UUID requesterId, UUID targetId, BukkitTask expiryTask) {
    }

    private void registerCommand(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Missing command in plugin.yml: " + name);
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }
}
