package He1ly03.command;

import He1ly03.LiseryPrivate;
import He1ly03.chunk.ChunkData;
import He1ly03.chunk.ChunkManager;
import He1ly03.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main command handler for /chunk
 */
public class ChunkCommand implements CommandExecutor, TabCompleter {
    
    private final LiseryPrivate plugin;
    
    public ChunkCommand(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "wand" -> handleWand(player);
            case "info" -> handleInfo(player);
            case "private" -> handlePrivate(player, args);
            case "unprivate" -> handleUnprivate(player, args);
            case "rename" -> handleRename(player, args);
            case "trust" -> handleTrust(player, args);
            case "untrust" -> handleUntrust(player, args);
            case "menu" -> handleMenu(player, args);
            case "settings" -> handleSettings(player, args);
            case "admin" -> handleAdmin(player, args);
            case "sell" -> handleSell(player, args);
            case "unsell" -> handleUnsell(player);
            case "auction" -> handleAuction(player);
            default -> sendHelp(player);
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        String cmd = plugin.getConfigManager().getMainCommand();
        MessageUtils.sendMessage(player, "&6=== &eLiseryPrivate &6===");
        MessageUtils.sendMessage(player, "&e/" + cmd + " wand &7- Получить редактор регионов");
        MessageUtils.sendMessage(player, "&e/" + cmd + " info &7- Информация о текущем чанке");
        MessageUtils.sendMessage(player, "&e/" + cmd + " private [название] &7- Приватить чанк");
        MessageUtils.sendMessage(player, "&e/" + cmd + " unprivate &7- Расприватить чанк");
        MessageUtils.sendMessage(player, "&e/" + cmd + " rename <старое> <новое> &7- Переименовать чанк");
        MessageUtils.sendMessage(player, "&e/" + cmd + " trust <игрок> &7- Добавить в доверенные");
        MessageUtils.sendMessage(player, "&e/" + cmd + " untrust <игрок> &7- Убрать из доверенных");
        MessageUtils.sendMessage(player, "&e/" + cmd + " menu &7- Открыть меню");
        MessageUtils.sendMessage(player, "&e/" + cmd + " auction &7- Аукцион чанков");
        MessageUtils.sendMessage(player, "&e/" + cmd + " settings <флаг> &7- Настройки чанка");
        if (player.hasPermission("liseryprivate.admin")) {
            MessageUtils.sendMessage(player, "&c/" + cmd + " admin reload &7- Перезагрузить плагин");
            MessageUtils.sendMessage(player, "&c/" + cmd + " admin forceunprivate &7- Принудительный расприват");
        }
    }
    
    private void handleWand(Player player) {
        if (!player.hasPermission("liseryprivate.wand")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        plugin.getWandManager().giveWand(player);
        MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("wand"));
    }
    
    private void handleInfo(Player player) {
        if (!player.hasPermission("liseryprivate.use")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
        
        if (chunk == null) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("chunk-not-private"));
            return;
        }
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%chunk%", chunk.getName());
        placeholders.put("%owner%", chunk.getOwnerName());
        
        // Build trust list
        StringBuilder trustList = new StringBuilder();
        Map<UUID, String> trusted = chunk.getTrustedPlayers();
        if (trusted.isEmpty()) {
            trustList.append(plugin.getConfigManager().getTrustFormatNoPlayers());
        } else {
            String format = plugin.getConfigManager().getTrustFormatPlayer();
            for (String name : trusted.values()) {
                trustList.append(format.replace("%player%", name));
            }
            // Remove trailing comma and space
            String result = trustList.toString();
            if (result.endsWith(", ")) {
                result = result.substring(0, result.length() - 2);
            }
            trustList = new StringBuilder(result);
        }
        placeholders.put("%trust%", trustList.toString());
        
        MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("info-1"), placeholders);
        MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("info-2"), placeholders);
        MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("info-3"), placeholders);
        MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("info-4"), placeholders);
    }
    
    private void handlePrivate(Player player, String[] args) {
        if (!player.hasPermission("liseryprivate.use")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        String customName = args.length > 1 ? args[1] : null;
        
        ChunkManager.ClaimResult result = plugin.getChunkManager().claimChunk(player, customName);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%price%", MessageUtils.formatMoney(plugin.getConfigManager().getChunkPrivatePrice()));
        
        switch (result) {
            case SUCCESS -> {
                ChunkData chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
                placeholders.put("%chunk%", chunk != null ? chunk.getName() : "");
                MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("private-success"), placeholders);
            }
            case ALREADY_CLAIMED -> MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("chunk-already-private"));
            case WORLD_DISABLED -> MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("world-disabled"));
            case LIMIT_REACHED -> {
                placeholders.put("%limit%", String.valueOf(plugin.getIntegrationManager().getPlayerChunkLimit(player)));
                MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("limit-reached"), placeholders);
            }
            case NOT_ENOUGH_MONEY -> MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("not-enough-money"), placeholders);
            case NAME_EXISTS -> MessageUtils.sendMessage(player, "&cЧанк с таким названием уже существует.");
            case NAME_TOO_LONG -> MessageUtils.sendMessage(player, "&cНазвание слишком длинное.");
            case TOO_CLOSE -> {
                Map<String, String> closePlaceholders = new HashMap<>();
                closePlaceholders.put("%distance%", String.valueOf(plugin.getConfigManager().getMinDistance()));
                MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("too-close"), closePlaceholders);
            }
            default -> MessageUtils.sendMessage(player, "&cОшибка при привате чанка.");
        }
    }
    
    private void handleUnprivate(Player player, String[] args) {
        if (!player.hasPermission("liseryprivate.use")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        ChunkData chunk;
        if (args.length > 1) {
            chunk = plugin.getChunkManager().getChunkByName(player.getUniqueId(), args[1]);
            if (chunk == null) {
                MessageUtils.sendMessage(player, "&cЧанк с таким названием не найден.");
                return;
            }
        } else {
            chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
        }
        
        if (chunk == null) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("chunk-not-private"));
            return;
        }
        
        ChunkManager.UnclaimResult result = plugin.getChunkManager().unclaimChunk(player, chunk);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%chunk%", chunk.getName());
        placeholders.put("%refund%", MessageUtils.formatMoney(plugin.getConfigManager().getChunkUnprivateRefund()));
        
        switch (result) {
            case SUCCESS -> MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("unprivate-success"), placeholders);
            case NOT_OWNER -> MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("not-owner"));
            case NOT_CLAIMED -> MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("chunk-not-private"));
        }
    }
    
    private void handleRename(Player player, String[] args) {
        if (!player.hasPermission("liseryprivate.use")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 3) {
            MessageUtils.sendMessage(player, "&cИспользование: /chunk rename <старое название> <новое название>");
            return;
        }
        
        String oldName = args[1];
        String newName = args[2];
        
        ChunkData chunk = plugin.getChunkManager().getChunkByName(player.getUniqueId(), oldName);
        if (chunk == null) {
            MessageUtils.sendMessage(player, "&cЧанк с названием &e" + oldName + " &cне найден.");
            return;
        }
        
        if (newName.length() > plugin.getConfigManager().getMaxNameLength()) {
            MessageUtils.sendMessage(player, "&cНовое название слишком длинное.");
            return;
        }
        
        if (plugin.getChunkManager().renameChunk(chunk, newName)) {
            MessageUtils.sendMessage(player, "&aЧанк переименован в &e" + newName);
        } else {
            MessageUtils.sendMessage(player, "&cЧанк с таким названием уже существует.");
        }
    }
    
    private void handleTrust(Player player, String[] args) {
        if (!player.hasPermission("liseryprivate.use")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, "&cИспользование: /chunk trust <игрок>");
            return;
        }
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
        if (chunk == null) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("chunk-not-private"));
            return;
        }
        
        if (!chunk.isOwner(player.getUniqueId())) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("not-owner"));
            return;
        }
        
        String targetName = args[1];
        
        // Check if trying to add self
        if (targetName.equalsIgnoreCase(player.getName())) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("trust-self"));
            return;
        }
        
        // Find target player
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            // Try offline player
            var offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (!offlinePlayer.hasPlayedBefore()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%player%", targetName);
                MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("trust-not-found"), placeholders);
                return;
            }
            
            if (chunk.isTrusted(offlinePlayer.getUniqueId())) {
                MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("trust-already"));
                return;
            }
            
            if (plugin.getChunkManager().addTrustedPlayer(chunk, offlinePlayer.getUniqueId(), offlinePlayer.getName())) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%player%", offlinePlayer.getName());
                MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("trust-added"), placeholders);
            }
        } else {
            if (chunk.isTrusted(target.getUniqueId())) {
                MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("trust-already"));
                return;
            }
            
            if (plugin.getChunkManager().addTrustedPlayer(chunk, target.getUniqueId(), target.getName())) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%player%", target.getName());
                MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("trust-added"), placeholders);
            }
        }
    }
    
    private void handleUntrust(Player player, String[] args) {
        if (!player.hasPermission("liseryprivate.use")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, "&cИспользование: /chunk untrust <игрок>");
            return;
        }
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
        if (chunk == null) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("chunk-not-private"));
            return;
        }
        
        if (!chunk.isOwner(player.getUniqueId())) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("not-owner"));
            return;
        }
        
        String targetName = args[1];
        
        // Find in trusted list
        UUID targetUUID = null;
        for (Map.Entry<UUID, String> entry : chunk.getTrustedPlayers().entrySet()) {
            if (entry.getValue().equalsIgnoreCase(targetName)) {
                targetUUID = entry.getKey();
                break;
            }
        }
        
        if (targetUUID == null) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("trust-not-in-list"));
            return;
        }
        
        if (plugin.getChunkManager().removeTrustedPlayer(chunk, targetUUID)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%player%", targetName);
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("trust-removed"), placeholders);
        }
    }
    
    private void handleMenu(Player player, String[] args) {
        if (!player.hasPermission("liseryprivate.use")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        String menuName = args.length > 1 ? args[1].toLowerCase() : "private";
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
        plugin.getMenuManager().openMenu(player, menuName, 0, chunk, null);
    }
    
    private void handleSettings(Player player, String[] args) {
        if (!player.hasPermission("liseryprivate.use")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
        if (chunk == null) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("chunk-not-private"));
            return;
        }
        
        if (!chunk.isOwner(player.getUniqueId()) && !player.hasPermission("liseryprivate.admin")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("not-owner"));
            return;
        }
        
        if (args.length < 2) {
            // Open settings menu
            plugin.getMenuManager().openMenu(player, "sell-list", 0, chunk, null);
            return;
        }
        
        String flagArg = args[1].toLowerCase();
        boolean value = args.length > 2 && (args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("true"));
        
        // Parse flag name and action
        String flagName;
        if (flagArg.endsWith("_on") || flagArg.endsWith("_off")) {
            flagName = flagArg.substring(0, flagArg.lastIndexOf('_'));
            value = flagArg.endsWith("_on");
        } else {
            flagName = flagArg;
        }
        
        chunk.getSettings().setSetting(flagName, value);
        plugin.getChunkManager().updateChunkSettings(chunk);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%chunk%", chunk.getName());
        
        String messageKey = flagName + "-" + (value ? "on" : "off");
        String message = plugin.getConfigManager().getMessage(messageKey);
        if (message != null && !message.isEmpty()) {
            MessageUtils.sendMessage(player, message, placeholders);
        } else {
            MessageUtils.sendMessage(player, "&aНастройка &e" + flagName + " &aизменена на &e" + (value ? "ВКЛ" : "ВЫКЛ"));
        }
    }
    
    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("liseryprivate.admin")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, "&cИспользование: /chunk admin <reload|forceunprivate>");
            return;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "reload" -> {
                plugin.reload();
                MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("reload-success"));
            }
            case "forceunprivate" -> {
                ChunkData chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
                if (chunk == null) {
                    MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("chunk-not-private"));
                    return;
                }
                
                plugin.getChunkManager().forceUnclaimChunk(chunk);
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%chunk%", chunk.getName());
                MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("force-unprivate"), placeholders);
            }
            default -> MessageUtils.sendMessage(player, "&cНеизвестная команда. Используйте: reload или forceunprivate");
        }
    }
    
    private void handleSell(Player player, String[] args) {
        if (!player.hasPermission("liseryprivate.use")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (!plugin.getConfigManager().isAuctionEnabled()) {
            MessageUtils.sendMessage(player, "&cСистема аукциона отключена.");
            return;
        }
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
        if (chunk == null) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("chunk-not-private"));
            return;
        }
        
        if (!chunk.isOwner(player.getUniqueId())) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("not-owner"));
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, "&cИспользование: /chunk sell <цена>");
            return;
        }
        
        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(player, "&cНеверная цена.");
            return;
        }
        
        double minPrice = plugin.getConfigManager().getMinChunkPrice();
        double maxPrice = plugin.getConfigManager().getMaxChunkPrice();
        
        if (price < minPrice || price > maxPrice) {
            MessageUtils.sendMessage(player, "&cЦена должна быть от &e" + MessageUtils.formatMoney(minPrice) + 
                    " &cдо &e" + MessageUtils.formatMoney(maxPrice));
            return;
        }
        
        Location loc = player.getLocation();
        String locString = loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
        
        chunk.putForSale(price, locString);
        plugin.getChunkManager().updateChunkSettings(chunk);
        
        MessageUtils.sendMessage(player, "&aЧанк &e" + chunk.getName() + " &aвыставлен на продажу за &e" + 
                MessageUtils.formatMoney(price) + " &aмонет.");
    }
    
    private void handleUnsell(Player player) {
        if (!player.hasPermission("liseryprivate.use")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (!plugin.getConfigManager().isAuctionEnabled()) {
            MessageUtils.sendMessage(player, "&cСистема аукциона отключена.");
            return;
        }
        
        ChunkData chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
        if (chunk == null) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("chunk-not-private"));
            return;
        }
        
        if (!chunk.isOwner(player.getUniqueId())) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("not-owner"));
            return;
        }
        
        if (!chunk.isForSale()) {
            MessageUtils.sendMessage(player, "&cЭтот чанк не выставлен на продажу.");
            return;
        }
        
        chunk.removeFromSale();
        plugin.getChunkManager().updateChunkSettings(chunk);
        
        MessageUtils.sendMessage(player, "&aЧанк &e" + chunk.getName() + " &aснят с продажи.");
    }
    
    private void handleAuction(Player player) {
        if (!player.hasPermission("liseryprivate.use")) {
            MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (!plugin.getConfigManager().isAuctionEnabled()) {
            MessageUtils.sendMessage(player, "&cСистема аукциона отключена.");
            return;
        }
        
        plugin.getMenuManager().openMenu(player, "sell");
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                                  @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList(
                    "wand", "info", "private", "unprivate", "rename", "trust", "untrust", 
                    "menu", "settings", "sell", "unsell", "auction"
            ));
            if (player.hasPermission("liseryprivate.admin")) {
                subCommands.add("admin");
            }
            
            String prefix = args[0].toLowerCase();
            completions = subCommands.stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            
            switch (subCommand) {
                case "unprivate", "rename" -> {
                    List<ChunkData> playerChunks = plugin.getChunkManager().getPlayerChunks(player.getUniqueId());
                    completions = playerChunks.stream()
                            .map(ChunkData::getName)
                            .filter(s -> s.toLowerCase().startsWith(prefix))
                            .collect(Collectors.toList());
                }
                case "trust" -> {
                    completions = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(s -> s.toLowerCase().startsWith(prefix))
                            .collect(Collectors.toList());
                }
                case "untrust" -> {
                    ChunkData chunk = plugin.getChunkManager().getChunkAt(player.getLocation());
                    if (chunk != null && chunk.isOwner(player.getUniqueId())) {
                        completions = chunk.getTrustedPlayers().values().stream()
                                .filter(s -> s.toLowerCase().startsWith(prefix))
                                .collect(Collectors.toList());
                    }
                }
                case "menu" -> {
                    completions = Arrays.asList("private", "trust", "sell", "sell-list").stream()
                            .filter(s -> s.startsWith(prefix))
                            .collect(Collectors.toList());
                }
                case "settings" -> {
                    completions = Arrays.asList("build", "destroy", "use", "switch", "mobs", "pvp", "fire", "explosion")
                            .stream()
                            .filter(s -> s.startsWith(prefix))
                            .collect(Collectors.toList());
                }
                case "admin" -> {
                    if (player.hasPermission("liseryprivate.admin")) {
                        completions = Arrays.asList("reload", "forceunprivate").stream()
                                .filter(s -> s.startsWith(prefix))
                                .collect(Collectors.toList());
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String prefix = args[2].toLowerCase();
            
            if (subCommand.equals("settings")) {
                completions = Arrays.asList("on", "off").stream()
                        .filter(s -> s.startsWith(prefix))
                        .collect(Collectors.toList());
            } else if (subCommand.equals("rename")) {
                // Suggest new name
                completions = Collections.emptyList();
            }
        }
        
        return completions;
    }
}

