package com.flyaway.presentshunt.managers;

import com.flyaway.presentshunt.PresentsHunt;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {
    private final PresentsHunt plugin;
    private final File leaderboardFile;
    private final Map<UUID, PlayerRecord> leaderboard;

    public LeaderboardManager(PresentsHunt plugin) {
        this.plugin = plugin;
        this.leaderboardFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        this.leaderboard = new LinkedHashMap<>();
        loadLeaderboard();
    }

    private void loadLeaderboard() {
        if (!leaderboardFile.exists()) {
            saveLeaderboard();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(leaderboardFile);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String playerName = config.getString(key + ".name", "");
                int found = config.getInt(key + ".found", 0);
                long lastUpdated = config.getLong(key + ".updated", 0);
                
                if (!playerName.isEmpty() && found > 0) {
                    leaderboard.put(uuid, new PlayerRecord(playerName, found, lastUpdated));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveLeaderboard() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<UUID, PlayerRecord> entry : leaderboard.entrySet()) {
            String uuidStr = entry.getKey().toString();
            PlayerRecord record = entry.getValue();
            
            config.set(uuidStr + ".name", record.playerName);
            config.set(uuidStr + ".found", record.found);
            config.set(uuidStr + ".updated", record.lastUpdated);
        }

        try {
            config.save(leaderboardFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить лидерборд: " + e.getMessage());
        }
    }

    public void updatePlayer(Player player, int foundCount) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        long currentTime = System.currentTimeMillis();
        
        PlayerRecord record = leaderboard.get(uuid);
        if (record == null || foundCount > record.found) {
            leaderboard.put(uuid, new PlayerRecord(playerName, foundCount, currentTime));
            maintainLeaderboard();
            saveLeaderboard();
        } else if (!playerName.equals(record.playerName)) {
            record.playerName = playerName;
            record.lastUpdated = currentTime;
            saveLeaderboard();
        }
    }

    private void maintainLeaderboard() {
        int maxLimit = plugin.getMaxLeaderBoardPlayers();
        if (leaderboard.size() <= maxLimit) {
            return;
        }

        List<Map.Entry<UUID, PlayerRecord>> sorted = new ArrayList<>(leaderboard.entrySet());
        sorted.sort((entry1, entry2) -> {
            PlayerRecord r1 = entry1.getValue();
            PlayerRecord r2 = entry2.getValue();
            
            if (r2.found != r1.found) {
                return Integer.compare(r2.found, r1.found);
            }
            return Long.compare(r1.lastUpdated, r2.lastUpdated);
        });

        leaderboard.clear();
        int count = Math.min(maxLimit, sorted.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<UUID, PlayerRecord> entry = sorted.get(i);
            leaderboard.put(entry.getKey(), entry.getValue());
        }
    }

    public Map<UUID, PlayerRecord> getTopPlayers(int limit) {
        int maxLimit = plugin.getMaxLeaderBoardPlayers();
        if (limit < 0) {
            limit = maxLimit;
        }
        limit = Math.min(limit, maxLimit);

        return leaderboard.entrySet().stream()
                .sorted((entry1, entry2) -> {
                    PlayerRecord r1 = entry1.getValue();
                    PlayerRecord r2 = entry2.getValue();
                    
                    if (r2.found != r1.found) {
                        return Integer.compare(r2.found, r1.found);
                    }
                    return Long.compare(r1.lastUpdated, r2.lastUpdated);
                })
                .limit(limit)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
    }

    public void removePlayer(UUID uuid) {
        leaderboard.remove(uuid);
        saveLeaderboard();
    }

    public void clearLeaderboard() {
        leaderboard.clear();
        saveLeaderboard();
    }

    public static class PlayerRecord {
        public String playerName;
        public int found;
        public long lastUpdated;

        public PlayerRecord(String playerName, int found, long lastUpdated) {
            this.playerName = playerName;
            this.found = found;
            this.lastUpdated = lastUpdated;
        }
    }
}
