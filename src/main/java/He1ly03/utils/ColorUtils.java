package He1ly03.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for color processing
 * Supports: Legacy colors (&), HEX colors (&#RRGGBB), MiniMessage tags
 */
public final class ColorUtils {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("&x(&[A-Fa-f0-9]){6}");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    
    private ColorUtils() {}
    
    /**
     * Converts a string with color codes to a Component
     * Supports: Legacy (&), HEX (&#RRGGBB), MiniMessage (<color>)
     */
    public static Component colorize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // Check if text contains MiniMessage tags
        if (containsMiniMessage(text)) {
            // Convert legacy colors to MiniMessage format first
            text = convertLegacyToMiniMessage(text);
            return MINI_MESSAGE.deserialize(text);
        }
        
        // Process HEX colors (&#RRGGBB -> &x&R&R&G&G&B&B)
        text = processHexColors(text);
        
        // Use legacy serializer
        return LEGACY_SERIALIZER.deserialize(text);
    }
    
    /**
     * Converts legacy color codes to MiniMessage format
     */
    private static String convertLegacyToMiniMessage(String text) {
        // Convert &#RRGGBB to <#RRGGBB>
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(result, "<#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(result);
        text = result.toString();
        
        // Convert legacy codes to MiniMessage
        text = text.replace("&0", "<black>")
                   .replace("&1", "<dark_blue>")
                   .replace("&2", "<dark_green>")
                   .replace("&3", "<dark_aqua>")
                   .replace("&4", "<dark_red>")
                   .replace("&5", "<dark_purple>")
                   .replace("&6", "<gold>")
                   .replace("&7", "<gray>")
                   .replace("&8", "<dark_gray>")
                   .replace("&9", "<blue>")
                   .replace("&a", "<green>")
                   .replace("&b", "<aqua>")
                   .replace("&c", "<red>")
                   .replace("&d", "<light_purple>")
                   .replace("&e", "<yellow>")
                   .replace("&f", "<white>")
                   .replace("&l", "<bold>")
                   .replace("&m", "<strikethrough>")
                   .replace("&n", "<underlined>")
                   .replace("&o", "<italic>")
                   .replace("&r", "<reset>");
        
        return text;
    }
    
    /**
     * Processes HEX colors in format &#RRGGBB to &x&R&R&G&G&B&B
     */
    private static String processHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                replacement.append("&").append(c);
            }
            matcher.appendReplacement(result, replacement.toString());
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Checks if text contains MiniMessage tags
     */
    private static boolean containsMiniMessage(String text) {
        return text.contains("<") && text.contains(">") && 
               (text.contains("<color") || text.contains("<gradient") || 
                text.contains("<rainbow") || text.contains("<hover") ||
                text.contains("<click") || text.contains("<gold>") ||
                text.contains("<red>") || text.contains("<green>") ||
                text.contains("<blue>") || text.contains("<white>") ||
                text.contains("<black>") || text.contains("<yellow>") ||
                text.contains("<aqua>") || text.contains("<gray>") ||
                text.contains("<bold>") || text.contains("<italic>") ||
                text.contains("<underlined>") || text.contains("<strikethrough>"));
    }
    
    /**
     * Strip all color codes from text
     */
    public static String stripColors(String text) {
        if (text == null) return null;
        
        // Remove MiniMessage tags
        text = text.replaceAll("<[^>]+>", "");
        
        // Remove HEX colors
        text = HEX_PATTERN.matcher(text).replaceAll("");
        text = LEGACY_HEX_PATTERN.matcher(text).replaceAll("");
        
        // Remove legacy colors
        text = text.replaceAll("&[0-9a-fk-or]", "");
        
        return text;
    }
    
    /**
     * Parse RGB color from string format "R:G:B"
     */
    public static int[] parseRGB(String colorString) {
        if (colorString == null || !colorString.contains(":")) {
            return new int[]{255, 255, 255};
        }
        
        String[] parts = colorString.split(":");
        if (parts.length != 3) {
            return new int[]{255, 255, 255};
        }
        
        try {
            int r = Math.min(255, Math.max(0, Integer.parseInt(parts[0].trim())));
            int g = Math.min(255, Math.max(0, Integer.parseInt(parts[1].trim())));
            int b = Math.min(255, Math.max(0, Integer.parseInt(parts[2].trim())));
            return new int[]{r, g, b};
        } catch (NumberFormatException e) {
            return new int[]{255, 255, 255};
        }
    }
    
    /**
     * Parse ARGB color from hex string format "#AARRGGBB"
     */
    public static int parseARGB(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return 0x80000000; // Semi-transparent black
        }
        
        hexColor = hexColor.replace("#", "");
        
        try {
            if (hexColor.length() == 8) {
                return (int) Long.parseLong(hexColor, 16);
            } else if (hexColor.length() == 6) {
                return 0xFF000000 | Integer.parseInt(hexColor, 16);
            }
        } catch (NumberFormatException ignored) {}
        
        return 0x80000000;
    }
}

