package com.flyaway.presentshunt.managers;

import com.flyaway.presentshunt.PresentsHunt;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.TileState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PresentsManager {
    public final NamespacedKey presentKey;
    private final PresentsHunt plugin;
    private final File dataFolder;
    private final LeaderboardManager leaderboardManager;

    public PresentsManager(PresentsHunt plugin) {
        this.plugin = plugin;
        this.presentKey = new NamespacedKey(plugin, "present");
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.leaderboardManager = new LeaderboardManager(plugin);

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    private File getPlayerFile(UUID playerId) {
        return new File(dataFolder, playerId + ".yml");
    }

    private FileConfiguration getPlayerConfig(UUID playerId) {
        File file = getPlayerFile(playerId);
        if (!file.exists()) {
            try {
                file.createNewFile();
                YamlConfiguration config = new YamlConfiguration();
                config.set("foundPresents", new ArrayList<String>());
                config.save(file);
                return config;
            } catch (IOException e) {
                plugin.getLogger().warning("Не удалось создать файл данных для " + playerId + ": " + e.getMessage());
                return new YamlConfiguration();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void savePlayerConfig(Player player, FileConfiguration config) {
        try {
            config.save(getPlayerFile(player.getUniqueId()));
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить данные для " + player.getUniqueId() + ": " + e.getMessage());
        }
    }

    public int getFoundPresents(UUID playerId) {
        FileConfiguration config = getPlayerConfig(playerId);
        return config.getStringList("foundPresents").size();
    }

    public void findPresent(Player player, Location location) {
        int totalPresents = plugin.getConfig().getInt("totalPresents", 0);
        if (totalPresents <= 0) {
            plugin.getLogger().warning("totalPresents не настроен в конфиге!");
            return;
        }

        String locationStr = locationToString(location);
        FileConfiguration config = getPlayerConfig(player.getUniqueId());

        List<String> foundList = config.getStringList("foundPresents");
        int foundCount = foundList.size();

        if (foundList.contains(locationStr)) {
            handleAlreadyFound(player, location, foundCount, totalPresents);
            return;
        }

        if (foundCount >= totalPresents) {
            plugin.sendMessage(player, plugin.getMessage("alreadyCompleted")
                    .replace("%total%", String.valueOf(totalPresents)));
            return;
        }

        foundList.add(locationStr);
        foundCount++;
        config.set("foundPresents", foundList);

        if (foundCount >= totalPresents) {
            handleCompletion(player, foundCount, totalPresents);
        } else {
            handleNewFound(player, location, foundCount, totalPresents);
        }

        savePlayerConfig(player, config);

        leaderboardManager.updatePlayer(player, foundCount);
    }

    private void handleAlreadyFound(Player player, Location location, int foundCount, int totalPresents) {
        plugin.sendMessage(player, plugin.getMessage("alreadyFound")
                .replace("%found%", String.valueOf(foundCount))
                .replace("%total%", String.valueOf(totalPresents)));

        plugin.playSound(player, plugin.getConfig().getString("sounds.alreadyFound", "entity.zombie.ambient"));

        String particle = plugin.getConfig().getString("particles.alreadyFound", "SQUID_INK");
        plugin.spawnParticle(player, particle, location.clone().add(0.5, 0.0, 0.5), 3);
    }

    private void handleNewFound(Player player, Location location, int foundCount, int totalPresents) {
        plugin.sendMessage(player, plugin.getMessage("found")
                .replace("%found%", String.valueOf(foundCount))
                .replace("%total%", String.valueOf(totalPresents)));

        plugin.playSound(player, plugin.getConfig().getString("sounds.found", "block.pumpkin.carve"));

        String particle = plugin.getConfig().getString("particles.found", "SWEEP_ATTACK");
        plugin.spawnParticle(player, particle, location.clone().add(0.5, 0.0, 0.5), 1);

        executeCommands(player, "commands.foundCommands", foundCount, totalPresents);
    }

    private void handleCompletion(Player player, int foundCount, int totalPresents) {
        plugin.sendMessage(player, plugin.getMessage("complete")
                .replace("%total%", String.valueOf(totalPresents)));

        plugin.playSound(player, plugin.getConfig().getString("sounds.complete", "entity.firework_rocket.blast"));

        executeCommands(player, "commands.rewardCommands", foundCount, totalPresents);

        String broadcastMessage = plugin.getMessage("foundAllPresents")
                .replace("%player%", player.getName())
                .replace("%total%", String.valueOf(totalPresents));

        plugin.broadcastMessage(broadcastMessage);
    }

    private void executeCommands(Player player, String configPath, int foundCount, int totalPresents) {
        for (String command : plugin.getConfig().getStringList(configPath)) {
            String processedCommand = command
                    .replace("%player%", player.getName())
                    .replace("%found%", String.valueOf(foundCount))
                    .replace("%total%", String.valueOf(totalPresents));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        }
    }

    public boolean isPresentBlock(Block block) {
        Material type = block.getType();
        if (type != Material.PLAYER_HEAD && type != Material.PLAYER_WALL_HEAD) {
            return false;
        }

        if (!(block.getState() instanceof TileState tileState)) {
            return false;
        }

        PersistentDataContainer blockData = tileState.getPersistentDataContainer();
        String mode = blockData.get(presentKey, PersistentDataType.STRING);

        return mode != null && mode.equals(plugin.getPresentsMode().name());
    }

    public boolean isPresentItem(ItemStack item) {
        if (item.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                PersistentDataContainer itemData = meta.getPersistentDataContainer();
                return itemData.has(presentKey, PersistentDataType.STRING);
            }
        }
        return false;
    }

    public boolean removePresentData(Block block) {
        Material type = block.getType();
        if (type != Material.PLAYER_HEAD && type != Material.PLAYER_WALL_HEAD) {
            return false;
        }

        if (!(block.getState() instanceof TileState tileState)) {
            return false;
        }

        PersistentDataContainer blockData = tileState.getPersistentDataContainer();
        if (blockData.has(presentKey, PersistentDataType.STRING)) {
            blockData.remove(presentKey);
            tileState.update();
            return true;
        }
        return false;
    }

    public boolean replacePresent(Block block, String fromMode) {
        if (!(block.getState() instanceof TileState tileState)) {
            return false;
        }

        PersistentDataContainer data = tileState.getPersistentDataContainer();
        String mode = data.get(presentKey, PersistentDataType.STRING);

        if (mode == null || !mode.equalsIgnoreCase(fromMode)) {
            return false;
        }

        data.set(presentKey, PersistentDataType.STRING, plugin.getPresentsMode().name());
        tileState.update();

        if (block.getState() instanceof Skull skull) {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "Present");
            String texture = plugin.getConfig().getString("heads." + plugin.getPresentsMode().name(), "");
            ProfileProperty property = new ProfileProperty("textures", texture);
            profile.setProperty(property);
            skull.setPlayerProfile(profile);
            skull.update();
        }

        return true;
    }

    public ItemStack getPresentSkull() {
        ItemStack present = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) present.getItemMeta();

        if (skullMeta != null) {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "Present");
            String texture = plugin.getConfig().getString("heads." + plugin.getPresentsMode().name(), "");
            ProfileProperty property = new ProfileProperty("textures", texture);
            profile.setProperty(property);
            skullMeta.setPlayerProfile(profile);

            PersistentDataContainer data = skullMeta.getPersistentDataContainer();
            data.set(presentKey, PersistentDataType.STRING, plugin.getPresentsMode().name());

            skullMeta.displayName(plugin.miniMessage.deserialize(
                    "<gradient:red:gold>" + plugin.getPresentsMode().name() + " Present</gradient>"
            ));

            present.setItemMeta(skullMeta);
        }

        return present;
    }

    public boolean markBlockAsPresent(Block block, ItemStack item) {
        Material type = block.getType();
        if (type != Material.PLAYER_HEAD && type != Material.PLAYER_WALL_HEAD) {
            return false;
        }

        if (!(block.getState() instanceof TileState tileState)) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) {
            return false;
        }

        PersistentDataContainer itemData = skullMeta.getPersistentDataContainer();
        String mode = itemData.get(presentKey, PersistentDataType.STRING);

        if (mode == null) {
            return false;
        }

        PersistentDataContainer blockData = tileState.getPersistentDataContainer();
        blockData.set(presentKey, PersistentDataType.STRING, mode);

        tileState.update();
        return true;
    }

    public int getPlayersWithData() {
        if (!dataFolder.exists()) return 0;

        File[] files = dataFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        return files != null ? files.length : 0;
    }

    public int getCompletedPlayers() {
        if (!dataFolder.exists()) return 0;

        int totalPresents = plugin.getConfig().getInt("totalPresents", 0);
        if (totalPresents <= 0) return 0;

        int count = 0;
        File[] files = dataFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));

        if (files != null) {
            for (File file : files) {
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    int found = config.getStringList("foundPresents").size();
                    if (found >= totalPresents) {
                        count++;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при чтении файла " + file.getName() + ": " + e.getMessage());
                }
            }
        }

        return count;
    }

    public boolean resetPlayerData(String username) {
        Player onlinePlayer = Bukkit.getPlayerExact(username);
        if (onlinePlayer != null) {
            return resetPlayerData(onlinePlayer);
        }
        return false;
    }

    public boolean resetPlayerData(Player player) {
        File file = getPlayerFile(player.getUniqueId());
        if (file.exists()) {
            if (file.delete()) {
                leaderboardManager.removePlayer(player.getUniqueId());
                plugin.getLogger().info("Данные игрока " + player.getName() + " сброшены");
                return true;
            } else {
                plugin.getLogger().warning("Не удалось удалить файл данных для " + player.getName());
                return false;
            }
        }
        return false;
    }

    public boolean resetAllPlayersData() {
        if (!dataFolder.exists()) return false;

        File[] files = dataFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) return true;

        int deleted = 0;
        for (File file : files) {
            if (file.delete()) {
                deleted++;
            } else {
                plugin.getLogger().warning("Не удалось удалить файл: " + file.getName());
            }
        }

        leaderboardManager.clearLeaderboard();

        plugin.getLogger().info("Сброшены данные " + deleted + " игроков из " + files.length);
        return deleted == files.length;
    }

    public String locationToString(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ":" +
                location.getBlockX() + ":" +
                location.getBlockY() + ":" +
                location.getBlockZ();
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
}
