package He1ly03.config;

import He1ly03.LiseryPrivate;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages all configuration files for the plugin
 */
public class ConfigManager {
    
    private final LiseryPrivate plugin;
    
    // Configuration files
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration holograms;
    private FileConfiguration integrations;
    
    // Menu configurations
    private FileConfiguration privateMenu;
    private FileConfiguration trustMenu;
    private FileConfiguration sellMenu;
    private FileConfiguration sellListMenu;
    private FileConfiguration settingsMenu;
    private FileConfiguration confirmMenu;
    
    public ConfigManager(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all configuration files
     */
    public void loadAll() {
        // Create plugin folder if not exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Load main configs
        this.config = loadConfig("config.yml");
        this.messages = loadConfig("messages.yml");
        this.holograms = loadConfig("holograms.yml");
        this.integrations = loadConfig("integrations.yml");
        
        // Create menu folder
        File menuFolder = new File(plugin.getDataFolder(), "menu");
        if (!menuFolder.exists()) {
            menuFolder.mkdirs();
        }
        
        // Load menu configs
        this.privateMenu = loadConfig("menu/private.yml");
        this.trustMenu = loadConfig("menu/trust.yml");
        this.sellMenu = loadConfig("menu/sell.yml");
        this.sellListMenu = loadConfig("menu/sell-list.yml");
        this.settingsMenu = loadConfig("menu/settings.yml");
        this.confirmMenu = loadConfig("menu/confirm.yml");
        
        // Copy reference files
        copyResourceFile("menu/action.txt");
        copyResourceFile("placeholders.txt");
        
        plugin.getLogger().info("All configurations loaded successfully!");
    }
    
    /**
     * Copy a resource file if it doesn't exist
     */
    private void copyResourceFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        
        if (!file.exists()) {
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            }
        }
    }
    
    /**
     * Reload all configuration files
     */
    public void reloadAll() {
        loadAll();
    }
    
    /**
     * Load a configuration file
     */
    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        
        if (!file.exists()) {
            // Try to save default from resources
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                // Create empty file
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create " + fileName, e);
                }
            }
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // Load defaults from resources if available
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            config.setDefaults(defaults);
        }
        
        return config;
    }
    
    // ==================== Main Settings ====================
    
    public String getMainCommand() {
        return config.getString("settings.main-command", "chunk");
    }
    
    // ==================== Storage Settings ====================
    
    public String getStorageType() {
        return config.getString("storage.type", "SQLITE").toUpperCase();
    }
    
    public String getSQLiteFile() {
        return config.getString("storage.sqlite.file", "chunkprivate.db");
    }
    
    public String getMySQLHost() {
        return config.getString("storage.mysql.host", "localhost");
    }
    
    public int getMySQLPort() {
        return config.getInt("storage.mysql.port", 3306);
    }
    
    public String getMySQLDatabase() {
        return config.getString("storage.mysql.database", "chunkprivate");
    }
    
    public String getMySQLUsername() {
        return config.getString("storage.mysql.username", "root");
    }
    
    public String getMySQLPassword() {
        return config.getString("storage.mysql.password", "password");
    }
    
    public boolean getMySQLUseSSL() {
        return config.getBoolean("storage.mysql.use-ssl", false);
    }
    
    public String getMySQLTablePrefix() {
        return config.getString("storage.mysql.table-prefix", "cp_");
    }
    
    // ==================== Economy Settings ====================
    
    public double getChunkPrivatePrice() {
        return config.getDouble("economy.chunk-private", 100.0);
    }
    
    public double getChunkUnprivateRefund() {
        return config.getDouble("economy.chunk-unprivate", 50.0);
    }
    
    public double getMinChunkPrice() {
        return config.getDouble("economy.min-chunk-price", 100.0);
    }
    
    public double getMaxChunkPrice() {
        return config.getDouble("economy.max-chunk-price", 10000.0);
    }
    
    public boolean isAuctionEnabled() {
        return config.getBoolean("economy.auction-enabled", true);
    }
    
    // ==================== World Settings ====================
    
    public List<String> getDisabledWorlds() {
        return config.getStringList("worlds.disabled-worlds");
    }
    
    public boolean isWorldDisabled(String worldName) {
        return getDisabledWorlds().contains(worldName);
    }
    
    // ==================== Limits Settings ====================
    
    public int getDefaultLimit() {
        return config.getInt("limits.default", 3);
    }
    
    public int getGroupLimit(String group) {
        return config.getInt("limits.groups." + group, getDefaultLimit());
    }
    
    // ==================== Protection Settings ====================
    
    public boolean isProtectBuild() {
        return config.getBoolean("protection.protect-build", true);
    }
    
    public boolean isProtectDestroy() {
        return config.getBoolean("protection.protect-destroy", true);
    }
    
    public boolean isProtectUse() {
        return config.getBoolean("protection.protect-use", true);
    }
    
    public boolean isProtectSwitch() {
        return config.getBoolean("protection.protect-switch", true);
    }
    
    public boolean isProtectMobs() {
        return config.getBoolean("protection.protect-mobs", true);
    }
    
    public boolean isProtectPvP() {
        return config.getBoolean("protection.protect-pvp", true);
    }
    
    public boolean isProtectFire() {
        return config.getBoolean("protection.protect-fire", true);
    }
    
    public boolean isProtectExplosion() {
        return config.getBoolean("protection.protect-explosion", true);
    }
    
    // ==================== Chunk Settings ====================
    
    public int getMaxNameLength() {
        return config.getInt("chunk.max-name-length", 16);
    }
    
    public int getMinDistance() {
        return config.getInt("chunk.min-distance", 0);
    }
    
    // ==================== Effects Settings ====================
    
    public String getEnterTitle() {
        return config.getString("effects.enter.message.title", "");
    }
    
    public String getEnterSubtitle() {
        return config.getString("effects.enter.message.subtitle", "");
    }
    
    public String getEnterChat() {
        return config.getString("effects.enter.message.chat", "");
    }
    
    public String getEnterActionbar() {
        return config.getString("effects.enter.message.actionbar", "");
    }
    
    public Sound getEnterSound() {
        String soundName = config.getString("effects.enter.sound.type", "");
        if (soundName == null || soundName.isEmpty()) return null;
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public float getEnterSoundVolume() {
        return (float) config.getDouble("effects.enter.sound.volume", 1.0);
    }
    
    public float getEnterSoundPitch() {
        return (float) config.getDouble("effects.enter.sound.pitch", 1.0);
    }
    
    public String getExitTitle() {
        return config.getString("effects.exit.message.title", "");
    }
    
    public String getExitSubtitle() {
        return config.getString("effects.exit.message.subtitle", "");
    }
    
    public String getExitChat() {
        return config.getString("effects.exit.message.chat", "");
    }
    
    public String getExitActionbar() {
        return config.getString("effects.exit.message.actionbar", "");
    }
    
    public Sound getExitSound() {
        String soundName = config.getString("effects.exit.sound.type", "");
        if (soundName == null || soundName.isEmpty()) return null;
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public float getExitSoundVolume() {
        return (float) config.getDouble("effects.exit.sound.volume", 1.0);
    }
    
    public float getExitSoundPitch() {
        return (float) config.getDouble("effects.exit.sound.pitch", 1.0);
    }
    
    public Particle getTeleportParticle() {
        String particleName = config.getString("effects.teleport.particle.type", "");
        if (particleName == null || particleName.isEmpty()) return null;
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public String getTeleportParticleColor() {
        return config.getString("effects.teleport.particle.color", "0:245:130");
    }
    
    public Sound getTeleportSound() {
        String soundName = config.getString("effects.teleport.sound.type", "");
        if (soundName == null || soundName.isEmpty()) return null;
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public float getTeleportSoundVolume() {
        return (float) config.getDouble("effects.teleport.sound.volume", 1.0);
    }
    
    public float getTeleportSoundPitch() {
        return (float) config.getDouble("effects.teleport.sound.pitch", 1.0);
    }
    
    // ==================== PvP Format ====================
    
    public String getPvPFormatTrue() {
        return config.getString("pvp-format.true", "&cPvP");
    }
    
    public String getPvPFormatFalse() {
        return config.getString("pvp-format.false", "&fБез &aPvP");
    }
    
    public String getSettingsFormatTrue() {
        return config.getString("settings-format.true", "&a✔");
    }
    
    public String getSettingsFormatFalse() {
        return config.getString("settings-format.false", "&c❌");
    }
    
    // ==================== Wand Item ====================
    
    public Material getWandMaterial() {
        String materialName = config.getString("wand-item.item", "BREEZE_ROD");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.BLAZE_ROD;
        }
    }
    
    public boolean isWandProtected() {
        return config.getBoolean("wand-item.protect-use", true);
    }
    
    public String getWandName() {
        return config.getString("wand-item.name", "&aРедактор регионов");
    }
    
    public List<String> getWandLore() {
        return config.getStringList("wand-item.lore");
    }
    
    // ==================== Outline Particle ====================
    
    public Particle getOutlineParticle() {
        String particleName = config.getString("outline-particle.type", "DUST");
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Particle.DUST;
        }
    }
    
    public String getOutlineParticleColor() {
        return config.getString("outline-particle.color", "0:245:130");
    }
    
    // ==================== Messages ====================
    
    public String getPrefix() {
        return messages.getString("prefix", "");
    }
    
    public String getMessage(String key) {
        String message = messages.getString(key, "");
        String prefix = getPrefix();
        if (!message.isEmpty() && !prefix.isEmpty()) {
            return prefix + message;
        }
        return message;
    }
    
    public String getMessageRaw(String key) {
        return messages.getString(key, "");
    }
    
    public String getTrustFormatPlayer() {
        return messages.getString("trust-format.player", "&f%player%&7, ");
    }
    
    public String getTrustFormatNoPlayers() {
        return messages.getString("trust-format.no-players", "&7Нет доверенных игроков.");
    }
    
    // ==================== Holograms ====================
    
    public double getHologramHeight() {
        return holograms.getDouble("hologram-height", 3.0);
    }
    
    public String getTextEntityBillboard() {
        return holograms.getString("text-entity-settings.billboard", "center");
    }
    
    public String getTextEntityBackgroundColor() {
        return holograms.getString("text-entity-settings.background-color", "#C8000000");
    }
    
    public double getTextEntityScale() {
        return holograms.getDouble("text-entity-settings.scale", 1.5);
    }
    
    public boolean isHologramEnabled(String type) {
        return holograms.getBoolean("holograms." + type + ".enabled", true);
    }
    
    public boolean isHologramRegionEditorOnly(String type) {
        return holograms.getBoolean("holograms." + type + ".region-editor-display-only", true);
    }
    
    public List<String> getHologramLines(String type) {
        return holograms.getStringList("holograms." + type + ".lines");
    }
    
    // ==================== Integrations ====================
    
    public boolean isLuckPermsEnabled() {
        return integrations.getBoolean("plugin-integrations.LuckPerms.enabled", true);
    }
    
    public boolean isVaultEnabled() {
        return integrations.getBoolean("plugin-integrations.Vault.enabled", true);
    }
    
    public boolean isCoinsEngineEnabled() {
        return integrations.getBoolean("plugin-integrations.CoinsEngine.enabled", true);
    }
    
    public String getCoinsEngineCurrency() {
        return integrations.getString("plugin-integrations.CoinsEngine.currency-name", "money");
    }
    
    public boolean isPlaceholderAPIEnabled() {
        return integrations.getBoolean("plugin-integrations.PlaceholderAPI.enabled", true);
    }
    
    // ==================== Menu Configs ====================
    
    public FileConfiguration getPrivateMenuConfig() {
        return privateMenu;
    }
    
    public FileConfiguration getTrustMenuConfig() {
        return trustMenu;
    }
    
    public FileConfiguration getSellMenuConfig() {
        return sellMenu;
    }
    
    public FileConfiguration getSellListMenuConfig() {
        return sellListMenu;
    }
    
    public FileConfiguration getConfirmMenuConfig() {
        return confirmMenu;
    }
    
    public FileConfiguration getMenuConfig(String menuName) {
        return switch (menuName.toLowerCase()) {
            case "private" -> privateMenu;
            case "trust" -> trustMenu;
            case "sell" -> sellMenu;
            case "sell-list" -> sellListMenu;
            case "settings" -> settingsMenu;
            case "confirm" -> confirmMenu;
            default -> null;
        };
    }
    
    // ==================== Getters ====================
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getMessages() {
        return messages;
    }
    
    public FileConfiguration getHolograms() {
        return holograms;
    }
    
    public FileConfiguration getIntegrations() {
        return integrations;
    }
}

