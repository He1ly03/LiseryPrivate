package He1ly03.wand;

import He1ly03.LiseryPrivate;
import He1ly03.chunk.ChunkData;
import He1ly03.utils.ColorUtils;
import He1ly03.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages the region editor wand item
 */
public class WandManager {
    
    private final LiseryPrivate plugin;
    private final NamespacedKey wandKey;
    
    // Task for showing chunk boundaries
    private BukkitTask outlineTask;
    
    // Track players holding wand
    private final Set<UUID> playersHoldingWand = new HashSet<>();
    
    public WandManager(LiseryPrivate plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "region_wand");
    }
    
    /**
     * Start the outline particle task
     */
    public void startOutlineTask() {
        if (outlineTask != null) {
            outlineTask.cancel();
        }
        
        outlineTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isHoldingWand(player)) {
                        playersHoldingWand.add(player.getUniqueId());
                        showChunkBoundaries(player, player.getLocation().getChunk());
                    } else {
                        playersHoldingWand.remove(player.getUniqueId());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // Every 5 ticks (0.25 seconds) for smooth particles
    }
    
    /**
     * Stop the outline task
     */
    public void stopOutlineTask() {
        if (outlineTask != null) {
            outlineTask.cancel();
            outlineTask = null;
        }
        playersHoldingWand.clear();
    }
    
    /**
     * Check if player is holding wand in either hand
     */
    public boolean isHoldingWand(Player player) {
        return isWand(player.getInventory().getItemInMainHand()) || 
               isWand(player.getInventory().getItemInOffHand());
    }
    
    /**
     * Check if player is in the holding wand set
     */
    public boolean isPlayerHoldingWand(UUID playerUUID) {
        return playersHoldingWand.contains(playerUUID);
    }
    
    /**
     * Create a new wand item
     */
    public ItemStack createWand() {
        Material material = plugin.getConfigManager().getWandMaterial();
        ItemStack wand = new ItemStack(material);
        
        ItemMeta meta = wand.getItemMeta();
        if (meta == null) return wand;
        
        // Set display name without italic
        String name = plugin.getConfigManager().getWandName();
        Component nameComponent = ColorUtils.colorize(name)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(nameComponent);
        
        // Set lore without italic
        List<String> loreStrings = plugin.getConfigManager().getWandLore();
        List<Component> lore = new ArrayList<>();
        for (String line : loreStrings) {
            lore.add(ColorUtils.colorize(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        
        // Hide attributes
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        
        // Mark as wand using PDC
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        
        wand.setItemMeta(meta);
        return wand;
    }
    
    /**
     * Check if an item is the wand
     */
    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }
    
    /**
     * Give wand to player
     */
    public void giveWand(Player player) {
        ItemStack wand = createWand();
        
        // Check if player already has a wand
        for (ItemStack item : player.getInventory().getContents()) {
            if (isWand(item)) {
                return; // Already has wand
            }
        }
        
        player.getInventory().addItem(wand);
    }
    
    /**
     * Show chunk boundaries with particles
     */
    public void showChunkBoundaries(Player player, Chunk chunk) {
        int chunkX = chunk.getX() << 4;
        int chunkZ = chunk.getZ() << 4;
        int playerY = player.getLocation().getBlockY();
        
        Particle particle = plugin.getConfigManager().getOutlineParticle();
        String colorString = plugin.getConfigManager().getOutlineParticleColor();
        int[] rgb = ColorUtils.parseRGB(colorString);
        
        Particle.DustOptions dustOptions = new Particle.DustOptions(
                Color.fromRGB(rgb[0], rgb[1], rgb[2]), 1.0f
        );
        
        // Draw outline at multiple Y levels around player
        for (int y = playerY - 1; y <= playerY + 3; y += 2) {
            // Draw particles with spacing for performance
            for (int i = 0; i <= 16; i += 2) {
                // North edge
                spawnParticle(player, particle, chunkX + i, y, chunkZ, dustOptions);
                // South edge
                spawnParticle(player, particle, chunkX + i, y, chunkZ + 16, dustOptions);
                // West edge
                spawnParticle(player, particle, chunkX, y, chunkZ + i, dustOptions);
                // East edge
                spawnParticle(player, particle, chunkX + 16, y, chunkZ + i, dustOptions);
            }
        }
        
        // Corner pillars for better visibility
        for (int y = playerY - 2; y <= playerY + 4; y++) {
            spawnParticle(player, particle, chunkX, y, chunkZ, dustOptions);
            spawnParticle(player, particle, chunkX + 16, y, chunkZ, dustOptions);
            spawnParticle(player, particle, chunkX, y, chunkZ + 16, dustOptions);
            spawnParticle(player, particle, chunkX + 16, y, chunkZ + 16, dustOptions);
        }
    }
    
    private void spawnParticle(Player player, Particle particle, double x, double y, double z, 
                               Particle.DustOptions dustOptions) {
        Location loc = new Location(player.getWorld(), x, y, z);
        
        if (particle == Particle.DUST || particle == Particle.DUST_COLOR_TRANSITION) {
            player.spawnParticle(particle, loc, 1, 0, 0, 0, 0, dustOptions);
        } else {
            player.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Play teleport effects
     */
    public void playTeleportEffects(Player player, Location location) {
        // Particle
        Particle particle = plugin.getConfigManager().getTeleportParticle();
        if (particle != null) {
            String colorString = plugin.getConfigManager().getTeleportParticleColor();
            int[] rgb = ColorUtils.parseRGB(colorString);
            
            if (particle == Particle.DUST || particle == Particle.DUST_COLOR_TRANSITION) {
                Particle.DustOptions dustOptions = new Particle.DustOptions(
                        Color.fromRGB(rgb[0], rgb[1], rgb[2]), 1.5f
                );
                player.getWorld().spawnParticle(particle, location.clone().add(0, 1, 0), 
                        30, 0.5, 1, 0.5, 0, dustOptions);
            } else {
                player.getWorld().spawnParticle(particle, location.clone().add(0, 1, 0), 
                        30, 0.5, 1, 0.5, 0);
            }
        }
        
        // Sound
        Sound sound = plugin.getConfigManager().getTeleportSound();
        if (sound != null) {
            player.playSound(location, sound, 
                    plugin.getConfigManager().getTeleportSoundVolume(),
                    plugin.getConfigManager().getTeleportSoundPitch());
        }
    }
    
    /**
     * Handle wand right-click
     */
    public void handleWandUse(Player player, ChunkData chunkData) {
        if (chunkData == null) {
            // Free chunk - open confirm menu for claiming
            plugin.getMenuManager().openMenu(player, "confirm", 0, null, "[CHUNK_PRIVATE]");
        } else if (chunkData.isOwner(player.getUniqueId())) {
            // Own chunk - open private menu
            plugin.getMenuManager().openMenu(player, "private", 0, chunkData, null);
        } else {
            // Someone else's chunk
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("not-owner"));
        }
    }
}
