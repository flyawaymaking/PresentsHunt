package com.flyaway.presentshunt;

import com.flyaway.presentshunt.commands.PresentsHuntCommand;
import com.flyaway.presentshunt.placeholder.PresentsPlaceholderExpansion;
import com.flyaway.presentshunt.listeners.PlayerInteractListener;
import com.flyaway.presentshunt.managers.PresentsManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PresentsHunt extends JavaPlugin {
    public PresentsManager presentsManager;
    private PresentMode presentMode;
    public final MiniMessage miniMessage = MiniMessage.miniMessage();
    private FileConfiguration config;

    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        presentsManager = new PresentsManager(this);
        presentMode = PresentMode.valueOf(config.getString("presentsMode", "CHRISTMAS"));
        getLogger().info("PresentsHunt, загружен в режиме " + presentMode.name() + "!");

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            (new PresentsPlaceholderExpansion(this)).register();
            getLogger().info("Подключен PlaceholderAPI!");
        }

        getCommand("presentshunt").setExecutor(new PresentsHuntCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
    }

    public void reload() {
        reloadConfig();
        config = getConfig();
        presentMode = PresentMode.valueOf(config.getString("presentsMode", "CHRISTMAS"));
    }

    public void sendMessage(CommandSender sender, String message) {
        String prefix = config.getString("prefix", "<red><bold>🎁</bold> <gray>» <green>");
        if (message.isEmpty()) return;
        sender.sendMessage(miniMessage.deserialize(prefix + message));
    }

    public void broadcastMessage(String message) {
        String prefix = config.getString("prefix", "<red><bold>🎁</bold> <gray>» <green>");
        if (message.isEmpty()) return;
        Bukkit.broadcast(miniMessage.deserialize(prefix + message));
    }

    public void playSound(Player player, String soundName) {
        if (player == null || soundName == null || soundName.isEmpty()) {
            return;
        }

        NamespacedKey soundKey = NamespacedKey.fromString(soundName);
        if (soundKey == null) {
            getLogger().warning("Invalid sound key: " + soundName);
            return;
        }

        Sound sound = Registry.SOUNDS.get(soundKey);
        if (sound == null) {
            getLogger().warning("Sound not found in registry: " + soundName);
            return;
        }

        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    public void spawnParticle(Player player, String particleName, org.bukkit.Location location, int count) {
        if (particleName == null || particleName.isEmpty() || player == null || location == null) {
            return;
        }

        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            player.spawnParticle(particle, location, count);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Particle not found: " + particleName);
            player.spawnParticle(Particle.HEART, location, count);
        }
    }

    public String getMessage(String key) {
        return config.getString("messages." + key, "<red>not found: messages." + key);
    }

    public String getPlaceholderMessage(String key) {
        return config.getString("placeholders." + key, "<red>not found: placeholders." + key);
    }

    public int getMaxLeaderBoardPlayers() {
        return config.getInt("leaderboard.maxPlayersCount", 100);
    }

    public PresentMode getPresentsMode() {
        return presentMode;
    }

    public PresentsManager getPresentsManager() {
        return presentsManager;
    }

    public void runAsync(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(this, scheduledTask -> task.run());
    }

    public void runTask(Runnable task) {
        Bukkit.getScheduler().runTask(this, task);
    }
}
