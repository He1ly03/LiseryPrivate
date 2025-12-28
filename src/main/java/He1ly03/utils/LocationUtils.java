package He1ly03.utils;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * Utility class for location operations
 */
public final class LocationUtils {
    
    private LocationUtils() {}
    
    /**
     * Get chunk key from location
     */
    public static String getChunkKey(Location location) {
        return getChunkKey(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }
    
    /**
     * Get chunk key from world and coordinates
     */
    public static String getChunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }
    
    /**
     * Parse chunk key to components
     * @return array of [world, chunkX, chunkZ] or null if invalid
     */
    public static String[] parseChunkKey(String key) {
        if (key == null) return null;
        String[] parts = key.split(":");
        if (parts.length != 3) return null;
        return parts;
    }
    
    /**
     * Get the center of a chunk at safe Y level
     */
    public static Location getChunkCenter(Chunk chunk) {
        World world = chunk.getWorld();
        int x = (chunk.getX() << 4) + 8;
        int z = (chunk.getZ() << 4) + 8;
        
        // Find safe Y level
        int y = getSafeY(world, x, z);
        
        return new Location(world, x + 0.5, y, z + 0.5);
    }
    
    /**
     * Find a safe Y level at given X, Z coordinates
     */
    public static int getSafeY(World world, int x, int z) {
        int highestY = world.getHighestBlockYAt(x, z);
        
        // Check from highest block down
        for (int y = highestY; y > world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            Block above = block.getRelative(BlockFace.UP);
            Block aboveAbove = above.getRelative(BlockFace.UP);
            
            if (isSolid(block) && isSafe(above) && isSafe(aboveAbove)) {
                return y + 1;
            }
        }
        
        return highestY + 1;
    }
    
    /**
     * Find a safe teleport location in a chunk
     */
    public static Location getSafeTeleportLocation(Chunk chunk) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        
        // Try center first
        Location center = getChunkCenter(chunk);
        if (isSafeLocation(center)) {
            return center;
        }
        
        // Try corners and edges
        int[][] offsets = {
                {8, 8}, {4, 4}, {12, 4}, {4, 12}, {12, 12},
                {0, 0}, {15, 0}, {0, 15}, {15, 15}
        };
        
        for (int[] offset : offsets) {
            int x = baseX + offset[0];
            int z = baseZ + offset[1];
            int y = getSafeY(world, x, z);
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            if (isSafeLocation(loc)) {
                return loc;
            }
        }
        
        // Fallback to center
        return center;
    }
    
    /**
     * Check if a location is safe for teleportation
     */
    public static boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);
        
        return isSolid(ground) && isSafe(feet) && isSafe(head);
    }
    
    /**
     * Check if a block is solid (can stand on)
     */
    private static boolean isSolid(Block block) {
        Material type = block.getType();
        return type.isSolid() && !type.name().contains("SIGN") && !type.name().contains("BANNER");
    }
    
    /**
     * Check if a block is safe (can be inside)
     */
    private static boolean isSafe(Block block) {
        Material type = block.getType();
        return !type.isSolid() && type != Material.LAVA && type != Material.FIRE && 
               type != Material.CACTUS && type != Material.SWEET_BERRY_BUSH;
    }
    
    /**
     * Get chunk coordinates from a chunk
     */
    public static int[] getChunkCoords(Chunk chunk) {
        return new int[]{chunk.getX(), chunk.getZ()};
    }
    
    /**
     * Check if two chunks are adjacent
     */
    public static boolean areChunksAdjacent(Chunk chunk1, Chunk chunk2) {
        if (!chunk1.getWorld().equals(chunk2.getWorld())) {
            return false;
        }
        
        int dx = Math.abs(chunk1.getX() - chunk2.getX());
        int dz = Math.abs(chunk1.getZ() - chunk2.getZ());
        
        return (dx == 1 && dz == 0) || (dx == 0 && dz == 1);
    }
    
    /**
     * Check if a chunk is within distance of another chunk
     */
    public static boolean isWithinDistance(Chunk chunk1, Chunk chunk2, int distance) {
        if (!chunk1.getWorld().equals(chunk2.getWorld())) {
            return false;
        }
        
        int dx = Math.abs(chunk1.getX() - chunk2.getX());
        int dz = Math.abs(chunk1.getZ() - chunk2.getZ());
        
        return dx <= distance && dz <= distance;
    }
    
    /**
     * Get chunk distance between two chunks (Chebyshev distance)
     */
    public static int getChunkDistance(Chunk chunk1, Chunk chunk2) {
        if (!chunk1.getWorld().equals(chunk2.getWorld())) {
            return Integer.MAX_VALUE;
        }
        
        int dx = Math.abs(chunk1.getX() - chunk2.getX());
        int dz = Math.abs(chunk1.getZ() - chunk2.getZ());
        
        return Math.max(dx, dz);
    }
}

