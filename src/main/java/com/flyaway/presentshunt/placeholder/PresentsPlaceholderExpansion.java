package com.flyaway.presentshunt.placeholder;

import com.flyaway.presentshunt.PresentsHunt;
import com.flyaway.presentshunt.managers.LeaderboardManager;
import com.flyaway.presentshunt.managers.PresentsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class PresentsPlaceholderExpansion extends PlaceholderExpansion {
    private final PresentsHunt plugin;
    private final PresentsManager presentsManager;
    private final LeaderboardManager leaderboardManager;

    public PresentsPlaceholderExpansion(PresentsHunt plugin) {
        this.plugin = plugin;
        this.presentsManager = plugin.getPresentsManager();
        this.leaderboardManager = presentsManager.getLeaderboardManager();
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getPluginMeta().getAuthors().getFirst();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "presentshunt";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (identifier.equalsIgnoreCase("status")) {
            return getPlayerStatus(player);
        }

        if (identifier.matches("top_(\\d+)_status")) {
            try {
                int position = Integer.parseInt(identifier.split("_")[1]);
                return getTopPlayerStatus(position);
            } catch (NumberFormatException e) {
                return "";
            }
        }

        if (player == null) {
            return "";
        }

        switch (identifier.toLowerCase()) {
            case "found":
                return getFoundPresents(player);
            case "total":
                return String.valueOf(plugin.getConfig().getInt("totalPresents", 0));
            case "mode":
                String presentsMode = plugin.getPresentsMode().name();
                presentsMode = presentsMode.substring(0, 1).toUpperCase() + presentsMode.substring(1).toLowerCase();
                return presentsMode;
            case "completed":
                return String.valueOf(presentsManager.getCompletedPlayers());
            case "players":
                return String.valueOf(presentsManager.getPlayersWithData());
            case "position":
                return getLeaderboardPosition(player);
            default:
                return null;
        }
    }

    private String getFoundPresents(OfflinePlayer player) {
        return String.valueOf(presentsManager.getFoundPresents(player.getUniqueId()));
    }

    private String getPlayerStatus(OfflinePlayer player) {
        int found = presentsManager.getFoundPresents(player.getUniqueId());
        int total = plugin.getConfig().getInt("totalPresents", 0);

        return getStatusText(found, total);
    }

    private String getTopPlayerStatus(int position) {
        Map<UUID, LeaderboardManager.PlayerRecord> topPlayers = leaderboardManager.getTopPlayers(10);

        if (position <= 0 || position > topPlayers.size()) {
            return plugin.getPlaceholderMessage("top.noPlayer");
        }

        int i = 1;
        for (LeaderboardManager.PlayerRecord record : topPlayers.values()) {
            if (i == position) {
                int total = plugin.getConfig().getInt("totalPresents", 0);
                String status = getStatusText(record.found, total);
                return plugin.getPlaceholderMessage("top.status")
                        .replace("%position%", String.valueOf(position))
                        .replace("%username%", record.playerName)
                        .replace("%status%", status);
            }
            i++;
        }

        return plugin.getPlaceholderMessage("top.noPlayer");
    }

    private String getStatusText(int found, int total) {
        if (total > 0 && found >= total) {
            return plugin.getPlaceholderMessage("status.completed");
        } else if (found > 0) {
            return plugin.getPlaceholderMessage("status.inProgress")
                    .replace("%found%", String.valueOf(found))
                    .replace("%total%", String.valueOf(total));
        }
        return plugin.getPlaceholderMessage("status.notStarted");
    }

    private String getLeaderboardPosition(OfflinePlayer player) {
        Map<UUID, LeaderboardManager.PlayerRecord> topPlayers = leaderboardManager.getTopPlayers(-1);
        int position = 1;

        for (LeaderboardManager.PlayerRecord record : topPlayers.values()) {
            UUID recordUuid = null;
            for (Map.Entry<UUID, LeaderboardManager.PlayerRecord> entry : topPlayers.entrySet()) {
                if (entry.getValue() == record) {
                    recordUuid = entry.getKey();
                    break;
                }
            }

            if (recordUuid != null && recordUuid.equals(player.getUniqueId())) {
                return String.valueOf(position);
            }
            position++;
        }

        return "-";
    }
}
