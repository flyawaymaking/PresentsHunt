package com.flyaway.presentshunt.commands;

import com.flyaway.presentshunt.PresentsHunt;
import com.flyaway.presentshunt.managers.PresentsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PresentsHuntCommand implements CommandExecutor, TabExecutor {
    private final PresentsHunt plugin;
    private final PresentsManager presentsManager;

    public PresentsHuntCommand(PresentsHunt plugin) {
        this.plugin = plugin;
        this.presentsManager = plugin.getPresentsManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("presentshunt.admin")) {
            plugin.sendMessage(sender, plugin.getMessage("needPermission"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                handleGiveCommand(sender);
                break;
            case "stats":
                handleStatsCommand(sender);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "version":
                handleVersionCommand(sender);
                break;
            case "locate":
                handleLocateCommand(sender, args);
                break;
            case "cleanup":
                handleCleanupCommand(sender, args);
                break;
            case "resetplayer":
                handleResetPlayerCommand(sender, args);
                break;
            case "resetall":
                handleResetAllCommand(sender);
                break;
            default:
                sendUsage(sender);
                break;
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        plugin.sendMessage(sender, plugin.getMessage("commands.usage"));
    }

    private void handleGiveCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, plugin.getMessage("commands.playersOnly"));
            return;
        }

        ItemStack presentSkull = presentsManager.getPresentSkull();
        player.getInventory().addItem(presentSkull);
        plugin.sendMessage(sender, plugin.getMessage("commands.presentGiven")
                .replace("%mode%", plugin.getPresentsMode().name()));
    }

    private void handleStatsCommand(CommandSender sender) {
        plugin.runAsync(() -> {
            int players = presentsManager.getPlayersWithData();
            int completedPlayers = presentsManager.getCompletedPlayers();
            int percentage = players == 0 ? 0 : completedPlayers * 100 / players;

            plugin.sendMessage(sender, plugin.getMessage("commands.stats")
                    .replace("%completed%", String.valueOf(completedPlayers))
                    .replace("%percentage%", String.valueOf(percentage))
                    .replace("%total%", String.valueOf(players)));
        });
    }

    private void handleReloadCommand(CommandSender sender) {
        plugin.reload();
        plugin.sendMessage(sender, plugin.getMessage("commands.reload"));
    }

    private void handleVersionCommand(CommandSender sender) {
        String currentVersion = plugin.getPluginMeta().getVersion();
        plugin.sendMessage(sender, plugin.getMessage("commands.version")
                .replace("%version%", currentVersion));
    }

    private void handleLocateCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, plugin.getMessage("playersOnly"));
            return;
        }

        int radius = 100;
        if (args.length > 1) {
            try {
                radius = Integer.parseInt(args[1]);
                if (radius <= 0) {
                    plugin.sendMessage(player, plugin.getMessage("commands.invalidRadius"));
                    return;
                }
            } catch (NumberFormatException e) {
                plugin.sendMessage(player, plugin.getMessage("commands.invalidRadius"));
                return;
            }
        }
        final int finalRadius = radius;

        plugin.runTask(() -> {
            List<Location> presents = findPresentsInRadius(player.getLocation(), finalRadius);

            if (presents.isEmpty()) {
                plugin.sendMessage(player, plugin.getMessage("commands.locate.noneFound")
                        .replace("%radius%", String.valueOf(finalRadius)));
                return;
            }

            StringBuilder locations = new StringBuilder();

            int index = 1;
            for (Location loc : presents) {
                String locationText = plugin.getMessage("commands.locate.location")
                        .replace("%index%", String.valueOf(index))
                        .replace("%x%", String.valueOf(loc.getBlockX()))
                        .replace("%y%", String.valueOf(loc.getBlockY()))
                        .replace("%z%", String.valueOf(loc.getBlockZ()));

                String tpCommand = "/tp " + player.getName() + " " +
                        loc.getBlockX() + " " +
                        loc.getBlockY() + " " +
                        loc.getBlockZ();

                String hoverText = plugin.getMessage("commands.locate.hoverText");

                hoverText = hoverText.replace("'", "''");

                locations.append("<hover:show_text:'")
                        .append(hoverText)
                        .append("'><click:run_command:'")
                        .append(tpCommand)
                        .append("'>")
                        .append(locationText)
                        .append("</click></hover>");

                if (index < presents.size()) {
                    locations.append("; ");
                }

                index++;
            }

            String message = plugin.getMessage("commands.locate.found")
                    .replace("%count%", String.valueOf(presents.size()))
                    .replace("%radius%", String.valueOf(finalRadius))
                    .replace("%locations%", locations.toString());

            plugin.sendMessage(player, message);
        });
    }

    private void handleCleanupCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, plugin.getMessage("commands.playersOnly"));
            return;
        }

        int radius = 100;
        if (args.length > 1) {
            try {
                radius = Integer.parseInt(args[1]);
                if (radius <= 0) {
                    plugin.sendMessage(player, plugin.getMessage("commands.invalidRadius"));
                    return;
                }
            } catch (NumberFormatException e) {
                plugin.sendMessage(player, plugin.getMessage("commands.invalidRadius"));
                return;
            }
        }
        final int finalRadius = radius;

        plugin.runTask(() -> {
            List<Location> presents = findPresentsInRadius(player.getLocation(), finalRadius);
            int removed = 0;

            for (Location loc : presents) {
                Block block = loc.getBlock();
                if (presentsManager.removePresentData(block)) {
                    block.setType(Material.AIR);
                    removed++;
                }
            }

            plugin.sendMessage(player, plugin.getMessage("commands.cleanup-success")
                    .replace("%removed%", String.valueOf(removed))
                    .replace("%radius%", String.valueOf(finalRadius)));
        });
    }

    private void handleResetPlayerCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(sender, plugin.getMessage("commands.resetPlayer.usage"));
            return;
        }

        String username = args[1];
        plugin.runAsync(() -> {
            boolean success = presentsManager.resetPlayerData(username);
            if (success) {
                plugin.sendMessage(sender, plugin.getMessage("commands.resetPlayer.success")
                        .replace("%player%", username));
            } else {
                plugin.sendMessage(sender, plugin.getMessage("commands.resetPlayer.failed")
                        .replace("%player%", username));
            }
        });
    }

    private void handleResetAllCommand(CommandSender sender) {
        plugin.runAsync(() -> {
            boolean success = presentsManager.resetAllPlayersData();
            if (success) {
                plugin.sendMessage(sender, plugin.getMessage("commands.resetAll.success"));
            } else {
                plugin.sendMessage(sender, plugin.getMessage("commands.resetAll.failed"));
            }
        });
    }

    private List<Location> findPresentsInRadius(Location center, int radius) {
        List<Location> presents = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location loc = center.clone().add(x, y, z);
                    Block block = loc.getBlock();

                    if (presentsManager.isPresentBlock(block)) {
                        presents.add(loc);
                    }
                }
            }
        }

        return presents;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("presentshunt.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Stream.of("give", "stats", "reload", "version", "locate", "cleanup", "resetplayer", "resetall")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("locate") || args[0].equalsIgnoreCase("cleanup")) {
                return Arrays.asList("50", "100", "150", "200");
            } else if (args[0].equalsIgnoreCase("resetplayer")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
