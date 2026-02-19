package com.flyaway.presentshunt.listeners;

import com.flyaway.presentshunt.managers.PresentsManager;
import com.flyaway.presentshunt.PresentsHunt;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {
    private final PresentsHunt plugin;
    private final PresentsManager presentsManager;

    public PlayerInteractListener(PresentsHunt plugin) {
        this.plugin = plugin;
        this.presentsManager = plugin.getPresentsManager();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (!presentsManager.isPresentItem(item)) {
            return;
        }

        if (presentsManager.markBlockAsPresent(event.getBlockPlaced(), item)) {
            plugin.sendMessage(event.getPlayer(), plugin.getMessage("presentPlaced"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block != null && event.getHand() == EquipmentSlot.HAND && presentsManager.isPresentBlock(block)) {
            if (!player.hasPermission("presentshunt.use")) {
                plugin.sendMessage(player, plugin.getMessage("needPermission"));
                return;
            }

            plugin.runTask(() -> presentsManager.findPresent(player, block.getLocation()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent e) {
        if (!presentsManager.isPresentBlock(e.getBlock())) return;
        if (!e.getPlayer().hasPermission("presentshunt.admin")) {
            e.setCancelled(true);
            plugin.sendMessage(e.getPlayer(), plugin.getMessage("cannotBreak"));
        } else {
            presentsManager.removePresentData(e.getBlock());
        }
    }
}
