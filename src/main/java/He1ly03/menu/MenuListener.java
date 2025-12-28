package He1ly03.menu;

import He1ly03.LiseryPrivate;
import He1ly03.chunk.ChunkData;
import He1ly03.chunk.ChunkManager;
import He1ly03.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener for menu interactions
 */
public class MenuListener implements Listener {
    
    private final LiseryPrivate plugin;
    
    public MenuListener(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        MenuManager.MenuSession session = plugin.getMenuManager().getSession(player.getUniqueId());
        if (session == null) return;
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;
        
        String clickType = switch (event.getClick()) {
            case LEFT -> "LEFT";
            case RIGHT -> "RIGHT";
            case MIDDLE -> "MIDDLE";
            default -> "LEFT";
        };
        
        List<String> commands = plugin.getMenuManager().getItemCommands(session.getMenuName(), slot, clickType);
        
        // Store selected slot for dynamic items
        session.setSelectedSlot(slot);
        
        // Execute commands
        executeCommands(player, session, commands, slot);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        plugin.getMenuManager().removeSession(player.getUniqueId());
    }
    
    private void executeCommands(Player player, MenuManager.MenuSession session, List<String> commands, int slot) {
        String pendingAction = null;
        boolean needsConfirm = false;
        
        for (String command : commands) {
            command = command.trim();
            String commandLower = command.toLowerCase();
            
            // Handle confirm - both old and LiseryMenu format
            if (commandLower.equals("[confirm]")) {
                needsConfirm = true;
                continue;
            }
            
            // Handle open_menu (LiseryMenu format) and MENU (old format)
            if (commandLower.startsWith("[open_menu]")) {
                String menuName = command.substring("[open_menu]".length()).trim();
                player.closeInventory();
                plugin.getMenuManager().openMenu(player, menuName, 0, session.getContextChunk(), null);
                return;
            }
            if (command.startsWith("[MENU ")) {
                String menuName = command.substring(6, command.length() - 1).trim();
                player.closeInventory();
                plugin.getMenuManager().openMenu(player, menuName, 0, session.getContextChunk(), null);
                return;
            }
            
            // Handle pagination - both old and LiseryMenu format
            if (commandLower.equals("[next_page]")) {
                player.closeInventory();
                plugin.getMenuManager().openMenu(player, session.getMenuName(), session.getPage() + 1, 
                        session.getContextChunk(), null);
                return;
            }
            
            if (commandLower.equals("[prev_page]")) {
                player.closeInventory();
                plugin.getMenuManager().openMenu(player, session.getMenuName(), 
                        Math.max(0, session.getPage() - 1), session.getContextChunk(), null);
                return;
            }
            
            // Handle close - both old and LiseryMenu format
            if (commandLower.equals("[close]")) {
                player.closeInventory();
                return;
            }
            
            // Handle deny - both old and LiseryMenu format
            if (commandLower.equals("[deny]")) {
                player.closeInventory();
                if (session.getPendingAction() != null) {
                    plugin.getMenuManager().openMenu(player, "private");
                }
                return;
            }
            
            // Handle accept - both old and LiseryMenu format
            if (commandLower.equals("[accept]")) {
                String action = session.getPendingAction();
                if (action != null) {
                    executeAction(player, session, action);
                }
                player.closeInventory();
                return;
            }
            
            // Action commands (LiseryMenu format - lowercase)
            if (commandLower.startsWith("[chunk_") || commandLower.startsWith("[trust_") || 
                commandLower.startsWith("[settings_") || commandLower.startsWith("[auc_")) {
                // Normalize to uppercase for processing
                pendingAction = command.toUpperCase();
                
                // Get chunk from context based on slot
                ChunkData chunk = getChunkFromSlot(player, session, slot);
                if (chunk != null) {
                    session.setContextChunk(chunk);
                }
            }
        }
        
        // If needs confirmation, open confirm menu
        if (needsConfirm && pendingAction != null) {
            session.setPendingAction(pendingAction);
            player.closeInventory();
            plugin.getMenuManager().openMenu(player, "confirm", 0, session.getContextChunk(), pendingAction);
        } else if (pendingAction != null) {
            // Execute immediately
            executeAction(player, session, pendingAction);
            player.closeInventory();
        }
    }
    
    private ChunkData getChunkFromSlot(Player player, MenuManager.MenuSession session, int slot) {
        List<ChunkData> chunks;
        
        switch (session.getMenuName()) {
            case "private" -> chunks = plugin.getChunkManager().getPlayerChunks(player.getUniqueId());
            case "sell" -> chunks = plugin.getChunkManager().getChunksForSale();
            case "sell-list" -> chunks = plugin.getChunkManager().getPlayerChunks(player.getUniqueId())
                    .stream().filter(ChunkData::isForSale).toList();
            default -> {
                return session.getContextChunk();
            }
        }
        
        int itemsPerPage = 27; // Default slots 0-26
        int index = session.getPage() * itemsPerPage + slot;
        
        if (index >= 0 && index < chunks.size()) {
            return chunks.get(index);
        }
        
        return session.getContextChunk();
    }
    
    private void executeAction(Player player, MenuManager.MenuSession session, String action) {
        ChunkData chunk = session.getContextChunk();
        
        switch (action) {
            case "[CHUNK_PRIVATE]" -> {
                ChunkManager.ClaimResult result = plugin.getChunkManager().claimChunk(player, null);
                handleClaimResult(player, result);
            }
            
            case "[CHUNK_UNPRIVATE]" -> {
                if (chunk != null) {
                    ChunkManager.UnclaimResult result = plugin.getChunkManager().unclaimChunk(player, chunk);
                    handleUnclaimResult(player, result, chunk);
                }
            }
            
            case "[CHUNK_TELEPORT]" -> {
                if (chunk != null) {
                    Location loc = plugin.getChunkManager().getTeleportLocation(chunk);
                    if (loc != null) {
                        player.teleport(loc);
                        plugin.getWandManager().playTeleportEffects(player, loc);
                    }
                }
            }
            
            case "[TRUST_REMOVE]" -> {
                // This would need the trusted player UUID from context
                // For now, we'll skip the implementation detail
            }
            
            case "[AUC_BUY]" -> {
                if (!plugin.getConfigManager().isAuctionEnabled()) {
                    MessageUtils.sendMessage(player, "&cСистема аукциона отключена.");
                    return;
                }
                if (chunk != null && chunk.isForSale()) {
                    buyChunk(player, chunk);
                }
            }
            
            case "[AUC_TELEPORT]" -> {
                if (!plugin.getConfigManager().isAuctionEnabled()) {
                    MessageUtils.sendMessage(player, "&cСистема аукциона отключена.");
                    return;
                }
                if (chunk != null && chunk.getSaleLocation() != null) {
                    Location loc = parseSaleLocation(chunk.getSaleLocation());
                    if (loc != null) {
                        player.teleport(loc);
                        plugin.getWandManager().playTeleportEffects(player, loc);
                    }
                }
            }
            
            case "[AUC_REMOVE]" -> {
                if (!plugin.getConfigManager().isAuctionEnabled()) {
                    MessageUtils.sendMessage(player, "&cСистема аукциона отключена.");
                    return;
                }
                if (chunk != null && chunk.isOwner(player.getUniqueId())) {
                    chunk.removeFromSale();
                    plugin.getChunkManager().updateChunkSettings(chunk);
                    MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("chunk-removed-from-sale"));
                }
            }
            
            default -> {
                // Handle settings toggles
                if (action.startsWith("[SETTINGS_")) {
                    handleSettingsToggle(player, chunk, action);
                }
            }
        }
    }
    
    private void handleSettingsToggle(Player player, ChunkData chunk, String action) {
        if (chunk == null || !chunk.isOwner(player.getUniqueId())) return;
        
        String setting = action.substring(10, action.length() - 1); // Remove [SETTINGS_ and ]
        String[] parts = setting.split("_");
        if (parts.length != 2) return;
        
        String flagName = parts[0].toLowerCase();
        boolean value = parts[1].equalsIgnoreCase("ON");
        
        chunk.getSettings().setSetting(flagName, value);
        plugin.getChunkManager().updateChunkSettings(chunk);
        
        // Reopen settings menu to show updated state
        player.closeInventory();
        plugin.getMenuManager().openMenu(player, "settings", 0, chunk, null);
        
        // Send message
        String messageKey = flagName + "-" + (value ? "on" : "off");
        String message = plugin.getConfigManager().getMessage(messageKey);
        if (message != null && !message.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%chunk%", chunk.getName());
            MessageUtils.sendMessage(player, message, placeholders);
        }
    }
    
    private void handleClaimResult(Player player, ChunkManager.ClaimResult result) {
        String message = switch (result) {
            case SUCCESS -> {
                Map<String, String> placeholders = new HashMap<>();
                ChunkData chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
                placeholders.put("%chunk%", chunk != null ? chunk.getName() : "");
                placeholders.put("%price%", MessageUtils.formatMoney(plugin.getConfigManager().getChunkPrivatePrice()));
                yield MessageUtils.replacePlaceholders(
                        plugin.getConfigManager().getMessage("private-success"), placeholders);
            }
            case ALREADY_CLAIMED -> plugin.getConfigManager().getMessage("chunk-already-private");
            case WORLD_DISABLED -> plugin.getConfigManager().getMessage("world-disabled");
            case LIMIT_REACHED -> {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%limit%", String.valueOf(plugin.getIntegrationManager().getPlayerChunkLimit(player)));
                yield MessageUtils.replacePlaceholders(
                        plugin.getConfigManager().getMessage("limit-reached"), placeholders);
            }
            case NOT_ENOUGH_MONEY -> {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%price%", MessageUtils.formatMoney(plugin.getConfigManager().getChunkPrivatePrice()));
                yield MessageUtils.replacePlaceholders(
                        plugin.getConfigManager().getMessage("not-enough-money"), placeholders);
            }
            default -> "&cОшибка при привате чанка.";
        };
        
        MessageUtils.sendMessage(player, message);
    }
    
    private void handleUnclaimResult(Player player, ChunkManager.UnclaimResult result, ChunkData chunk) {
        String message = switch (result) {
            case SUCCESS -> {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%chunk%", chunk.getName());
                placeholders.put("%refund%", MessageUtils.formatMoney(plugin.getConfigManager().getChunkUnprivateRefund()));
                yield MessageUtils.replacePlaceholders(
                        plugin.getConfigManager().getMessage("unprivate-success"), placeholders);
            }
            case NOT_OWNER -> plugin.getConfigManager().getMessage("not-owner");
            case NOT_CLAIMED -> plugin.getConfigManager().getMessage("chunk-not-private");
        };
        
        MessageUtils.sendMessage(player, message);
    }
    
    private void buyChunk(Player player, ChunkData chunk) {
        double price = chunk.getSalePrice();
        
        if (!plugin.getIntegrationManager().hasBalance(player, price)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%price%", MessageUtils.formatMoney(price));
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("not-enough-money"), placeholders);
            return;
        }
        
        // Check limit (operators have unlimited chunks)
        int currentCount = plugin.getChunkManager().getPlayerChunkCount(player.getUniqueId());
        int limit = plugin.getIntegrationManager().getPlayerChunkLimit(player);
        // Check if limit is not Integer.MAX_VALUE (unlimited) before comparing
        if (limit != Integer.MAX_VALUE && currentCount >= limit) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%limit%", String.valueOf(limit));
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("limit-reached"), placeholders);
            return;
        }
        
        // Withdraw from buyer
        if (!plugin.getIntegrationManager().withdrawBalance(player, price)) {
            return;
        }
        
        // Deposit to seller
        Player seller = plugin.getServer().getPlayer(chunk.getOwnerUUID());
        if (seller != null) {
            plugin.getIntegrationManager().depositBalance(seller, price);
        }
        
        // Transfer ownership
        chunk.removeFromSale();
        plugin.getChunkManager().transferChunk(chunk, player);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%chunk%", chunk.getName());
        placeholders.put("%price%", MessageUtils.formatMoney(price));
        MessageUtils.sendMessage(player, "&aВы успешно купили чанк &e%chunk% &aза &e%price% &aмонет!", placeholders);
    }
    
    private Location parseSaleLocation(String locString) {
        if (locString == null || locString.isEmpty()) return null;
        
        String[] parts = locString.split(",");
        if (parts.length < 4) return null;
        
        try {
            return new Location(
                    plugin.getServer().getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])
            );
        } catch (Exception e) {
            return null;
        }
    }
}

