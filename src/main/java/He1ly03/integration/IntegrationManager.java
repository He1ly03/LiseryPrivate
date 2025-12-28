package He1ly03.integration;

import He1ly03.LiseryPrivate;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

/**
 * Manages all plugin integrations
 */
public class IntegrationManager {
    
    private final LiseryPrivate plugin;
    
    // Economy
    private Economy vaultEconomy;
    private boolean vaultEnabled = false;
    private boolean coinsEngineEnabled = false;
    
    // LuckPerms
    private LuckPerms luckPerms;
    private boolean luckPermsEnabled = false;
    
    // PlaceholderAPI
    private boolean placeholderAPIEnabled = false;
    
    public IntegrationManager(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize all integrations
     */
    public void initialize() {
        setupVault();
        setupCoinsEngine();
        setupLuckPerms();
        setupPlaceholderAPI();
    }
    
    // ==================== Vault ====================
    
    private void setupVault() {
        if (!plugin.getConfigManager().isVaultEnabled()) {
            plugin.getLogger().info("Vault integration disabled in config.");
            return;
        }
        
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found. Economy features will be disabled.");
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().info("No economy plugin found. Economy features will be disabled.");
            return;
        }
        
        vaultEconomy = rsp.getProvider();
        vaultEnabled = true;
        plugin.getLogger().info("Vault economy integration enabled!");
    }
    
    // ==================== CoinsEngine ====================
    
    private void setupCoinsEngine() {
        if (!plugin.getConfigManager().isCoinsEngineEnabled()) {
            plugin.getLogger().info("CoinsEngine integration disabled in config.");
            return;
        }
        
        if (Bukkit.getPluginManager().getPlugin("CoinsEngine") == null) {
            plugin.getLogger().info("CoinsEngine not found.");
            return;
        }
        
        coinsEngineEnabled = true;
        plugin.getLogger().info("CoinsEngine integration enabled!");
    }
    
    // ==================== LuckPerms ====================
    
    private void setupLuckPerms() {
        if (!plugin.getConfigManager().isLuckPermsEnabled()) {
            plugin.getLogger().info("LuckPerms integration disabled in config.");
            return;
        }
        
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            plugin.getLogger().info("LuckPerms not found. Using default chunk limits.");
            return;
        }
        
        try {
            luckPerms = LuckPermsProvider.get();
            luckPermsEnabled = true;
            plugin.getLogger().info("LuckPerms integration enabled!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to hook into LuckPerms", e);
        }
    }
    
    // ==================== PlaceholderAPI ====================
    
    private void setupPlaceholderAPI() {
        if (!plugin.getConfigManager().isPlaceholderAPIEnabled()) {
            plugin.getLogger().info("PlaceholderAPI integration disabled in config.");
            return;
        }
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().info("PlaceholderAPI not found. Only built-in placeholders will work.");
            return;
        }
        
        placeholderAPIEnabled = true;
        // Register expansion
        new PlaceholderAPIExpansion(plugin).register();
        plugin.getLogger().info("PlaceholderAPI integration enabled!");
    }
    
    // ==================== Economy Methods ====================
    
    /**
     * Check if player has enough balance
     */
    public boolean hasBalance(Player player, double amount) {
        if (amount <= 0) return true;
        
        // Try CoinsEngine first if enabled
        if (coinsEngineEnabled) {
            return getCoinsEngineBalance(player) >= amount;
        }
        
        // Try Vault
        if (vaultEnabled && vaultEconomy != null) {
            return vaultEconomy.has(player, amount);
        }
        
        // No economy plugin - free
        return true;
    }
    
    /**
     * Withdraw from player balance
     */
    public boolean withdrawBalance(Player player, double amount) {
        if (amount <= 0) return true;
        
        // Try CoinsEngine first if enabled
        if (coinsEngineEnabled) {
            return withdrawCoinsEngine(player, amount);
        }
        
        // Try Vault
        if (vaultEnabled && vaultEconomy != null) {
            return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess();
        }
        
        // No economy plugin - free
        return true;
    }
    
    /**
     * Deposit to player balance
     */
    public boolean depositBalance(Player player, double amount) {
        if (amount <= 0) return true;
        
        // Try CoinsEngine first if enabled
        if (coinsEngineEnabled) {
            return depositCoinsEngine(player, amount);
        }
        
        // Try Vault
        if (vaultEnabled && vaultEconomy != null) {
            return vaultEconomy.depositPlayer(player, amount).transactionSuccess();
        }
        
        // No economy plugin
        return true;
    }
    
    /**
     * Get player balance
     */
    public double getBalance(Player player) {
        if (coinsEngineEnabled) {
            return getCoinsEngineBalance(player);
        }
        
        if (vaultEnabled && vaultEconomy != null) {
            return vaultEconomy.getBalance(player);
        }
        
        return 0;
    }
    
    // CoinsEngine methods using reflection to avoid hard dependency
    private double getCoinsEngineBalance(Player player) {
        try {
            Class<?> coinsEngineAPI = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
            Class<?> currencyClass = Class.forName("su.nightexpress.coinsengine.api.currency.Currency");
            
            String currencyName = plugin.getConfigManager().getCoinsEngineCurrency();
            
            Object currency = coinsEngineAPI.getMethod("getCurrency", String.class).invoke(null, currencyName);
            if (currency == null) return 0;
            
            Object result = coinsEngineAPI.getMethod("getBalance", Player.class, currencyClass)
                    .invoke(null, player, currency);
            return (double) result;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get CoinsEngine balance", e);
            return 0;
        }
    }
    
    private boolean withdrawCoinsEngine(Player player, double amount) {
        try {
            Class<?> coinsEngineAPI = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
            Class<?> currencyClass = Class.forName("su.nightexpress.coinsengine.api.currency.Currency");
            
            String currencyName = plugin.getConfigManager().getCoinsEngineCurrency();
            
            Object currency = coinsEngineAPI.getMethod("getCurrency", String.class).invoke(null, currencyName);
            if (currency == null) return false;
            
            coinsEngineAPI.getMethod("removeBalance", Player.class, currencyClass, double.class)
                    .invoke(null, player, currency, amount);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to withdraw from CoinsEngine", e);
            return false;
        }
    }
    
    private boolean depositCoinsEngine(Player player, double amount) {
        try {
            Class<?> coinsEngineAPI = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
            Class<?> currencyClass = Class.forName("su.nightexpress.coinsengine.api.currency.Currency");
            
            String currencyName = plugin.getConfigManager().getCoinsEngineCurrency();
            
            Object currency = coinsEngineAPI.getMethod("getCurrency", String.class).invoke(null, currencyName);
            if (currency == null) return false;
            
            coinsEngineAPI.getMethod("addBalance", Player.class, currencyClass, double.class)
                    .invoke(null, player, currency, amount);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to deposit to CoinsEngine", e);
            return false;
        }
    }
    
    // ==================== LuckPerms Methods ====================
    
    /**
     * Get player's chunk limit based on their group
     * Operators (OP) get unlimited chunks
     */
    public int getPlayerChunkLimit(Player player) {
        // Operators get unlimited chunks
        if (player.isOp()) {
            return Integer.MAX_VALUE;
        }
        
        int defaultLimit = plugin.getConfigManager().getDefaultLimit();
        
        if (!luckPermsEnabled || luckPerms == null) {
            return defaultLimit;
        }
        
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) return defaultLimit;
            
            String primaryGroup = user.getPrimaryGroup();
            int groupLimit = plugin.getConfigManager().getGroupLimit(primaryGroup);
            
            // Check all groups and return highest limit
            int maxLimit = groupLimit;
            for (var node : user.getNodes()) {
                if (node.getKey().startsWith("group.")) {
                    String group = node.getKey().substring(6);
                    int limit = plugin.getConfigManager().getGroupLimit(group);
                    maxLimit = Math.max(maxLimit, limit);
                }
            }
            
            return maxLimit;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get LuckPerms group", e);
            return defaultLimit;
        }
    }
    
    /**
     * Get player's primary group
     */
    public String getPlayerGroup(Player player) {
        if (!luckPermsEnabled || luckPerms == null) {
            return "default";
        }
        
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            return user != null ? user.getPrimaryGroup() : "default";
        } catch (Exception e) {
            return "default";
        }
    }
    
    // ==================== Getters ====================
    
    public boolean isVaultEnabled() {
        return vaultEnabled;
    }
    
    public boolean isCoinsEngineEnabled() {
        return coinsEngineEnabled;
    }
    
    public boolean isLuckPermsEnabled() {
        return luckPermsEnabled;
    }
    
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
    
    public boolean isEconomyEnabled() {
        return vaultEnabled || coinsEngineEnabled;
    }
}

