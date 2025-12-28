package He1ly03.menu;

import He1ly03.LiseryPrivate;
import He1ly03.chunk.ChunkData;
import He1ly03.utils.ColorUtils;
import He1ly03.utils.MessageUtils;
import He1ly03.utils.SkullUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Manages all GUI menus
 */
public class MenuManager {
    
    private final LiseryPrivate plugin;
    
    // Store player menu state
    private final Map<UUID, MenuSession> playerSessions = new HashMap<>();
    
    public MenuManager(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Open a menu for a player
     */
    public void openMenu(Player player, String menuName) {
        openMenu(player, menuName, 0, null, null);
    }
    
    /**
     * Open a menu for a player with context
     */
    public void openMenu(Player player, String menuName, int page, ChunkData contextChunk, String pendingAction) {
        FileConfiguration menuConfig = plugin.getConfigManager().getMenuConfig(menuName);
        if (menuConfig == null) {
            MessageUtils.sendMessage(player, "&cМеню не найдено: " + menuName);
            return;
        }
        
        // Support both LiseryMenu format (title) and old format (menu_title)
        String title = menuConfig.getString("title", menuConfig.getString("menu_title", "&0Меню"));
        int size = menuConfig.getInt("size", 27);
        
        // Create inventory with MiniMessage support
        Inventory inventory = Bukkit.createInventory(null, size, ColorUtils.colorize(title));
        
        // Create session
        MenuSession session = new MenuSession(menuName, page, contextChunk, pendingAction);
        playerSessions.put(player.getUniqueId(), session);
        
        // Fill menu based on type
        switch (menuName.toLowerCase()) {
            case "private" -> fillPrivateMenu(player, inventory, menuConfig, page);
            case "trust" -> fillTrustMenu(player, inventory, menuConfig, page, contextChunk);
            case "sell" -> {
                if (!plugin.getConfigManager().isAuctionEnabled()) {
                    player.sendMessage(ColorUtils.colorize("&cСистема аукциона отключена."));
                    return;
                }
                fillSellMenu(player, inventory, menuConfig, page);
            }
            case "sell-list" -> {
                if (!plugin.getConfigManager().isAuctionEnabled()) {
                    player.sendMessage(ColorUtils.colorize("&cСистема аукциона отключена."));
                    return;
                }
                fillSellListMenu(player, inventory, menuConfig, page, contextChunk);
            }
            case "settings" -> fillSettingsMenu(player, inventory, menuConfig, contextChunk);
            case "confirm" -> fillConfirmMenu(player, inventory, menuConfig, pendingAction);
            default -> fillGenericMenu(player, inventory, menuConfig);
        }
        
        player.openInventory(inventory);
    }
    
    /**
     * Fill private chunks menu
     */
    private void fillPrivateMenu(Player player, Inventory inventory, FileConfiguration config, int page) {
        List<ChunkData> chunks = plugin.getChunkManager().getPlayerChunks(player.getUniqueId());
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;
        
        // Get template item
        ConfigurationSection regionTemplate = itemsSection.getConfigurationSection("region");
        List<Integer> slots = parseSlots(regionTemplate != null ? regionTemplate.getString("slots", "0-26") : "0-26");
        
        int itemsPerPage = slots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) chunks.size() / itemsPerPage));
        page = Math.min(page, totalPages - 1);
        
        int startIndex = page * itemsPerPage;
        
        // Fill chunk items
        for (int i = 0; i < slots.size() && startIndex + i < chunks.size(); i++) {
            ChunkData chunk = chunks.get(startIndex + i);
            ItemStack item = createChunkItem(regionTemplate, chunk, player);
            inventory.setItem(slots.get(i), item);
        }
        
        // Fill static items
        fillStaticItems(player, inventory, itemsSection, page, totalPages, null);
    }
    
    /**
     * Fill trust menu
     */
    private void fillTrustMenu(Player player, Inventory inventory, FileConfiguration config, int page, ChunkData chunk) {
        if (chunk == null) {
            chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
        }
        
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;
        
        if (chunk != null && chunk.isOwner(player.getUniqueId())) {
            Map<UUID, String> trusted = chunk.getTrustedPlayers();
            ConfigurationSection trustTemplate = itemsSection.getConfigurationSection("trust");
            List<Integer> slots = parseSlots(trustTemplate != null ? trustTemplate.getString("slots", "0-26") : "0-26");
            
            int itemsPerPage = slots.size();
            List<Map.Entry<UUID, String>> trustedList = new ArrayList<>(trusted.entrySet());
            int totalPages = Math.max(1, (int) Math.ceil((double) trustedList.size() / itemsPerPage));
            page = Math.min(page, totalPages - 1);
            
            int startIndex = page * itemsPerPage;
            
            for (int i = 0; i < slots.size() && startIndex + i < trustedList.size(); i++) {
                Map.Entry<UUID, String> entry = trustedList.get(startIndex + i);
                ItemStack item = createTrustedPlayerItem(trustTemplate, entry.getValue(), chunk);
                inventory.setItem(slots.get(i), item);
            }
            
            fillStaticItems(player, inventory, itemsSection, page, totalPages, chunk);
        } else {
            fillStaticItems(player, inventory, itemsSection, 0, 1, chunk);
        }
    }
    
    /**
     * Fill sell/auction menu
     */
    private void fillSellMenu(Player player, Inventory inventory, FileConfiguration config, int page) {
        List<ChunkData> forSale = plugin.getChunkManager().getChunksForSale();
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;
        
        ConfigurationSection sellTemplate = itemsSection.getConfigurationSection("sell");
        List<Integer> slots = parseSlots(sellTemplate != null ? sellTemplate.getString("slots", "0-26") : "0-26");
        
        int itemsPerPage = slots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) forSale.size() / itemsPerPage));
        page = Math.min(page, totalPages - 1);
        
        int startIndex = page * itemsPerPage;
        
        for (int i = 0; i < slots.size() && startIndex + i < forSale.size(); i++) {
            ChunkData chunk = forSale.get(startIndex + i);
            ItemStack item = createSaleItem(sellTemplate, chunk, player);
            inventory.setItem(slots.get(i), item);
        }
        
        fillStaticItems(player, inventory, itemsSection, page, totalPages, null);
    }
    
    /**
     * Fill player's sell list menu
     */
    private void fillSellListMenu(Player player, Inventory inventory, FileConfiguration config, int page, ChunkData chunk) {
        List<ChunkData> playerChunksForSale = plugin.getChunkManager().getPlayerChunks(player.getUniqueId())
                .stream()
                .filter(ChunkData::isForSale)
                .toList();
        
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;
        
        ConfigurationSection sellListTemplate = itemsSection.getConfigurationSection("sell-list");
        List<Integer> slots = parseSlots(sellListTemplate != null ? sellListTemplate.getString("slots", "0-26") : "0-26");
        
        int itemsPerPage = slots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) playerChunksForSale.size() / itemsPerPage));
        page = Math.min(page, totalPages - 1);
        
        int startIndex = page * itemsPerPage;
        
        for (int i = 0; i < slots.size() && startIndex + i < playerChunksForSale.size(); i++) {
            ChunkData chunkData = playerChunksForSale.get(startIndex + i);
            ItemStack item = createSaleItem(sellListTemplate, chunkData, player);
            inventory.setItem(slots.get(i), item);
        }
        
        fillStaticItems(player, inventory, itemsSection, page, totalPages, chunk);
    }
    
    /**
     * Fill settings menu
     */
    private void fillSettingsMenu(Player player, Inventory inventory, FileConfiguration config, ChunkData chunk) {
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;
        
        if (chunk == null) {
            chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
        }
        
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;
            
            int slot = getSlotFromSection(itemSection);
            if (slot < 0) continue;
            
            ItemStack item = createMenuItem(itemSection, player, chunk);
            inventory.setItem(slot, item);
        }
    }
    
    /**
     * Fill confirm menu
     */
    private void fillConfirmMenu(Player player, Inventory inventory, FileConfiguration config, String pendingAction) {
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;
        
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;
            
            int slot = getSlotFromSection(itemSection);
            if (slot < 0) continue;
            
            ItemStack item = createMenuItem(itemSection, player, null);
            inventory.setItem(slot, item);
        }
    }
    
    /**
     * Fill generic menu
     */
    private void fillGenericMenu(Player player, Inventory inventory, FileConfiguration config) {
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;
        
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;
            
            int slot = getSlotFromSection(itemSection);
            if (slot < 0) continue;
            
            ItemStack item = createMenuItem(itemSection, player, null);
            inventory.setItem(slot, item);
        }
    }
    
    /**
     * Fill static menu items (navigation, etc.)
     */
    private void fillStaticItems(Player player, Inventory inventory, ConfigurationSection itemsSection, 
                                  int page, int totalPages, ChunkData contextChunk) {
        for (String key : itemsSection.getKeys(false)) {
            if (key.equals("region") || key.equals("trust") || key.equals("sell") || key.equals("sell-list")) continue;
            
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;
            
            int slot = getSlotFromSection(itemSection);
            if (slot < 0) continue;
            
            // Check pagination visibility - support both old and new format
            List<String> commands = getActionsFromSection(itemSection, "LEFT");
            boolean hasNextPage = commands.stream().anyMatch(c -> c.equalsIgnoreCase("[next_page]") || c.equalsIgnoreCase("[NEXT_PAGE]"));
            boolean hasPrevPage = commands.stream().anyMatch(c -> c.equalsIgnoreCase("[prev_page]") || c.equalsIgnoreCase("[PREV_PAGE]"));
            
            if (hasNextPage && page >= totalPages - 1) continue;
            if (hasPrevPage && page <= 0) continue;
            
            ItemStack item = createMenuItem(itemSection, player, contextChunk);
            inventory.setItem(slot, item);
        }
    }
    
    /**
     * Get slot from section - supports both int and string format
     */
    private int getSlotFromSection(ConfigurationSection section) {
        // Try string format first (LiseryMenu)
        String slotStr = section.getString("slot", null);
        if (slotStr != null) {
            try {
                return Integer.parseInt(slotStr.trim());
            } catch (NumberFormatException ignored) {}
        }
        // Fall back to int format
        return section.getInt("slot", -1);
    }
    
    /**
     * Create chunk item for private menu
     */
    private ItemStack createChunkItem(ConfigurationSection template, ChunkData chunk, Player player) {
        if (template == null) {
            return new ItemStack(Material.GRASS_BLOCK);
        }
        
        Material material = Material.matchMaterial(template.getString("material", "GRASS_BLOCK"));
        if (material == null) material = Material.GRASS_BLOCK;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        Map<String, String> placeholders = createChunkPlaceholders(chunk, player);
        
        String displayName = template.getString("display_name", "&fЧанк");
        // Remove italic decoration
        meta.displayName(ColorUtils.colorize(MessageUtils.replacePlaceholders(displayName, placeholders))
                .decoration(TextDecoration.ITALIC, false));
        
        List<String> lore = template.getStringList("lore");
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(ColorUtils.colorize(MessageUtils.replacePlaceholders(line, placeholders))
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(loreComponents);
        
        // Add flags
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        
        // Handle base64 texture for player heads - AFTER setting display name and lore
        if (material == Material.PLAYER_HEAD && meta instanceof SkullMeta) {
            // First save all meta (display name, lore, flags)
            item.setItemMeta(meta);
            
            String base64Texture = template.getString("base64");
            if (base64Texture != null && !base64Texture.isEmpty()) {
                base64Texture = MessageUtils.replacePlaceholders(base64Texture, placeholders);
                // applyBase64Texture preserves display name, lore, and flags
                SkullUtils.applyBase64Texture(item, base64Texture);
            }
        } else {
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create trusted player item
     */
    private ItemStack createTrustedPlayerItem(ConfigurationSection template, String playerName, ChunkData chunk) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", playerName);
        placeholders.put("%player_name%", playerName);
        if (chunk != null) {
            placeholders.put("%chunk%", chunk.getName());
            placeholders.put("%owner%", chunk.getOwnerName());
        }
        
        // Set display name and lore first
        if (template != null) {
            String displayName = template.getString("display_name", "&f%player%");
            meta.displayName(ColorUtils.colorize(MessageUtils.replacePlaceholders(displayName, placeholders))
                    .decoration(TextDecoration.ITALIC, false));
            
            List<String> lore = template.getStringList("lore");
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(ColorUtils.colorize(MessageUtils.replacePlaceholders(line, placeholders))
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(loreComponents);
        }
        
        // Add flags
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        
        // Check for base64 texture in template
        if (template != null) {
            String base64Texture = template.getString("base64");
            if (base64Texture != null && !base64Texture.isEmpty()) {
                base64Texture = MessageUtils.replacePlaceholders(base64Texture, placeholders);
                // Save meta first, then apply base64
                item.setItemMeta(meta);
                SkullUtils.applyBase64Texture(item, base64Texture);
                return item;
            }
        }
        
        // Set skull owner using offline player (for trusted players)
        // Try to get online player first for better texture support
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            meta.setOwningPlayer(onlinePlayer);
        } else {
            // Use offline player
            var offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            meta.setOwningPlayer(offlinePlayer);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create sale item
     */
    private ItemStack createSaleItem(ConfigurationSection template, ChunkData chunk, Player player) {
        if (template == null) {
            return new ItemStack(Material.PAPER);
        }
        
        Material material = Material.matchMaterial(template.getString("material", "PAPER"));
        if (material == null) material = Material.PAPER;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        Map<String, String> placeholders = createChunkPlaceholders(chunk, player);
        placeholders.put("%price%", MessageUtils.formatMoney(chunk.getSalePrice()));
        
        String displayName = template.getString("display_name", "&fЧанк");
        meta.displayName(ColorUtils.colorize(MessageUtils.replacePlaceholders(displayName, placeholders))
                .decoration(TextDecoration.ITALIC, false));
        
        List<String> lore = template.getStringList("lore");
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(ColorUtils.colorize(MessageUtils.replacePlaceholders(line, placeholders))
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(loreComponents);
        
        // Add flags
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        
        // Handle base64 texture for player heads - AFTER setting display name and lore
        if (material == Material.PLAYER_HEAD && meta instanceof SkullMeta) {
            // First save all meta (display name, lore, flags)
            item.setItemMeta(meta);
            
            String base64Texture = template.getString("base64");
            if (base64Texture != null && !base64Texture.isEmpty()) {
                base64Texture = MessageUtils.replacePlaceholders(base64Texture, placeholders);
                // applyBase64Texture preserves display name, lore, and flags
                SkullUtils.applyBase64Texture(item, base64Texture);
            }
        } else {
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create menu item
     */
    private ItemStack createMenuItem(ConfigurationSection section, Player player, ChunkData chunk) {
        String materialName = section.getString("material", "STONE");
        Material material;
        
        // Handle player head with old format (head-<player>)
        if (materialName.startsWith("head-")) {
            material = Material.PLAYER_HEAD;
        } else {
            material = Material.matchMaterial(materialName);
            if (material == null) material = Material.STONE;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", player.getName());
        placeholders.put("%player_name%", player.getName());
        if (chunk != null) {
            placeholders.putAll(createChunkPlaceholders(chunk, player));
        }
        
        String displayName = section.getString("display_name", "");
        if (!displayName.isEmpty()) {
            meta.displayName(ColorUtils.colorize(MessageUtils.replacePlaceholders(displayName, placeholders))
                    .decoration(TextDecoration.ITALIC, false));
        }
        
        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(ColorUtils.colorize(MessageUtils.replacePlaceholders(line, placeholders))
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(loreComponents);
        }
        
        // Add flags before handling skull
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        
        // Handle skull - IMPORTANT: Apply texture AFTER setting display name and lore
        if (material == Material.PLAYER_HEAD && meta instanceof SkullMeta) {
            // First, save display name, lore, and flags to item
            item.setItemMeta(meta);
            
            // Check for base64 texture first
            String base64Texture = section.getString("base64");
            if (base64Texture != null && !base64Texture.isEmpty()) {
                base64Texture = MessageUtils.replacePlaceholders(base64Texture, placeholders);
                SkullUtils.applyBase64Texture(item, base64Texture);
            } 
            // Check for skull_owner (LiseryMenu format)
            else if (section.contains("skull_owner")) {
                String skullOwner = section.getString("skull_owner", "");
                skullOwner = MessageUtils.replacePlaceholders(skullOwner, placeholders);
                if (!skullOwner.isEmpty()) {
                    SkullMeta currentMeta = (SkullMeta) item.getItemMeta();
                    if (currentMeta != null) {
                        Player onlinePlayer = Bukkit.getPlayer(skullOwner);
                        if (onlinePlayer != null) {
                            currentMeta.setOwningPlayer(onlinePlayer);
                        } else {
                            currentMeta.setOwningPlayer(Bukkit.getOfflinePlayer(skullOwner));
                        }
                        item.setItemMeta(currentMeta);
                    }
                }
            }
            // Check for old format (head-<player>)
            else if (materialName.startsWith("head-")) {
                String targetPlayer = materialName.substring(5);
                targetPlayer = MessageUtils.replacePlaceholders(targetPlayer, placeholders);
                SkullMeta currentMeta = (SkullMeta) item.getItemMeta();
                if (currentMeta != null) {
                    currentMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetPlayer));
                    item.setItemMeta(currentMeta);
                }
            }
        } else {
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create placeholders for chunk
     */
    private Map<String, String> createChunkPlaceholders(ChunkData chunk, Player player) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%chunk%", chunk.getName());
        placeholders.put("%owner%", chunk.getOwnerName());
        placeholders.put("%player%", player.getName());
        
        boolean pvp = chunk.getSettings().isPvpAllowed();
        placeholders.put("%pvp%", pvp ? plugin.getConfigManager().getPvPFormatTrue() : 
                plugin.getConfigManager().getPvPFormatFalse());
        
        // Setting toggles
        placeholders.put("%build_toggle%", formatToggle(chunk.getSettings().isBuildAllowed()));
        placeholders.put("%destroy_toggle%", formatToggle(chunk.getSettings().isDestroyAllowed()));
        placeholders.put("%use_toggle%", formatToggle(chunk.getSettings().isUseAllowed()));
        placeholders.put("%switch_toggle%", formatToggle(chunk.getSettings().isSwitchAllowed()));
        placeholders.put("%mobs_toggle%", formatToggle(chunk.getSettings().isMobsAllowed()));
        placeholders.put("%pvp_toggle%", formatToggle(chunk.getSettings().isPvpAllowed()));
        placeholders.put("%fire_toggle%", formatToggle(chunk.getSettings().isFireAllowed()));
        placeholders.put("%explosion_toggle%", formatToggle(chunk.getSettings().isExplosionAllowed()));
        
        return placeholders;
    }
    
    private String formatToggle(boolean value) {
        return value ? plugin.getConfigManager().getSettingsFormatTrue() : 
                plugin.getConfigManager().getSettingsFormatFalse();
    }
    
    /**
     * Parse slot range (e.g., "0-26" or "10,11,12")
     */
    private List<Integer> parseSlots(String slotsString) {
        List<Integer> slots = new ArrayList<>();
        
        if (slotsString.contains("-")) {
            String[] parts = slotsString.split("-");
            int start = Integer.parseInt(parts[0].trim());
            int end = Integer.parseInt(parts[1].trim());
            for (int i = start; i <= end; i++) {
                slots.add(i);
            }
        } else if (slotsString.contains(",")) {
            for (String part : slotsString.split(",")) {
                slots.add(Integer.parseInt(part.trim()));
            }
        } else {
            slots.add(Integer.parseInt(slotsString.trim()));
        }
        
        return slots;
    }
    
    /**
     * Get player session
     */
    public MenuSession getSession(UUID playerUUID) {
        return playerSessions.get(playerUUID);
    }
    
    /**
     * Remove player session
     */
    public void removeSession(UUID playerUUID) {
        playerSessions.remove(playerUUID);
    }
    
    /**
     * Get commands/actions for an item - supports both old and LiseryMenu format
     */
    public List<String> getItemCommands(String menuName, int slot, String clickType) {
        FileConfiguration config = plugin.getConfigManager().getMenuConfig(menuName);
        if (config == null) return Collections.emptyList();
        
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return Collections.emptyList();
        
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection item = itemsSection.getConfigurationSection(key);
            if (item == null) continue;
            
            // Check if this item is in this slot - support both int and string format
            int itemSlot = getSlotFromSection(item);
            String slotsStr = item.getString("slots", "");
            
            boolean matchesSlot = itemSlot == slot;
            if (!matchesSlot && !slotsStr.isEmpty()) {
                matchesSlot = parseSlots(slotsStr).contains(slot);
            }
            
            if (matchesSlot) {
                return getActionsFromSection(item, clickType);
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Get actions from section - supports both old format (click_commands) and LiseryMenu format (left_click_actions)
     */
    private List<String> getActionsFromSection(ConfigurationSection section, String clickType) {
        // LiseryMenu format keys
        String liseryKey = switch (clickType) {
            case "LEFT" -> "left_click_actions";
            case "RIGHT" -> "right_click_actions";
            case "MIDDLE" -> "middle_click_actions";
            case "SHIFT_LEFT" -> "shift_left_click_actions";
            default -> "left_click_actions";
        };
        
        // Old format keys
        String oldKey = switch (clickType) {
            case "LEFT" -> "left_click_commands";
            case "RIGHT" -> "right_click_commands";
            case "MIDDLE" -> "middle_click_commands";
            default -> "click_commands";
        };
        
        // Try LiseryMenu format first
        List<String> actions = section.getStringList(liseryKey);
        if (!actions.isEmpty()) {
            return actions;
        }
        
        // Try old format
        actions = section.getStringList(oldKey);
        if (!actions.isEmpty()) {
            return actions;
        }
        
        // Fallback to click_commands
        return section.getStringList("click_commands");
    }
    
    /**
     * Menu session data
     */
    public static class MenuSession {
        private final String menuName;
        private int page;
        private ChunkData contextChunk;
        private String pendingAction;
        private int selectedSlot;
        
        public MenuSession(String menuName, int page, ChunkData contextChunk, String pendingAction) {
            this.menuName = menuName;
            this.page = page;
            this.contextChunk = contextChunk;
            this.pendingAction = pendingAction;
            this.selectedSlot = -1;
        }
        
        public String getMenuName() { return menuName; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public ChunkData getContextChunk() { return contextChunk; }
        public void setContextChunk(ChunkData chunk) { this.contextChunk = chunk; }
        public String getPendingAction() { return pendingAction; }
        public void setPendingAction(String action) { this.pendingAction = action; }
        public int getSelectedSlot() { return selectedSlot; }
        public void setSelectedSlot(int slot) { this.selectedSlot = slot; }
    }
}
