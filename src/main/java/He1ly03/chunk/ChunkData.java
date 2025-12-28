package He1ly03.chunk;

import He1ly03.utils.LocationUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a claimed chunk
 */
public class ChunkData {
    
    private int id;
    private final String world;
    private final int chunkX;
    private final int chunkZ;
    private String name;
    private UUID ownerUUID;
    private String ownerName;
    private ChunkSettings settings;
    
    // Sale info
    private boolean forSale;
    private double salePrice;
    private String saleLocation; // Serialized location
    
    // WorldGuard region name
    private String worldGuardRegion;
    
    // Cached trusted players (loaded on demand)
    private Map<UUID, String> trustedPlayers;
    
    public ChunkData(int id, String world, int chunkX, int chunkZ, String name, 
                     UUID ownerUUID, String ownerName, ChunkSettings settings) {
        this.id = id;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.settings = settings != null ? settings : new ChunkSettings();
        this.forSale = false;
        this.salePrice = 0;
        this.trustedPlayers = new ConcurrentHashMap<>();
    }
    
    /**
     * Create new ChunkData without ID (for new chunks)
     */
    public ChunkData(String world, int chunkX, int chunkZ, String name, 
                     UUID ownerUUID, String ownerName) {
        this(-1, world, chunkX, chunkZ, name, ownerUUID, ownerName, new ChunkSettings());
    }
    
    // ==================== Getters ====================
    
    public int getId() {
        return id;
    }
    
    public String getWorld() {
        return world;
    }
    
    public int getChunkX() {
        return chunkX;
    }
    
    public int getChunkZ() {
        return chunkZ;
    }
    
    public String getName() {
        return name;
    }
    
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public ChunkSettings getSettings() {
        return settings;
    }
    
    public boolean isForSale() {
        return forSale;
    }
    
    public double getSalePrice() {
        return salePrice;
    }
    
    public String getSaleLocation() {
        return saleLocation;
    }
    
    public String getWorldGuardRegion() {
        return worldGuardRegion;
    }
    
    public Map<UUID, String> getTrustedPlayers() {
        return trustedPlayers;
    }
    
    /**
     * Get the unique chunk key
     */
    public String getChunkKey() {
        return LocationUtils.getChunkKey(world, chunkX, chunkZ);
    }
    
    // ==================== Setters ====================
    
    public void setId(int id) {
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    public void setSettings(ChunkSettings settings) {
        this.settings = settings;
    }
    
    public void setForSale(boolean forSale) {
        this.forSale = forSale;
    }
    
    public void setSalePrice(double salePrice) {
        this.salePrice = salePrice;
    }
    
    public void setSaleLocation(String saleLocation) {
        this.saleLocation = saleLocation;
    }
    
    public void setWorldGuardRegion(String worldGuardRegion) {
        this.worldGuardRegion = worldGuardRegion;
    }
    
    public void setTrustedPlayers(Map<UUID, String> trustedPlayers) {
        this.trustedPlayers = trustedPlayers != null ? 
                new ConcurrentHashMap<>(trustedPlayers) : new ConcurrentHashMap<>();
    }
    
    // ==================== Trust Methods ====================
    
    /**
     * Check if a player is the owner
     */
    public boolean isOwner(UUID playerUUID) {
        return ownerUUID.equals(playerUUID);
    }
    
    /**
     * Check if a player is trusted
     */
    public boolean isTrusted(UUID playerUUID) {
        return trustedPlayers.containsKey(playerUUID);
    }
    
    /**
     * Check if a player can interact (is owner or trusted)
     */
    public boolean canInteract(UUID playerUUID) {
        return isOwner(playerUUID) || isTrusted(playerUUID);
    }
    
    /**
     * Add a trusted player
     */
    public void addTrusted(UUID playerUUID, String playerName) {
        trustedPlayers.put(playerUUID, playerName);
    }
    
    /**
     * Remove a trusted player
     */
    public void removeTrusted(UUID playerUUID) {
        trustedPlayers.remove(playerUUID);
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Transfer ownership to another player
     */
    public void transferOwnership(UUID newOwnerUUID, String newOwnerName) {
        this.ownerUUID = newOwnerUUID;
        this.ownerName = newOwnerName;
        this.trustedPlayers.clear();
    }
    
    /**
     * Put chunk for sale
     */
    public void putForSale(double price, String location) {
        this.forSale = true;
        this.salePrice = price;
        this.saleLocation = location;
    }
    
    /**
     * Remove from sale
     */
    public void removeFromSale() {
        this.forSale = false;
        this.salePrice = 0;
        this.saleLocation = null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkData chunkData = (ChunkData) o;
        return chunkX == chunkData.chunkX && chunkZ == chunkData.chunkZ && world.equals(chunkData.world);
    }
    
    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + chunkX;
        result = 31 * result + chunkZ;
        return result;
    }
    
    @Override
    public String toString() {
        return "ChunkData{" +
                "id=" + id +
                ", world='" + world + '\'' +
                ", chunkX=" + chunkX +
                ", chunkZ=" + chunkZ +
                ", name='" + name + '\'' +
                ", owner='" + ownerName + '\'' +
                '}';
    }
}

