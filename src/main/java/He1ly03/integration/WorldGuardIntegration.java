package He1ly03.integration;

import He1ly03.LiseryPrivate;
import He1ly03.chunk.ChunkData;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

/**
 * Integration with WorldGuard for region protection
 */
public class WorldGuardIntegration {
    
    private final LiseryPrivate plugin;
    private WorldGuard worldGuard;
    private RegionContainer regionContainer;
    private boolean enabled = false;
    
    public WorldGuardIntegration(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize WorldGuard integration
     */
    public boolean initialize() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            plugin.getLogger().severe("WorldGuard not found! This plugin requires WorldGuard to function.");
            return false;
        }
        
        try {
            this.worldGuard = WorldGuard.getInstance();
            this.regionContainer = worldGuard.getPlatform().getRegionContainer();
            this.enabled = true;
            plugin.getLogger().info("WorldGuard integration enabled!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize WorldGuard integration!", e);
            return false;
        }
    }
    
    /**
     * Get region manager for a world
     */
    private RegionManager getRegionManager(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return regionContainer.get(BukkitAdapter.adapt(world));
    }
    
    /**
     * Create a protected region for a chunk
     */
    public String createRegion(Player owner, ChunkData chunkData) {
        if (!enabled) return null;
        
        RegionManager regionManager = getRegionManager(chunkData.getWorld());
        if (regionManager == null) return null;
        
        // Generate unique region name
        String regionName = generateRegionName(owner.getName(), regionManager);
        
        // Calculate region bounds (full chunk from bedrock to sky)
        int minX = chunkData.getChunkX() << 4;
        int minZ = chunkData.getChunkZ() << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        
        World world = Bukkit.getWorld(chunkData.getWorld());
        int minY = world != null ? world.getMinHeight() : -64;
        int maxY = world != null ? world.getMaxHeight() : 320;
        
        BlockVector3 min = BlockVector3.at(minX, minY, minZ);
        BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);
        
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, min, max);
        
        // Set owner
        DefaultDomain owners = new DefaultDomain();
        owners.addPlayer(owner.getUniqueId());
        region.setOwners(owners);
        
        // Set priority
        region.setPriority(10);
        
        try {
            regionManager.addRegion(region);
            regionManager.save();
            return regionName;
        } catch (StorageException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save WorldGuard region!", e);
            return null;
        }
    }
    
    /**
     * Generate a unique region name
     */
    private String generateRegionName(String playerName, RegionManager regionManager) {
        String baseName = playerName.toLowerCase();
        int counter = 1;
        
        while (regionManager.hasRegion(baseName + "_" + counter)) {
            counter++;
        }
        
        return baseName + "_" + counter;
    }
    
    /**
     * Delete a region
     */
    public void deleteRegion(String worldName, String regionName) {
        if (!enabled || regionName == null) return;
        
        RegionManager regionManager = getRegionManager(worldName);
        if (regionManager == null) return;
        
        regionManager.removeRegion(regionName);
        
        try {
            regionManager.save();
        } catch (StorageException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save WorldGuard after region deletion", e);
        }
    }
    
    /**
     * Add a member to a region
     */
    public void addMember(String worldName, String regionName, UUID playerUUID) {
        if (!enabled || regionName == null) return;
        
        RegionManager regionManager = getRegionManager(worldName);
        if (regionManager == null) return;
        
        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) return;
        
        DefaultDomain members = region.getMembers();
        members.addPlayer(playerUUID);
        region.setMembers(members);
        
        saveRegionManager(regionManager);
    }
    
    /**
     * Remove a member from a region
     */
    public void removeMember(String worldName, String regionName, UUID playerUUID) {
        if (!enabled || regionName == null) return;
        
        RegionManager regionManager = getRegionManager(worldName);
        if (regionManager == null) return;
        
        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) return;
        
        DefaultDomain members = region.getMembers();
        members.removePlayer(playerUUID);
        region.setMembers(members);
        
        saveRegionManager(regionManager);
    }
    
    /**
     * Transfer region ownership
     */
    public void transferOwnership(String worldName, String regionName, UUID newOwnerUUID) {
        if (!enabled || regionName == null) return;
        
        RegionManager regionManager = getRegionManager(worldName);
        if (regionManager == null) return;
        
        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) return;
        
        DefaultDomain owners = new DefaultDomain();
        owners.addPlayer(newOwnerUUID);
        region.setOwners(owners);
        region.setMembers(new DefaultDomain()); // Clear members
        
        saveRegionManager(regionManager);
    }
    
    /**
     * Merge adjacent regions when chunks are connected
     */
    public void mergeRegions(ChunkData newChunk, List<ChunkData> adjacentChunks) {
        if (!enabled || adjacentChunks.isEmpty()) return;
        
        RegionManager regionManager = getRegionManager(newChunk.getWorld());
        if (regionManager == null) return;
        
        // Get the new region
        ProtectedRegion newRegion = regionManager.getRegion(newChunk.getWorldGuardRegion());
        if (newRegion == null) return;
        
        // Find the main region to expand (use the first adjacent region as base)
        ChunkData mainChunk = adjacentChunks.get(0);
        ProtectedRegion mainRegion = regionManager.getRegion(mainChunk.getWorldGuardRegion());
        if (mainRegion == null) return;
        
        // Calculate merged bounds
        int minX = Math.min(mainRegion.getMinimumPoint().x(), newRegion.getMinimumPoint().x());
        int minY = Math.min(mainRegion.getMinimumPoint().y(), newRegion.getMinimumPoint().y());
        int minZ = Math.min(mainRegion.getMinimumPoint().z(), newRegion.getMinimumPoint().z());
        int maxX = Math.max(mainRegion.getMaximumPoint().x(), newRegion.getMaximumPoint().x());
        int maxY = Math.max(mainRegion.getMaximumPoint().y(), newRegion.getMaximumPoint().y());
        int maxZ = Math.max(mainRegion.getMaximumPoint().z(), newRegion.getMaximumPoint().z());
        
        // Include all adjacent chunks
        for (ChunkData adj : adjacentChunks) {
            ProtectedRegion adjRegion = regionManager.getRegion(adj.getWorldGuardRegion());
            if (adjRegion != null) {
                minX = Math.min(minX, adjRegion.getMinimumPoint().x());
                minY = Math.min(minY, adjRegion.getMinimumPoint().y());
                minZ = Math.min(minZ, adjRegion.getMinimumPoint().z());
                maxX = Math.max(maxX, adjRegion.getMaximumPoint().x());
                maxY = Math.max(maxY, adjRegion.getMaximumPoint().y());
                maxZ = Math.max(maxZ, adjRegion.getMaximumPoint().z());
            }
        }
        
        // Create merged region with new bounds
        String mergedName = newChunk.getWorldGuardRegion();
        ProtectedCuboidRegion mergedRegion = new ProtectedCuboidRegion(
                mergedName,
                BlockVector3.at(minX, minY, minZ),
                BlockVector3.at(maxX, maxY, maxZ)
        );
        
        // Copy owners and members from main region
        mergedRegion.setOwners(mainRegion.getOwners());
        mergedRegion.setMembers(mainRegion.getMembers());
        mergedRegion.setPriority(mainRegion.getPriority());
        
        // Remove old regions and add merged
        regionManager.removeRegion(newChunk.getWorldGuardRegion());
        for (ChunkData adj : adjacentChunks) {
            if (adj.getWorldGuardRegion() != null) {
                regionManager.removeRegion(adj.getWorldGuardRegion());
                // Update the chunk to use the merged region name
                adj.setWorldGuardRegion(mergedName);
                plugin.getDatabaseManager().updateChunk(adj);
            }
        }
        
        regionManager.addRegion(mergedRegion);
        saveRegionManager(regionManager);
    }
    
    /**
     * Unmerge regions when a chunk is removed
     */
    public void unmergeRegions(ChunkData removedChunk, List<ChunkData> remainingChunks) {
        if (!enabled || remainingChunks.isEmpty()) return;
        
        RegionManager regionManager = getRegionManager(removedChunk.getWorld());
        if (regionManager == null) return;
        
        World world = Bukkit.getWorld(removedChunk.getWorld());
        int minY = world != null ? world.getMinHeight() : -64;
        int maxY = world != null ? world.getMaxHeight() : 320;
        
        // Recreate individual regions for remaining chunks
        for (ChunkData chunk : remainingChunks) {
            // Calculate individual chunk bounds
            int chunkMinX = chunk.getChunkX() << 4;
            int chunkMinZ = chunk.getChunkZ() << 4;
            int chunkMaxX = chunkMinX + 15;
            int chunkMaxZ = chunkMinZ + 15;
            
            // Generate new region name
            String newRegionName = generateRegionName(chunk.getOwnerName(), regionManager);
            
            ProtectedCuboidRegion newRegion = new ProtectedCuboidRegion(
                    newRegionName,
                    BlockVector3.at(chunkMinX, minY, chunkMinZ),
                    BlockVector3.at(chunkMaxX, maxY, chunkMaxZ)
            );
            
            // Set owner
            DefaultDomain owners = new DefaultDomain();
            owners.addPlayer(chunk.getOwnerUUID());
            newRegion.setOwners(owners);
            
            // Add trusted players as members
            for (UUID trusted : chunk.getTrustedPlayers().keySet()) {
                newRegion.getMembers().addPlayer(trusted);
            }
            
            newRegion.setPriority(10);
            
            regionManager.addRegion(newRegion);
            
            // Update chunk data
            chunk.setWorldGuardRegion(newRegionName);
            plugin.getDatabaseManager().updateChunk(chunk);
        }
        
        // Remove old merged region if it exists
        if (removedChunk.getWorldGuardRegion() != null) {
            regionManager.removeRegion(removedChunk.getWorldGuardRegion());
        }
        
        saveRegionManager(regionManager);
    }
    
    /**
     * Check if a region exists
     */
    public boolean regionExists(String worldName, String regionName) {
        if (!enabled || regionName == null) return false;
        
        RegionManager regionManager = getRegionManager(worldName);
        return regionManager != null && regionManager.hasRegion(regionName);
    }
    
    /**
     * Get a region
     */
    public ProtectedRegion getRegion(String worldName, String regionName) {
        if (!enabled || regionName == null) return null;
        
        RegionManager regionManager = getRegionManager(worldName);
        if (regionManager == null) return null;
        
        return regionManager.getRegion(regionName);
    }
    
    private void saveRegionManager(RegionManager regionManager) {
        try {
            regionManager.save();
        } catch (StorageException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save WorldGuard regions", e);
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}

