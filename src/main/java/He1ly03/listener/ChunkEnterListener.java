package He1ly03.listener;

import He1ly03.LiseryPrivate;
import He1ly03.chunk.ChunkData;
import He1ly03.utils.MessageUtils;
import org.bukkit.Chunk;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for chunk enter/exit events and effects
 */
public class ChunkEnterListener implements Listener {
    
    private final LiseryPrivate plugin;
    
    public ChunkEnterListener(LiseryPrivate plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only process if chunk changed
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        
        handleChunkChange(event.getPlayer(), event.getFrom().getChunk(), event.getTo().getChunk());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        
        handleChunkChange(event.getPlayer(), event.getFrom().getChunk(), event.getTo().getChunk());
    }
    
    private void handleChunkChange(Player player, Chunk fromChunk, Chunk toChunk) {
        ChunkData fromData = plugin.getChunkManager().getChunkAt(fromChunk);
        ChunkData toData = plugin.getChunkManager().getChunkAt(toChunk);
        
        // Leaving a claimed chunk
        if (fromData != null && toData == null) {
            sendExitEffects(player, fromData);
        }
        
        // Entering a claimed chunk
        if (toData != null && (fromData == null || !fromData.equals(toData))) {
            sendEnterEffects(player, toData);
        }
        
        // Moving between different claimed chunks
        if (fromData != null && toData != null && !fromData.equals(toData)) {
            sendExitEffects(player, fromData);
            sendEnterEffects(player, toData);
        }
    }
    
    private void sendEnterEffects(Player player, ChunkData chunk) {
        Map<String, String> placeholders = createPlaceholders(player, chunk);
        
        // Title
        String title = plugin.getConfigManager().getEnterTitle();
        String subtitle = plugin.getConfigManager().getEnterSubtitle();
        if ((title != null && !title.isEmpty()) || (subtitle != null && !subtitle.isEmpty())) {
            MessageUtils.sendTitle(player, 
                    MessageUtils.replacePlaceholders(title, placeholders),
                    MessageUtils.replacePlaceholders(subtitle, placeholders));
        }
        
        // Chat
        String chat = plugin.getConfigManager().getEnterChat();
        if (chat != null && !chat.isEmpty()) {
            MessageUtils.sendMessage(player, MessageUtils.replacePlaceholders(chat, placeholders));
        }
        
        // Actionbar
        String actionbar = plugin.getConfigManager().getEnterActionbar();
        if (actionbar != null && !actionbar.isEmpty()) {
            MessageUtils.sendActionBar(player, MessageUtils.replacePlaceholders(actionbar, placeholders));
        }
        
        // Sound
        Sound sound = plugin.getConfigManager().getEnterSound();
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 
                    plugin.getConfigManager().getEnterSoundVolume(),
                    plugin.getConfigManager().getEnterSoundPitch());
        }
    }
    
    private void sendExitEffects(Player player, ChunkData chunk) {
        Map<String, String> placeholders = createPlaceholders(player, chunk);
        
        // Title
        String title = plugin.getConfigManager().getExitTitle();
        String subtitle = plugin.getConfigManager().getExitSubtitle();
        if ((title != null && !title.isEmpty()) || (subtitle != null && !subtitle.isEmpty())) {
            MessageUtils.sendTitle(player,
                    MessageUtils.replacePlaceholders(title, placeholders),
                    MessageUtils.replacePlaceholders(subtitle, placeholders));
        }
        
        // Chat
        String chat = plugin.getConfigManager().getExitChat();
        if (chat != null && !chat.isEmpty()) {
            MessageUtils.sendMessage(player, MessageUtils.replacePlaceholders(chat, placeholders));
        }
        
        // Actionbar
        String actionbar = plugin.getConfigManager().getExitActionbar();
        if (actionbar != null && !actionbar.isEmpty()) {
            MessageUtils.sendActionBar(player, MessageUtils.replacePlaceholders(actionbar, placeholders));
        }
        
        // Sound
        Sound sound = plugin.getConfigManager().getExitSound();
        if (sound != null) {
            player.playSound(player.getLocation(), sound,
                    plugin.getConfigManager().getExitSoundVolume(),
                    plugin.getConfigManager().getExitSoundPitch());
        }
    }
    
    private Map<String, String> createPlaceholders(Player player, ChunkData chunk) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", player.getName());
        placeholders.put("%owner%", chunk.getOwnerName());
        placeholders.put("%chunk%", chunk.getName());
        
        boolean pvp = chunk.getSettings().isPvpAllowed();
        String pvpFormat = pvp ? plugin.getConfigManager().getPvPFormatTrue() : 
                plugin.getConfigManager().getPvPFormatFalse();
        placeholders.put("%pvp%", pvpFormat);
        
        return placeholders;
    }
}

