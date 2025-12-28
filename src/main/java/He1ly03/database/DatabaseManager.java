package He1ly03.database;

import He1ly03.LiseryPrivate;
import He1ly03.chunk.ChunkData;
import He1ly03.chunk.ChunkSettings;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Database manager for SQLite and MySQL support
 * Uses built-in SQLite from Paper/Spigot
 */
public class DatabaseManager {
    
    private final LiseryPrivate plugin;
    private Connection connection;
    private String tablePrefix;
    private boolean isMySQL;
    private String jdbcUrl;
    
    public DatabaseManager(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize the database connection
     */
    public boolean initialize() {
        String storageType = plugin.getConfigManager().getStorageType();
        this.isMySQL = storageType.equalsIgnoreCase("MYSQL");
        this.tablePrefix = isMySQL ? plugin.getConfigManager().getMySQLTablePrefix() : "";
        
        try {
            if (isMySQL) {
                initializeMySQL();
            } else {
                initializeSQLite();
            }
            
            createTables();
            plugin.getLogger().info("Database initialized successfully! Type: " + storageType);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database!", e);
            e.printStackTrace();
            return false;
        }
    }
    
    private void initializeSQLite() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File dbFile = new File(dataFolder, plugin.getConfigManager().getSQLiteFile());
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        this.connection = DriverManager.getConnection(jdbcUrl);
        
        // Enable foreign keys
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }
    
    private void initializeMySQL() throws SQLException {
        var configManager = plugin.getConfigManager();
        
        this.jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=%b&autoReconnect=true",
                configManager.getMySQLHost(),
                configManager.getMySQLPort(),
                configManager.getMySQLDatabase(),
                configManager.getMySQLUseSSL()
        );
        
        this.connection = DriverManager.getConnection(
                jdbcUrl,
                configManager.getMySQLUsername(),
                configManager.getMySQLPassword()
        );
    }
    
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            if (isMySQL) {
                connection = DriverManager.getConnection(
                        jdbcUrl,
                        plugin.getConfigManager().getMySQLUsername(),
                        plugin.getConfigManager().getMySQLPassword()
                );
            } else {
                connection = DriverManager.getConnection(jdbcUrl);
            }
        }
        return connection;
    }
    
    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // Chunks table
            String autoIncrement = isMySQL ? "AUTO_INCREMENT" : "AUTOINCREMENT";
            
            String chunksTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "chunks (" +
                    "id INTEGER PRIMARY KEY " + autoIncrement + "," +
                    "world VARCHAR(64) NOT NULL," +
                    "chunk_x INTEGER NOT NULL," +
                    "chunk_z INTEGER NOT NULL," +
                    "name VARCHAR(64) NOT NULL," +
                    "owner_uuid VARCHAR(36) NOT NULL," +
                    "owner_name VARCHAR(16) NOT NULL," +
                    "build_allowed BOOLEAN DEFAULT 0," +
                    "destroy_allowed BOOLEAN DEFAULT 0," +
                    "use_allowed BOOLEAN DEFAULT 0," +
                    "switch_allowed BOOLEAN DEFAULT 0," +
                    "mobs_allowed BOOLEAN DEFAULT 0," +
                    "pvp_allowed BOOLEAN DEFAULT 0," +
                    "fire_allowed BOOLEAN DEFAULT 0," +
                    "explosion_allowed BOOLEAN DEFAULT 0," +
                    "for_sale BOOLEAN DEFAULT 0," +
                    "sale_price DOUBLE DEFAULT 0," +
                    "sale_location VARCHAR(255) DEFAULT NULL," +
                    "worldguard_region VARCHAR(128) DEFAULT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE(world, chunk_x, chunk_z)" +
                    ")";
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(chunksTable);
            }
            
            // Trusted players table
            String trustTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "trusted_players (" +
                    "id INTEGER PRIMARY KEY " + autoIncrement + "," +
                    "chunk_id INTEGER NOT NULL," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(16) NOT NULL," +
                    "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE(chunk_id, player_uuid)" +
                    ")";
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(trustTable);
            }
        }
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing database connection", e);
        }
    }
    
    // ==================== Chunk Operations ====================
    
    /**
     * Save a chunk to the database
     */
    public int saveChunk(ChunkData chunk) {
        String sql = "INSERT INTO " + tablePrefix + "chunks " +
                "(world, chunk_x, chunk_z, name, owner_uuid, owner_name, " +
                "build_allowed, destroy_allowed, use_allowed, switch_allowed, mobs_allowed, " +
                "pvp_allowed, fire_allowed, explosion_allowed, for_sale, sale_price, sale_location, worldguard_region) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, chunk.getWorld());
            stmt.setInt(2, chunk.getChunkX());
            stmt.setInt(3, chunk.getChunkZ());
            stmt.setString(4, chunk.getName());
            stmt.setString(5, chunk.getOwnerUUID().toString());
            stmt.setString(6, chunk.getOwnerName());
            
            ChunkSettings settings = chunk.getSettings();
            stmt.setBoolean(7, settings.isBuildAllowed());
            stmt.setBoolean(8, settings.isDestroyAllowed());
            stmt.setBoolean(9, settings.isUseAllowed());
            stmt.setBoolean(10, settings.isSwitchAllowed());
            stmt.setBoolean(11, settings.isMobsAllowed());
            stmt.setBoolean(12, settings.isPvpAllowed());
            stmt.setBoolean(13, settings.isFireAllowed());
            stmt.setBoolean(14, settings.isExplosionAllowed());
            stmt.setBoolean(15, chunk.isForSale());
            stmt.setDouble(16, chunk.getSalePrice());
            stmt.setString(17, chunk.getSaleLocation());
            stmt.setString(18, chunk.getWorldGuardRegion());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save chunk!", e);
        }
        return -1;
    }
    
    /**
     * Update a chunk in the database
     */
    public void updateChunk(ChunkData chunk) {
        String sql = "UPDATE " + tablePrefix + "chunks SET name = ?, " +
                "build_allowed = ?, destroy_allowed = ?, use_allowed = ?, " +
                "switch_allowed = ?, mobs_allowed = ?, pvp_allowed = ?, fire_allowed = ?, explosion_allowed = ?, " +
                "for_sale = ?, sale_price = ?, sale_location = ?, worldguard_region = ? " +
                "WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, chunk.getName());
            
            ChunkSettings settings = chunk.getSettings();
            stmt.setBoolean(2, settings.isBuildAllowed());
            stmt.setBoolean(3, settings.isDestroyAllowed());
            stmt.setBoolean(4, settings.isUseAllowed());
            stmt.setBoolean(5, settings.isSwitchAllowed());
            stmt.setBoolean(6, settings.isMobsAllowed());
            stmt.setBoolean(7, settings.isPvpAllowed());
            stmt.setBoolean(8, settings.isFireAllowed());
            stmt.setBoolean(9, settings.isExplosionAllowed());
            stmt.setBoolean(10, chunk.isForSale());
            stmt.setDouble(11, chunk.getSalePrice());
            stmt.setString(12, chunk.getSaleLocation());
            stmt.setString(13, chunk.getWorldGuardRegion());
            stmt.setInt(14, chunk.getId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update chunk!", e);
        }
    }
    
    /**
     * Delete a chunk from the database
     */
    public void deleteChunk(int chunkId) {
        // Delete trusted players first
        String deleteTrust = "DELETE FROM " + tablePrefix + "trusted_players WHERE chunk_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteTrust)) {
            stmt.setInt(1, chunkId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete trusted players!", e);
        }
        
        String sql = "DELETE FROM " + tablePrefix + "chunks WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chunkId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete chunk!", e);
        }
    }
    
    /**
     * Get a chunk by location
     */
    public ChunkData getChunk(String world, int chunkX, int chunkZ) {
        String sql = "SELECT * FROM " + tablePrefix + "chunks WHERE world = ? AND chunk_x = ? AND chunk_z = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, world);
            stmt.setInt(2, chunkX);
            stmt.setInt(3, chunkZ);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return parseChunkData(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get chunk!", e);
        }
        return null;
    }
    
    /**
     * Get all chunks owned by a player
     */
    public List<ChunkData> getPlayerChunks(UUID playerUUID) {
        List<ChunkData> chunks = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "chunks WHERE owner_uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    chunks.add(parseChunkData(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get player chunks!", e);
        }
        return chunks;
    }
    
    /**
     * Get the count of chunks owned by a player
     */
    public int getPlayerChunkCount(UUID playerUUID) {
        String sql = "SELECT COUNT(*) FROM " + tablePrefix + "chunks WHERE owner_uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get player chunk count!", e);
        }
        return 0;
    }
    
    /**
     * Check if a chunk name exists for a player
     */
    public boolean chunkNameExists(UUID playerUUID, String name) {
        String sql = "SELECT COUNT(*) FROM " + tablePrefix + "chunks WHERE owner_uuid = ? AND name = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check chunk name!", e);
        }
        return false;
    }
    
    /**
     * Get the next available chunk number for a player
     */
    public int getNextChunkNumber(UUID playerUUID, String prefix) {
        String sql = "SELECT name FROM " + tablePrefix + "chunks WHERE owner_uuid = ? AND name LIKE ?";
        
        Set<Integer> usedNumbers = new HashSet<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, prefix + "_%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    if (name.startsWith(prefix + "_")) {
                        try {
                            int num = Integer.parseInt(name.substring(prefix.length() + 1));
                            usedNumbers.add(num);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get next chunk number!", e);
        }
        
        int number = 1;
        while (usedNumbers.contains(number)) {
            number++;
        }
        return number;
    }
    
    /**
     * Get all chunks for sale
     */
    public List<ChunkData> getChunksForSale() {
        List<ChunkData> chunks = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "chunks WHERE for_sale = 1";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                chunks.add(parseChunkData(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get chunks for sale!", e);
        }
        return chunks;
    }
    
    /**
     * Load all chunks from database
     */
    public List<ChunkData> loadAllChunks() {
        List<ChunkData> chunks = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "chunks";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                chunks.add(parseChunkData(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load all chunks!", e);
        }
        return chunks;
    }
    
    private ChunkData parseChunkData(ResultSet rs) throws SQLException {
        ChunkSettings settings = new ChunkSettings(
                rs.getBoolean("build_allowed"),
                rs.getBoolean("destroy_allowed"),
                rs.getBoolean("use_allowed"),
                rs.getBoolean("switch_allowed"),
                rs.getBoolean("mobs_allowed"),
                rs.getBoolean("pvp_allowed"),
                rs.getBoolean("fire_allowed"),
                rs.getBoolean("explosion_allowed")
        );
        
        ChunkData chunk = new ChunkData(
                rs.getInt("id"),
                rs.getString("world"),
                rs.getInt("chunk_x"),
                rs.getInt("chunk_z"),
                rs.getString("name"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("owner_name"),
                settings
        );
        
        chunk.setForSale(rs.getBoolean("for_sale"));
        chunk.setSalePrice(rs.getDouble("sale_price"));
        chunk.setSaleLocation(rs.getString("sale_location"));
        chunk.setWorldGuardRegion(rs.getString("worldguard_region"));
        
        return chunk;
    }
    
    // ==================== Trust Operations ====================
    
    /**
     * Add a trusted player to a chunk
     */
    public boolean addTrustedPlayer(int chunkId, UUID playerUUID, String playerName) {
        String sql = "INSERT INTO " + tablePrefix + "trusted_players (chunk_id, player_uuid, player_name) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, chunkId);
            stmt.setString(2, playerUUID.toString());
            stmt.setString(3, playerName);
            
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (!e.getMessage().contains("UNIQUE")) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add trusted player!", e);
            }
        }
        return false;
    }
    
    /**
     * Remove a trusted player from a chunk
     */
    public boolean removeTrustedPlayer(int chunkId, UUID playerUUID) {
        String sql = "DELETE FROM " + tablePrefix + "trusted_players WHERE chunk_id = ? AND player_uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, chunkId);
            stmt.setString(2, playerUUID.toString());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove trusted player!", e);
        }
        return false;
    }
    
    /**
     * Get all trusted players for a chunk
     */
    public Map<UUID, String> getTrustedPlayers(int chunkId) {
        Map<UUID, String> trusted = new LinkedHashMap<>();
        String sql = "SELECT player_uuid, player_name FROM " + tablePrefix + "trusted_players WHERE chunk_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, chunkId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trusted.put(
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get trusted players!", e);
        }
        return trusted;
    }
    
    /**
     * Check if a player is trusted in a chunk
     */
    public boolean isTrusted(int chunkId, UUID playerUUID) {
        String sql = "SELECT COUNT(*) FROM " + tablePrefix + "trusted_players WHERE chunk_id = ? AND player_uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, chunkId);
            stmt.setString(2, playerUUID.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check trusted player!", e);
        }
        return false;
    }
}
