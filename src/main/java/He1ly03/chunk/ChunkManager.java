package He1ly03.chunk;

import He1ly03.LiseryPrivate;
import He1ly03.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all claimed chunks
 */
public class ChunkManager {
    
    private final LiseryPrivate plugin;
    
    // Cache: chunk key -> ChunkData
    private final Map<String, ChunkData> chunkCache;
    
    // Cache: player UUID -> set of owned chunk keys
    private final Map<UUID, Set<String>> playerChunks;
    
    public ChunkManager(LiseryPrivate plugin) {
        this.plugin = plugin;
        this.chunkCache = new ConcurrentHashMap<>();
        this.playerChunks = new ConcurrentHashMap<>();
    }
    
    /**
     * Load all chunks from database into cache
     */
    public void loadChunks() {
        chunkCache.clear();
        playerChunks.clear();
        
        List<ChunkData> chunks = plugin.getDatabaseManager().loadAllChunks();
        
        for (ChunkData chunk : chunks) {
            String key = chunk.getChunkKey();
            chunkCache.put(key, chunk);
            
            // Load trusted players
            Map<UUID, String> trusted = plugin.getDatabaseManager().getTrustedPlayers(chunk.getId());
            chunk.setTrustedPlayers(trusted);
            
            // Add to player cache
            playerChunks.computeIfAbsent(chunk.getOwnerUUID(), k -> ConcurrentHashMap.newKeySet())
                    .add(key);
        }
        
        plugin.getLogger().info("Loaded " + chunks.size() + " chunks from database.");
    }
    
    /**
     * Get chunk data at location
     */
    public ChunkData getChunkAt(Location location) {
        return getChunkAt(location.getWorld().getName(), 
                location.getBlockX() >> 4, 
                location.getBlockZ() >> 4);
    }
    
    /**
     * Get chunk data at chunk coordinates
     */
    public ChunkData getChunkAt(String world, int chunkX, int chunkZ) {
        String key = LocationUtils.getChunkKey(world, chunkX, chunkZ);
        return chunkCache.get(key);
    }
    
    /**
     * Get chunk data at Bukkit Chunk
     */
    public ChunkData getChunkAt(Chunk chunk) {
        return getChunkAt(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }
    
    /**
     * Check if a chunk is claimed
     */
    public boolean isClaimed(Location location) {
        return getChunkAt(location) != null;
    }
    
    /**
     * Check if a chunk is claimed
     */
    public boolean isClaimed(String world, int chunkX, int chunkZ) {
        return getChunkAt(world, chunkX, chunkZ) != null;
    }
    
    /**
     * Claim a chunk for a player
     */
    public ClaimResult claimChunk(Player player, String customName) {
        Location location = player.getLocation();
        String world = location.getWorld().getName();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        
        // Check if world is disabled
        if (plugin.getConfigManager().isWorldDisabled(world)) {
            return ClaimResult.WORLD_DISABLED;
        }
        
        // Check if already claimed
        if (isClaimed(world, chunkX, chunkZ)) {
            return ClaimResult.ALREADY_CLAIMED;
        }
        
        // Check player limit (operators have unlimited chunks)
        int currentCount = getPlayerChunkCount(player.getUniqueId());
        int limit = plugin.getIntegrationManager().getPlayerChunkLimit(player);
        // Check if limit is not Integer.MAX_VALUE (unlimited) before comparing
        if (limit != Integer.MAX_VALUE && currentCount >= limit) {
            return ClaimResult.LIMIT_REACHED;
        }
        
        // Check economy
        double price = plugin.getConfigManager().getChunkPrivatePrice();
        if (!plugin.getIntegrationManager().hasBalance(player, price)) {
            return ClaimResult.NOT_ENOUGH_MONEY;
        }
        
        // Check distance from other players' chunks
        int minDistance = plugin.getConfigManager().getMinDistance();
        if (minDistance > 0 && !checkDistanceFromOthers(player.getUniqueId(), world, chunkX, chunkZ, minDistance)) {
            return ClaimResult.TOO_CLOSE;
        }
        
        // Generate name if not provided
        String name;
        if (customName == null || customName.isEmpty()) {
            int nextNum = plugin.getDatabaseManager().getNextChunkNumber(player.getUniqueId(), player.getName());
            name = player.getName() + "_" + nextNum;
        } else {
            // Check name length
            if (customName.length() > plugin.getConfigManager().getMaxNameLength()) {
                return ClaimResult.NAME_TOO_LONG;
            }
            // Check if name already exists
            if (plugin.getDatabaseManager().chunkNameExists(player.getUniqueId(), customName)) {
                return ClaimResult.NAME_EXISTS;
            }
            name = customName;
        }
        
        // Withdraw money
        if (!plugin.getIntegrationManager().withdrawBalance(player, price)) {
            return ClaimResult.NOT_ENOUGH_MONEY;
        }
        
        // Create chunk data
        ChunkData chunkData = new ChunkData(world, chunkX, chunkZ, name, 
                player.getUniqueId(), player.getName());
        
        // Create WorldGuard region
        String regionName = plugin.getWorldGuardIntegration().createRegion(player, chunkData);
        if (regionName == null) {
            // Refund if WorldGuard failed
            plugin.getIntegrationManager().depositBalance(player, price);
            return ClaimResult.WORLDGUARD_ERROR;
        }
        chunkData.setWorldGuardRegion(regionName);
        
        // Save to database
        int id = plugin.getDatabaseManager().saveChunk(chunkData);
        if (id == -1) {
            // Rollback WorldGuard
            plugin.getWorldGuardIntegration().deleteRegion(world, regionName);
            plugin.getIntegrationManager().depositBalance(player, price);
            return ClaimResult.DATABASE_ERROR;
        }
        chunkData.setId(id);
        
        // Add to cache
        String key = chunkData.getChunkKey();
        chunkCache.put(key, chunkData);
        playerChunks.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(key);
        
        // Handle region merging
        handleRegionMerging(chunkData);
        
        return ClaimResult.SUCCESS;
    }
    
    /**
     * Unclaim a chunk
     */
    public UnclaimResult unclaimChunk(Player player, ChunkData chunkData) {
        // Check ownership
        if (!chunkData.isOwner(player.getUniqueId()) && 
            !player.hasPermission("liseryprivate.admin")) {
            return UnclaimResult.NOT_OWNER;
        }
        
        // Refund money
        double refund = plugin.getConfigManager().getChunkUnprivateRefund();
        plugin.getIntegrationManager().depositBalance(player, refund);
        
        // Delete WorldGuard region
        plugin.getWorldGuardIntegration().deleteRegion(chunkData.getWorld(), chunkData.getWorldGuardRegion());
        
        // Handle region unmerging before deletion
        handleRegionUnmerging(chunkData);
        
        // Remove from database
        plugin.getDatabaseManager().deleteChunk(chunkData.getId());
        
        // Remove from cache
        String key = chunkData.getChunkKey();
        chunkCache.remove(key);
        
        Set<String> playerChunkSet = playerChunks.get(chunkData.getOwnerUUID());
        if (playerChunkSet != null) {
            playerChunkSet.remove(key);
        }
        
        return UnclaimResult.SUCCESS;
    }
    
    /**
     * Force unclaim a chunk (admin)
     */
    public void forceUnclaimChunk(ChunkData chunkData) {
        // Delete WorldGuard region
        plugin.getWorldGuardIntegration().deleteRegion(chunkData.getWorld(), chunkData.getWorldGuardRegion());
        
        // Handle region unmerging
        handleRegionUnmerging(chunkData);
        
        // Remove from database
        plugin.getDatabaseManager().deleteChunk(chunkData.getId());
        
        // Remove from cache
        String key = chunkData.getChunkKey();
        chunkCache.remove(key);
        
        Set<String> playerChunkSet = playerChunks.get(chunkData.getOwnerUUID());
        if (playerChunkSet != null) {
            playerChunkSet.remove(key);
        }
    }
    
    /**
     * Get all chunks owned by a player
     */
    public List<ChunkData> getPlayerChunks(UUID playerUUID) {
        Set<String> keys = playerChunks.get(playerUUID);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        
        return keys.stream()
                .map(chunkCache::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Get player chunk count
     */
    public int getPlayerChunkCount(UUID playerUUID) {
        Set<String> keys = playerChunks.get(playerUUID);
        return keys != null ? keys.size() : 0;
    }
    
    /**
     * Get all chunks for sale
     */
    public List<ChunkData> getChunksForSale() {
        return chunkCache.values().stream()
                .filter(ChunkData::isForSale)
                .collect(Collectors.toList());
    }
    
    /**
     * Check distance from other players' chunks
     */
    private boolean checkDistanceFromOthers(UUID playerUUID, String worldName, int chunkX, int chunkZ, int minDistance) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return true;
        
        // Check surrounding chunks
        for (int dx = -minDistance; dx <= minDistance; dx++) {
            for (int dz = -minDistance; dz <= minDistance; dz++) {
                if (dx == 0 && dz == 0) continue;
                
                ChunkData nearby = getChunkAt(worldName, chunkX + dx, chunkZ + dz);
                if (nearby != null && !nearby.isOwner(playerUUID)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Handle region merging when a new chunk is claimed
     */
    private void handleRegionMerging(ChunkData newChunk) {
        List<ChunkData> adjacentChunks = getAdjacentChunks(newChunk);
        
        // Filter to only same-owner chunks
        List<ChunkData> ownedAdjacent = adjacentChunks.stream()
                .filter(c -> c.isOwner(newChunk.getOwnerUUID()))
                .collect(Collectors.toList());
        
        if (!ownedAdjacent.isEmpty()) {
            plugin.getWorldGuardIntegration().mergeRegions(newChunk, ownedAdjacent);
        }
    }
    
    /**
     * Handle region unmerging when a chunk is unclaimed
     */
    private void handleRegionUnmerging(ChunkData removedChunk) {
        List<ChunkData> adjacentChunks = getAdjacentChunks(removedChunk);
        
        // Filter to only same-owner chunks
        List<ChunkData> ownedAdjacent = adjacentChunks.stream()
                .filter(c -> c.isOwner(removedChunk.getOwnerUUID()))
                .collect(Collectors.toList());
        
        if (!ownedAdjacent.isEmpty()) {
            plugin.getWorldGuardIntegration().unmergeRegions(removedChunk, ownedAdjacent);
        }
    }
    
    /**
     * Get adjacent chunks
     */
    private List<ChunkData> getAdjacentChunks(ChunkData chunk) {
        List<ChunkData> adjacent = new ArrayList<>();
        
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        
        for (int[] offset : offsets) {
            ChunkData adj = getChunkAt(chunk.getWorld(), 
                    chunk.getChunkX() + offset[0], 
                    chunk.getChunkZ() + offset[1]);
            if (adj != null) {
                adjacent.add(adj);
            }
        }
        
        return adjacent;
    }
    
    /**
     * Rename a chunk
     */
    public boolean renameChunk(ChunkData chunk, String newName) {
        // Check if name already exists for this player
        if (plugin.getDatabaseManager().chunkNameExists(chunk.getOwnerUUID(), newName)) {
            return false;
        }
        
        chunk.setName(newName);
        plugin.getDatabaseManager().updateChunk(chunk);
        return true;
    }
    
    /**
     * Update chunk settings
     */
    public void updateChunkSettings(ChunkData chunk) {
        plugin.getDatabaseManager().updateChunk(chunk);
    }
    
    /**
     * Add trusted player to chunk
     */
    public boolean addTrustedPlayer(ChunkData chunk, UUID playerUUID, String playerName) {
        if (chunk.isTrusted(playerUUID) || chunk.isOwner(playerUUID)) {
            return false;
        }
        
        if (plugin.getDatabaseManager().addTrustedPlayer(chunk.getId(), playerUUID, playerName)) {
            chunk.addTrusted(playerUUID, playerName);
            
            // Update WorldGuard region
            plugin.getWorldGuardIntegration().addMember(chunk.getWorld(), chunk.getWorldGuardRegion(), playerUUID);
            
            return true;
        }
        return false;
    }
    
    /**
     * Remove trusted player from chunk
     */
    public boolean removeTrustedPlayer(ChunkData chunk, UUID playerUUID) {
        if (!chunk.isTrusted(playerUUID)) {
            return false;
        }
        
        if (plugin.getDatabaseManager().removeTrustedPlayer(chunk.getId(), playerUUID)) {
            chunk.removeTrusted(playerUUID);
            
            // Update WorldGuard region
            plugin.getWorldGuardIntegration().removeMember(chunk.getWorld(), chunk.getWorldGuardRegion(), playerUUID);
            
            return true;
        }
        return false;
    }
    
    /**
     * Transfer chunk ownership
     */
    public void transferChunk(ChunkData chunk, Player newOwner) {
        UUID oldOwner = chunk.getOwnerUUID();
        
        // Update owner
        chunk.transferOwnership(newOwner.getUniqueId(), newOwner.getName());
        
        // Update database
        plugin.getDatabaseManager().updateChunk(chunk);
        
        // Update caches
        String key = chunk.getChunkKey();
        
        Set<String> oldOwnerChunks = playerChunks.get(oldOwner);
        if (oldOwnerChunks != null) {
            oldOwnerChunks.remove(key);
        }
        
        playerChunks.computeIfAbsent(newOwner.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(key);
        
        // Update WorldGuard region
        plugin.getWorldGuardIntegration().transferOwnership(chunk.getWorld(), 
                chunk.getWorldGuardRegion(), newOwner.getUniqueId());
    }
    
    /**
     * Get chunk by name for a player
     */
    public ChunkData getChunkByName(UUID playerUUID, String name) {
        return getPlayerChunks(playerUUID).stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get safe teleport location for a chunk
     */
    public Location getTeleportLocation(ChunkData chunkData) {
        World world = Bukkit.getWorld(chunkData.getWorld());
        if (world == null) return null;
        
        Chunk chunk = world.getChunkAt(chunkData.getChunkX(), chunkData.getChunkZ());
        return LocationUtils.getSafeTeleportLocation(chunk);
    }
    
    // ==================== Result Enums ====================
    
    public enum ClaimResult {
        SUCCESS,
        ALREADY_CLAIMED,
        WORLD_DISABLED,
        LIMIT_REACHED,
        NOT_ENOUGH_MONEY,
        TOO_CLOSE,
        NAME_EXISTS,
        NAME_TOO_LONG,
        WORLDGUARD_ERROR,
        DATABASE_ERROR
    }
    
    public enum UnclaimResult {
        SUCCESS,
        NOT_OWNER,
        NOT_CLAIMED
    }
}

