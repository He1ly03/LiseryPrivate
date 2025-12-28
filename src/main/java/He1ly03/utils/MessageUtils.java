package He1ly03.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;

/**
 * Utility class for sending messages to players
 */
public final class MessageUtils {
    
    private MessageUtils() {}
    
    /**
     * Send a chat message to a player
     */
    public static void sendMessage(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendMessage(ColorUtils.colorize(message));
    }
    
    /**
     * Send a chat message with placeholders
     */
    public static void sendMessage(Player player, String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return;
        message = replacePlaceholders(message, placeholders);
        player.sendMessage(ColorUtils.colorize(message));
    }
    
    /**
     * Send an action bar message
     */
    public static void sendActionBar(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendActionBar(ColorUtils.colorize(message));
    }
    
    /**
     * Send an action bar with placeholders
     */
    public static void sendActionBar(Player player, String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return;
        message = replacePlaceholders(message, placeholders);
        player.sendActionBar(ColorUtils.colorize(message));
    }
    
    /**
     * Send a title to a player
     */
    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 70, 20);
    }
    
    /**
     * Send a title with custom timings
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = (title != null && !title.isEmpty()) 
                ? ColorUtils.colorize(title) 
                : Component.empty();
        Component subtitleComponent = (subtitle != null && !subtitle.isEmpty()) 
                ? ColorUtils.colorize(subtitle) 
                : Component.empty();
        
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );
        
        player.showTitle(Title.title(titleComponent, subtitleComponent, times));
    }
    
    /**
     * Send a title with placeholders
     */
    public static void sendTitle(Player player, String title, String subtitle, Map<String, String> placeholders) {
        if (title != null) title = replacePlaceholders(title, placeholders);
        if (subtitle != null) subtitle = replacePlaceholders(subtitle, placeholders);
        sendTitle(player, title, subtitle);
    }
    
    /**
     * Replace placeholders in a string
     */
    public static String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null) return text;
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }
        
        return text;
    }
    
    /**
     * Format a number with thousands separator
     */
    public static String formatNumber(double number) {
        if (number == (long) number) {
            return String.format("%,d", (long) number);
        }
        return String.format("%,.2f", number);
    }
    
    /**
     * Format money amount
     */
    public static String formatMoney(double amount) {
        return formatNumber(amount);
    }
}

