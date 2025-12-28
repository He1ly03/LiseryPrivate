package He1ly03;

import He1ly03.chunk.ChunkManager;
import He1ly03.command.ChunkCommand;
import He1ly03.config.ConfigManager;
import He1ly03.database.DatabaseManager;
import He1ly03.hologram.HologramManager;
import He1ly03.integration.IntegrationManager;
import He1ly03.integration.WorldGuardIntegration;
import He1ly03.listener.ChunkEnterListener;
import He1ly03.listener.ProtectionListener;
import He1ly03.menu.MenuListener;
import He1ly03.menu.MenuManager;
import He1ly03.utils.LogoUtils;
import He1ly03.utils.UpdateChecker;
import He1ly03.wand.WandListener;
import He1ly03.wand.WandManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * LiseryPrivate - Chunk claiming plugin for Minecraft servers
 */
public final class LiseryPrivate extends JavaPlugin {
    
    private static LiseryPrivate instance;
    
    // Managers
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ChunkManager chunkManager;
    private IntegrationManager integrationManager;
    private WorldGuardIntegration worldGuardIntegration;
    private MenuManager menuManager;
    private WandManager wandManager;
    private HologramManager hologramManager;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;
        
        // Print logo in green
        LogoUtils.printGreenLogoSafe(this);
        
        getLogger().info("Starting LiseryPrivate...");
        
        try {
            // Initialize config manager
            configManager = new ConfigManager(this);
            configManager.loadAll();
            getLogger().info("Configuration loaded!");
            
            // Initialize WorldGuard integration (required)
            worldGuardIntegration = new WorldGuardIntegration(this);
            if (!worldGuardIntegration.initialize()) {
                getLogger().severe("WorldGuard is required! Disabling plugin...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().info("WorldGuard integration initialized!");
            
            // Initialize database
            databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                getLogger().severe("Failed to initialize database! Disabling plugin...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().info("Database initialized!");
            
            // Initialize other integrations
            integrationManager = new IntegrationManager(this);
            integrationManager.initialize();
            getLogger().info("Integrations initialized!");
            
            // Initialize chunk manager
            chunkManager = new ChunkManager(this);
            chunkManager.loadChunks();
            getLogger().info("Chunk manager initialized!");
            
            // Initialize menu manager
            menuManager = new MenuManager(this);
            
            // Initialize wand manager
            wandManager = new WandManager(this);
            wandManager.startOutlineTask();
            
            // Initialize hologram manager
            hologramManager = new HologramManager(this);
            hologramManager.start();
            
            // Register listeners
            registerListeners();
            
            // Register commands
            registerCommands();
            
            // Check for updates
            updateChecker = new UpdateChecker(this);
            updateChecker.checkForUpdates();
            
            getLogger().info("LiseryPrivate enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Error during plugin initialization: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Print logo in red
        LogoUtils.printRedLogoSafe(this);
        
        // Stop wand manager
        if (wandManager != null) {
            wandManager.stopOutlineTask();
        }
        
        // Stop hologram manager
        if (hologramManager != null) {
            hologramManager.stop();
        }
        
        // Close database
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("LiseryPrivate disabled.");
    }
    
    /**
     * Reload the plugin
     */
    public void reload() {
        // Reload configs
        configManager.reloadAll();
        
        // Reload chunks from database
        chunkManager.loadChunks();
        
        // Restart hologram manager
        hologramManager.stop();
        hologramManager.start();
        
        getLogger().info("LiseryPrivate reloaded!");
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkEnterListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
    }
    
    private void registerCommands() {
        ChunkCommand chunkCommand = new ChunkCommand(this);
        Objects.requireNonNull(getCommand("chunk")).setExecutor(chunkCommand);
        Objects.requireNonNull(getCommand("chunk")).setTabCompleter(chunkCommand);
    }
    
    // ==================== Getters ====================
    
    public static LiseryPrivate getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public ChunkManager getChunkManager() {
        return chunkManager;
    }
    
    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }
    
    public WorldGuardIntegration getWorldGuardIntegration() {
        return worldGuardIntegration;
    }
    
    public MenuManager getMenuManager() {
        return menuManager;
    }
    
    public WandManager getWandManager() {
        return wandManager;
    }
    
    public HologramManager getHologramManager() {
        return hologramManager;
    }
    
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}
