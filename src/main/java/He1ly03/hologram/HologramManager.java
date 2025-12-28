package He1ly03.hologram;

import He1ly03.LiseryPrivate;
import He1ly03.chunk.ChunkData;
import He1ly03.utils.ColorUtils;
import He1ly03.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages holograms for chunks
 * Holograms follow player's Y position + hologram-height
 */
public class HologramManager {
    
    private final LiseryPrivate plugin;
    
    // Active holograms: chunk key -> hologram entity
    private final Map<String, TextDisplay> chunkHolograms = new HashMap<>();
    
    // Players viewing holograms
    private final Map<UUID, Set<String>> playerViewingChunks = new HashMap<>();
    
    // Update task
    private BukkitTask updateTask;
    
    public HologramManager(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start the hologram update task
     */
    public void start() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        
        // Update every 5 ticks (0.25 seconds) for smooth following
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllHolograms();
            }
        }.runTaskTimer(plugin, 20L, 5L);
    }
    
    /**
     * Stop the hologram manager
     */
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        
        // Remove all holograms
        for (TextDisplay display : chunkHolograms.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        chunkHolograms.clear();
        playerViewingChunks.clear();
    }
    
    /**
     * Update holograms for all online players
     */
    private void updateAllHolograms() {
        Set<String> activeChunks = new HashSet<>();
        
        // Track the highest player Y in each chunk for hologram positioning
        Map<String, Double> chunkPlayerY = new HashMap<>();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            Chunk playerChunk = player.getLocation().getChunk();
            String chunkKey = getChunkKey(playerChunk);
            
            // Track player Y position for their chunk
            double playerY = player.getLocation().getY();
            chunkPlayerY.merge(chunkKey, playerY, Math::max);
            
            // Check if player should see holograms
            boolean holdingWand = plugin.getWandManager().isHoldingWand(player);
            
            // Get chunks around player (3x3 area)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Chunk chunk = player.getWorld().getChunkAt(
                            playerChunk.getX() + dx,
                            playerChunk.getZ() + dz
                    );
                    
                    String nearbyChunkKey = getChunkKey(chunk);
                    ChunkData chunkData = plugin.getChunkManager().getChunkAt(chunk);
                    
                    String hologramType = getHologramType(chunkData);
                    
                    // Check if hologram should be shown
                    if (!plugin.getConfigManager().isHologramEnabled(hologramType)) {
                        continue;
                    }
                    
                    boolean requiresWand = plugin.getConfigManager().isHologramRegionEditorOnly(hologramType);
                    if (requiresWand && !holdingWand) {
                        hideHologramFromPlayer(player, nearbyChunkKey);
                        continue;
                    }
                    
                    activeChunks.add(nearbyChunkKey);
                    
                    // Create or get hologram
                    TextDisplay hologram = chunkHolograms.get(nearbyChunkKey);
                    if (hologram == null || !hologram.isValid()) {
                        hologram = createHologram(chunk, chunkData, player);
                        if (hologram != null) {
                            chunkHolograms.put(nearbyChunkKey, hologram);
                        }
                    }
                    
                    // Show to player
                    if (hologram != null && hologram.isValid()) {
                        showHologramToPlayer(player, nearbyChunkKey, hologram);
                    }
                }
            }
        }
        
        // Update hologram positions based on player Y in each chunk
        double hologramHeight = plugin.getConfigManager().getHologramHeight();
        
        for (Map.Entry<String, TextDisplay> entry : chunkHolograms.entrySet()) {
            String chunkKey = entry.getKey();
            TextDisplay hologram = entry.getValue();
            
            if (hologram == null || !hologram.isValid()) continue;
            
            // Get the highest player Y in this chunk, or use current hologram Y
            Double playerY = chunkPlayerY.get(chunkKey);
            
            if (playerY != null) {
                // Player is in this chunk - position hologram above their head
                Location currentLoc = hologram.getLocation();
                double newY = playerY + hologramHeight;
                
                // Only teleport if Y changed significantly
                if (Math.abs(currentLoc.getY() - newY) > 0.1) {
                    hologram.teleport(new Location(
                            currentLoc.getWorld(),
                            currentLoc.getX(),
                            newY,
                            currentLoc.getZ()
                    ));
                }
            }
        }
        
        // Remove holograms that are no longer needed
        Iterator<Map.Entry<String, TextDisplay>> iterator = chunkHolograms.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TextDisplay> entry = iterator.next();
            if (!activeChunks.contains(entry.getKey())) {
                if (entry.getValue().isValid()) {
                    entry.getValue().remove();
                }
                iterator.remove();
            }
        }
    }
    
    private String getHologramType(ChunkData chunkData) {
        if (chunkData == null) {
            return "free-chunk";
        } else if (chunkData.isForSale()) {
            return "for-sale-chunk";
        } else {
            return "claimed-chunk";
        }
    }
    
    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
    
    /**
     * Create hologram at chunk center
     * Y position based on viewer's position + hologram-height
     */
    private TextDisplay createHologram(Chunk chunk, ChunkData chunkData, Player viewer) {
        String hologramType = getHologramType(chunkData);
        
        List<String> lines = plugin.getConfigManager().getHologramLines(hologramType);
        if (lines.isEmpty()) return null;
        
        // Calculate chunk center X and Z
        int centerX = (chunk.getX() << 4) + 8;
        int centerZ = (chunk.getZ() << 4) + 8;
        
        // Y position = player Y + hologram-height
        double hologramHeight = plugin.getConfigManager().getHologramHeight();
        double y = viewer.getLocation().getY() + hologramHeight;
        
        Location hologramLoc = new Location(chunk.getWorld(), centerX + 0.5, y, centerZ + 0.5);
        
        // Create placeholders
        Map<String, String> placeholders = createPlaceholders(viewer, chunkData);
        
        // Combine all lines
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = MessageUtils.replacePlaceholders(lines.get(i), placeholders);
            combined.append(line);
            if (i < lines.size() - 1) {
                combined.append("\n");
            }
        }
        
        TextDisplay display = (TextDisplay) hologramLoc.getWorld().spawnEntity(hologramLoc, EntityType.TEXT_DISPLAY);
        
        // Set text
        display.text(ColorUtils.colorize(combined.toString()));
        
        // Set billboard
        String billboardStr = plugin.getConfigManager().getTextEntityBillboard();
        Display.Billboard billboard = switch (billboardStr.toLowerCase()) {
            case "fixed" -> Display.Billboard.FIXED;
            case "vertical" -> Display.Billboard.VERTICAL;
            case "horizontal" -> Display.Billboard.HORIZONTAL;
            default -> Display.Billboard.CENTER;
        };
        display.setBillboard(billboard);
        
        // Set background color
        String bgColor = plugin.getConfigManager().getTextEntityBackgroundColor();
        int argb = ColorUtils.parseARGB(bgColor);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(argb));
        
        // Set scale
        float scale = (float) plugin.getConfigManager().getTextEntityScale();
        display.setTransformation(new org.bukkit.util.Transformation(
                new org.joml.Vector3f(0, 0, 0),
                new org.joml.AxisAngle4f(0, 0, 0, 1),
                new org.joml.Vector3f(scale, scale, scale),
                new org.joml.AxisAngle4f(0, 0, 0, 1)
        ));
        
        // Make it invisible by default
        display.setVisibleByDefault(false);
        
        // Set view range
        display.setViewRange(0.5f);
        
        // Prevent despawning
        display.setPersistent(false);
        
        return display;
    }
    
    private void showHologramToPlayer(Player player, String chunkKey, TextDisplay hologram) {
        Set<String> viewing = playerViewingChunks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        
        if (!viewing.contains(chunkKey)) {
            player.showEntity(plugin, hologram);
            viewing.add(chunkKey);
        }
    }
    
    private void hideHologramFromPlayer(Player player, String chunkKey) {
        Set<String> viewing = playerViewingChunks.get(player.getUniqueId());
        if (viewing != null && viewing.contains(chunkKey)) {
            TextDisplay hologram = chunkHolograms.get(chunkKey);
            if (hologram != null && hologram.isValid()) {
                player.hideEntity(plugin, hologram);
            }
            viewing.remove(chunkKey);
        }
    }
    
    /**
     * Remove player's viewing data when they leave
     */
    public void removePlayer(UUID playerUUID) {
        playerViewingChunks.remove(playerUUID);
    }
    
    /**
     * Create placeholders for hologram
     */
    private Map<String, String> createPlaceholders(Player player, ChunkData chunk) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", player.getName());
        placeholders.put("%chunkprivate_price%", MessageUtils.formatMoney(plugin.getConfigManager().getChunkPrivatePrice()));
        placeholders.put("%price%", MessageUtils.formatMoney(plugin.getConfigManager().getChunkPrivatePrice()));
        
        if (chunk != null) {
            placeholders.put("%owner%", chunk.getOwnerName());
            placeholders.put("%chunk%", chunk.getName());
            
            boolean pvp = chunk.getSettings().isPvpAllowed();
            placeholders.put("%pvp%", pvp ? plugin.getConfigManager().getPvPFormatTrue() : 
                    plugin.getConfigManager().getPvPFormatFalse());
            
            if (chunk.isForSale()) {
                placeholders.put("%price%", MessageUtils.formatMoney(chunk.getSalePrice()));
            }
        } else {
            placeholders.put("%owner%", "Никто");
            placeholders.put("%chunk%", "Свободен");
            placeholders.put("%pvp%", "");
        }
        
        return placeholders;
    }
}
