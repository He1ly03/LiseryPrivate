package He1ly03.wand;

import He1ly03.LiseryPrivate;
import He1ly03.chunk.ChunkData;
import He1ly03.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for wand interactions
 */
public class WandListener implements Listener {
    
    private final LiseryPrivate plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    public WandListener(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (!plugin.getWandManager().isWand(item)) {
            return;
        }
        
        event.setCancelled(true);
        
        // Cooldown check
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse != null && now - lastUse < 500) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now);
        
        // Check permission
        if (!player.hasPermission("liseryprivate.wand")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        // Get chunk data at player location
        ChunkData chunkData = plugin.getChunkManager().getChunkAt(player.getLocation());
        
        // Handle based on chunk status
        if (chunkData == null) {
            // Free chunk - open confirm for claiming
            plugin.getMenuManager().openMenu(player, "confirm", 0, null, "[CHUNK_PRIVATE]");
        } else if (chunkData.isOwner(player.getUniqueId())) {
            // Own chunk - open settings menu
            plugin.getMenuManager().openMenu(player, "private", 0, chunkData, null);
        } else {
            // Someone else's chunk
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("not-owner"));
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
        plugin.getHologramManager().removePlayer(event.getPlayer().getUniqueId());
    }
    
    // Prevent crafting with wand
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (!plugin.getConfigManager().isWandProtected()) return;
        
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (plugin.getWandManager().isWand(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onCraftItem(CraftItemEvent event) {
        if (!plugin.getConfigManager().isWandProtected()) return;
        
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (plugin.getWandManager().isWand(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
