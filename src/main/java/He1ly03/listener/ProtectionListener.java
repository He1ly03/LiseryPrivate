package He1ly03.listener;

import He1ly03.LiseryPrivate;
import He1ly03.chunk.ChunkData;
import He1ly03.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

/**
 * Listener for chunk protection events
 */
public class ProtectionListener implements Listener {
    
    private final LiseryPrivate plugin;
    
    public ProtectionListener(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    // ==================== Build Protection ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfigManager().isProtectBuild()) return;
        
        Player player = event.getPlayer();
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getBlock().getLocation());
        
        if (chunk == null) return; // Not claimed
        
        if (canBuild(player, chunk)) return;
        
        event.setCancelled(true);
        sendDenyMessage(player, "protection-deny-build");
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (!plugin.getConfigManager().isProtectBuild()) return;
        
        Player player = event.getPlayer();
        if (player == null) return;
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getEntity().getLocation());
        
        if (chunk == null) return;
        
        if (canBuild(player, chunk)) return;
        
        event.setCancelled(true);
        sendDenyMessage(player, "protection-deny-build");
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!plugin.getConfigManager().isProtectBuild()) return;
        
        Player player = event.getPlayer();
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getBlock().getLocation());
        
        if (chunk == null) return;
        
        if (canBuild(player, chunk)) return;
        
        event.setCancelled(true);
        sendDenyMessage(player, "protection-deny-build");
    }
    
    // ==================== Destroy Protection ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfigManager().isProtectDestroy()) return;
        
        Player player = event.getPlayer();
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getBlock().getLocation());
        
        if (chunk == null) return;
        
        if (canDestroy(player, chunk)) return;
        
        event.setCancelled(true);
        sendDenyMessage(player, "protection-deny-destroy");
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!plugin.getConfigManager().isProtectDestroy()) return;
        
        Entity remover = event.getRemover();
        if (!(remover instanceof Player player)) return;
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getEntity().getLocation());
        
        if (chunk == null) return;
        
        if (canDestroy(player, chunk)) return;
        
        event.setCancelled(true);
        sendDenyMessage(player, "protection-deny-destroy");
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!plugin.getConfigManager().isProtectDestroy()) return;
        
        Player player = event.getPlayer();
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getBlock().getLocation());
        
        if (chunk == null) return;
        
        if (canDestroy(player, chunk)) return;
        
        event.setCancelled(true);
        sendDenyMessage(player, "protection-deny-destroy");
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (!plugin.getConfigManager().isProtectUse()) return;
        
        Player player = event.getPlayer();
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getRightClicked().getLocation());
        
        if (chunk == null) return;
        
        if (canUse(player, chunk)) return;
        
        event.setCancelled(true);
        sendDenyMessage(player, "protection-deny-use");
    }
    
    // ==================== Use Protection ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        Player player = event.getPlayer();
        ChunkData chunk = plugin.getChunkManager().getChunkAt(block.getLocation());
        
        if (chunk == null) return;
        
        Material type = block.getType();
        
        // Check for use-type blocks (containers)
        if (isUseBlock(type)) {
            if (!plugin.getConfigManager().isProtectUse()) return;
            
            if (canUse(player, chunk)) return;
            
            event.setCancelled(true);
            sendDenyMessage(player, "protection-deny-use");
            return;
        }
        
        // Check for switch-type blocks
        if (isSwitchBlock(type)) {
            if (!plugin.getConfigManager().isProtectSwitch()) return;
            
            if (canSwitch(player, chunk)) return;
            
            event.setCancelled(true);
            sendDenyMessage(player, "protection-deny-switch");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.getConfigManager().isProtectUse()) return;
        
        Entity entity = event.getRightClicked();
        
        // Check for interactive entities (villagers, item frames, etc.)
        if (!(entity instanceof ItemFrame) && !(entity instanceof Villager) && 
            !(entity instanceof ArmorStand)) {
            return;
        }
        
        Player player = event.getPlayer();
        ChunkData chunk = plugin.getChunkManager().getChunkAt(entity.getLocation());
        
        if (chunk == null) return;
        
        if (canUse(player, chunk)) return;
        
        event.setCancelled(true);
        sendDenyMessage(player, "protection-deny-use");
    }
    
    // ==================== Mob Protection ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();
        
        // Get the actual player damager
        Player player = null;
        if (damager instanceof Player) {
            player = (Player) damager;
        } else if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                player = (Player) projectile.getShooter();
            }
        }
        
        if (player == null) return;
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(damaged.getLocation());
        if (chunk == null) return;
        
        // PvP check
        if (damaged instanceof Player) {
            if (!plugin.getConfigManager().isProtectPvP()) return;
            
            if (canPvP(player, chunk)) return;
            
            event.setCancelled(true);
            sendDenyMessage(player, "protection-deny-pvp");
            return;
        }
        
        // Mob protection
        if (damaged instanceof LivingEntity && !(damaged instanceof Monster)) {
            if (!plugin.getConfigManager().isProtectMobs()) return;
            
            if (chunk.canInteract(player.getUniqueId())) return;
            if (chunk.getSettings().isMobsAllowed()) return;
            
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (!plugin.getConfigManager().isProtectMobs()) return;
        
        Entity attacker = event.getAttacker();
        if (!(attacker instanceof Player player)) return;
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getVehicle().getLocation());
        if (chunk == null) return;
        
        if (chunk.canInteract(player.getUniqueId())) return;
        if (chunk.getSettings().isMobsAllowed()) return;
        
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (!plugin.getConfigManager().isProtectMobs()) return;
        
        Entity attacker = event.getAttacker();
        if (!(attacker instanceof Player player)) return;
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getVehicle().getLocation());
        if (chunk == null) return;
        
        if (chunk.canInteract(player.getUniqueId())) return;
        if (chunk.getSettings().isMobsAllowed()) return;
        
        event.setCancelled(true);
    }
    
    // ==================== Fire Protection ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!plugin.getConfigManager().isProtectFire()) return;
        
        Player player = event.getPlayer();
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getBlock().getLocation());
        
        if (chunk == null) return;
        
        // Natural fire spread
        if (player == null) {
            if (!chunk.getSettings().isFireAllowed()) {
                event.setCancelled(true);
            }
            return;
        }
        
        if (chunk.canInteract(player.getUniqueId())) return;
        if (chunk.getSettings().isFireAllowed()) return;
        
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!plugin.getConfigManager().isProtectFire()) return;
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getBlock().getLocation());
        
        if (chunk == null) return;
        
        if (!chunk.getSettings().isFireAllowed()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireSpread(BlockSpreadEvent event) {
        if (!plugin.getConfigManager().isProtectFire()) return;
        
        if (event.getSource().getType() != Material.FIRE) return;
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(event.getBlock().getLocation());
        
        if (chunk == null) return;
        
        if (!chunk.getSettings().isFireAllowed()) {
            event.setCancelled(true);
        }
    }
    
    // ==================== Explosion Protection ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getConfigManager().isProtectExplosion()) return;
        
        Entity entity = event.getEntity();
        
        // Allow owner/trusted TNT
        if (entity instanceof TNTPrimed tnt) {
            Entity source = tnt.getSource();
            if (source instanceof Player player) {
                // Check each block
                event.blockList().removeIf(block -> {
                    ChunkData chunk = plugin.getChunkManager().getChunkAt(block.getLocation());
                    if (chunk == null) return false;
                    return !chunk.canInteract(player.getUniqueId()) && !chunk.getSettings().isExplosionAllowed();
                });
                return;
            }
        }
        
        // Remove protected blocks from explosion
        event.blockList().removeIf(block -> {
            ChunkData chunk = plugin.getChunkManager().getChunkAt(block.getLocation());
            if (chunk == null) return false;
            return !chunk.getSettings().isExplosionAllowed();
        });
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getConfigManager().isProtectExplosion()) return;
        
        event.blockList().removeIf(block -> {
            ChunkData chunk = plugin.getChunkManager().getChunkAt(block.getLocation());
            if (chunk == null) return false;
            return !chunk.getSettings().isExplosionAllowed();
        });
    }
    
    // ==================== Helper Methods ====================
    
    private boolean canBuild(Player player, ChunkData chunk) {
        if (player.hasPermission("liseryprivate.admin")) return true;
        if (chunk.canInteract(player.getUniqueId())) return true;
        return chunk.getSettings().isBuildAllowed();
    }
    
    private boolean canDestroy(Player player, ChunkData chunk) {
        if (player.hasPermission("liseryprivate.admin")) return true;
        if (chunk.canInteract(player.getUniqueId())) return true;
        return chunk.getSettings().isDestroyAllowed();
    }
    
    private boolean canUse(Player player, ChunkData chunk) {
        if (player.hasPermission("liseryprivate.admin")) return true;
        if (chunk.canInteract(player.getUniqueId())) return true;
        return chunk.getSettings().isUseAllowed();
    }
    
    private boolean canSwitch(Player player, ChunkData chunk) {
        if (player.hasPermission("liseryprivate.admin")) return true;
        if (chunk.canInteract(player.getUniqueId())) return true;
        return chunk.getSettings().isSwitchAllowed();
    }
    
    private boolean canPvP(Player player, ChunkData chunk) {
        // PvP settings don't consider trust - based on chunk settings only
        return chunk.getSettings().isPvpAllowed();
    }
    
    private boolean isUseBlock(Material type) {
        String name = type.name();
        return name.contains("CHEST") || name.contains("FURNACE") || name.contains("BARREL") ||
               name.contains("HOPPER") || name.contains("DISPENSER") || name.contains("DROPPER") ||
               name.contains("BREWING") || name.contains("BEACON") || name.contains("ANVIL") ||
               name.contains("ENCHANTING") || name.contains("GRINDSTONE") || name.contains("LOOM") ||
               name.contains("CARTOGRAPHY") || name.contains("SMITHING") || name.contains("STONECUTTER") ||
               name.contains("SHULKER") || name.contains("LECTERN") || name.contains("CAMPFIRE") ||
               name.contains("COMPOSTER") || name.contains("CAULDRON") || name.contains("BEEHIVE") ||
               name.contains("BEE_NEST") || name.contains("JUKEBOX") || name.contains("DECORATED_POT") ||
               type == Material.CRAFTING_TABLE || type == Material.ENDER_CHEST;
    }
    
    private boolean isSwitchBlock(Material type) {
        String name = type.name();
        return name.contains("BUTTON") || name.contains("LEVER") || name.contains("PRESSURE_PLATE") ||
               name.contains("DOOR") || name.contains("GATE") || name.contains("TRAPDOOR") ||
               name.contains("REPEATER") || name.contains("COMPARATOR") || name.contains("DAYLIGHT") ||
               name.contains("NOTE_BLOCK") || name.contains("TRIPWIRE") || name.contains("BELL");
    }
    
    private void sendDenyMessage(Player player, String messageKey) {
        String message = plugin.getConfigManager().getMessage(messageKey);
        if (message != null && !message.isEmpty()) {
            MessageUtils.sendMessage(player, message);
        }
    }
}

