package He1ly03.integration;

import He1ly03.LiseryPrivate;
import He1ly03.chunk.ChunkData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PlaceholderAPI expansion for LiseryPrivate
 */
public class PlaceholderAPIExpansion extends PlaceholderExpansion {
    
    private final LiseryPrivate plugin;
    
    public PlaceholderAPIExpansion(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "liseryprivate";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "He1ly03";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        
        // Get chunk at player location
        ChunkData currentChunk = plugin.getChunkManager().getChunkAt(player.getLocation());
        
        return switch (params.toLowerCase()) {
            // Player-related
            case "chunks_owned" -> String.valueOf(plugin.getChunkManager().getPlayerChunkCount(player.getUniqueId()));
            case "chunks_limit" -> String.valueOf(plugin.getIntegrationManager().getPlayerChunkLimit(player));
            case "chunks_remaining" -> {
                int owned = plugin.getChunkManager().getPlayerChunkCount(player.getUniqueId());
                int limit = plugin.getIntegrationManager().getPlayerChunkLimit(player);
                yield String.valueOf(limit - owned);
            }
            
            // Current chunk related (full names)
            case "chunk_name" -> currentChunk != null ? currentChunk.getName() : "";
            case "chunk_owner" -> currentChunk != null ? currentChunk.getOwnerName() : "";
            case "chunk_is_claimed" -> currentChunk != null ? "true" : "false";
            case "chunk_is_owner" -> currentChunk != null && currentChunk.isOwner(player.getUniqueId()) ? "true" : "false";
            case "chunk_is_trusted" -> currentChunk != null && currentChunk.isTrusted(player.getUniqueId()) ? "true" : "false";
            case "chunk_can_interact" -> currentChunk != null && currentChunk.canInteract(player.getUniqueId()) ? "true" : "false";
            case "chunk_is_for_sale" -> currentChunk != null && currentChunk.isForSale() ? "true" : "false";
            case "chunk_sale_price" -> currentChunk != null && currentChunk.isForSale() ? 
                    String.valueOf(currentChunk.getSalePrice()) : "";
            
            // Short names (new format requested)
            case "chunk" -> currentChunk != null ? currentChunk.getName() : "";
            case "owner" -> currentChunk != null ? currentChunk.getOwnerName() : "";
            case "index" -> getChunkIndex(currentChunk, player);
            case "x" -> String.valueOf(player.getLocation().getBlockX() >> 4);
            case "z" -> String.valueOf(player.getLocation().getBlockZ() >> 4);
            case "world" -> player.getWorld().getName();
            case "price" -> {
                if (currentChunk != null && currentChunk.isForSale()) {
                    yield String.valueOf(currentChunk.getSalePrice());
                }
                yield String.valueOf(plugin.getConfigManager().getChunkPrivatePrice());
            }
            case "pvp" -> {
                if (currentChunk == null) yield "";
                boolean pvp = currentChunk.getSettings().isPvpAllowed();
                yield pvp ? plugin.getConfigManager().getPvPFormatTrue() : plugin.getConfigManager().getPvPFormatFalse();
            }
            case "is_claimed" -> currentChunk != null ? "true" : "false";
            case "is_owner" -> currentChunk != null && currentChunk.isOwner(player.getUniqueId()) ? "true" : "false";
            case "is_trusted" -> currentChunk != null && currentChunk.isTrusted(player.getUniqueId()) ? "true" : "false";
            
            // PvP status (full name)
            case "chunk_pvp" -> {
                if (currentChunk == null) yield "";
                boolean pvp = currentChunk.getSettings().isPvpAllowed();
                yield pvp ? plugin.getConfigManager().getPvPFormatTrue() : plugin.getConfigManager().getPvPFormatFalse();
            }
            
            // Settings
            case "chunk_build" -> formatSetting(currentChunk, "build");
            case "chunk_destroy" -> formatSetting(currentChunk, "destroy");
            case "chunk_use" -> formatSetting(currentChunk, "use");
            case "chunk_switch" -> formatSetting(currentChunk, "switch");
            case "chunk_mobs" -> formatSetting(currentChunk, "mobs");
            case "chunk_fire" -> formatSetting(currentChunk, "fire");
            case "chunk_explosion" -> formatSetting(currentChunk, "explosion");
            
            // Prices (legacy)
            case "price_private" -> String.valueOf(plugin.getConfigManager().getChunkPrivatePrice());
            case "refund" -> String.valueOf(plugin.getConfigManager().getChunkUnprivateRefund());
            
            // Coordinates (full names)
            case "chunk_x" -> String.valueOf(player.getLocation().getBlockX() >> 4);
            case "chunk_z" -> String.valueOf(player.getLocation().getBlockZ() >> 4);
            
            // Trust list
            case "chunk_trust" -> formatTrustList(currentChunk);
            
            default -> null;
        };
    }
    
    private String formatTrustList(ChunkData chunk) {
        if (chunk == null || chunk.getTrustedPlayers().isEmpty()) {
            return plugin.getConfigManager().getTrustFormatNoPlayers();
        }
        
        StringBuilder result = new StringBuilder();
        String format = plugin.getConfigManager().getTrustFormatPlayer();
        
        for (String playerName : chunk.getTrustedPlayers().values()) {
            result.append(format.replace("%player%", playerName));
        }
        
        // Remove trailing comma and space if present
        String output = result.toString();
        if (output.endsWith(", ")) {
            output = output.substring(0, output.length() - 2);
        }
        
        return output;
    }
    
    private String formatSetting(ChunkData chunk, String setting) {
        if (chunk == null) return "";
        boolean value = chunk.getSettings().getSetting(setting);
        return value ? plugin.getConfigManager().getSettingsFormatTrue() : 
                plugin.getConfigManager().getSettingsFormatFalse();
    }
    
    /**
     * Get chunk index (position in owner's chunk list)
     */
    private String getChunkIndex(ChunkData chunk, Player player) {
        if (chunk == null) return "";
        
        List<ChunkData> playerChunks = plugin.getChunkManager().getPlayerChunks(chunk.getOwnerUUID());
        for (int i = 0; i < playerChunks.size(); i++) {
            if (playerChunks.get(i).getId() == chunk.getId()) {
                return String.valueOf(i + 1); // 1-based index
            }
        }
        return "";
    }
}

