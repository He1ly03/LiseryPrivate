package He1ly03.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for displaying plugin logo
 * Uses MiniMessage for colored output
 */
public final class LogoUtils {
    
    private LogoUtils() {}
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static List<String> logoLines = null;
    
    /**
     * Load logo from resources
     */
    private static List<String> loadLogo() {
        if (logoLines != null) {
            return logoLines;
        }
        
        logoLines = new ArrayList<>();
        try (InputStream is = LogoUtils.class.getClassLoader().getResourceAsStream("logo.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                logoLines.add(line);
            }
        } catch (Exception e) {
            // If logo.txt not found, use default ASCII art
            logoLines = getDefaultLogo();
        }
        
        return logoLines;
    }
    
    /**
     * Get default logo if file not found
     */
    private static List<String> getDefaultLogo() {
        List<String> defaultLogo = new ArrayList<>();
        defaultLogo.add("  _     _                     ____       _            _       ");
        defaultLogo.add(" | |   (_)___  ___ _ __ _   _|  _ \\ _ __(_)_   ____ _| |_ ___ ");
        defaultLogo.add(" | |   | / __|/ _ \\ '__| | | | |_) | '__| \\ \\ / / _` | __/ _ \\");
        defaultLogo.add(" | |___| \\__ \\  __/ |  | |_| |  __/| |  | |\\ V / (_| | ||  __/");
        defaultLogo.add(" |_____|_|___/\\___|_|   \\__, |_|   |_|  |_| \\_/ \\__,_|\\__\\___|");
        defaultLogo.add("                        |___/                                  ");
        return defaultLogo;
    }
    
    /**
     * Print logo in green color using MiniMessage (for enable)
     */
    public static void printGreenLogoSafe(org.bukkit.plugin.Plugin plugin) {
        printLogoSafe(plugin, "<green><bold>"); // Green bold
    }
    
    /**
     * Print logo in red color using MiniMessage (for disable)
     */
    public static void printRedLogoSafe(org.bukkit.plugin.Plugin plugin) {
        printLogoSafe(plugin, "<red><bold>"); // Red bold
    }
    
    /**
     * Print logo using Bukkit logger with MiniMessage
     */
    private static void printLogoSafe(org.bukkit.plugin.Plugin plugin, String colorTag) {
        List<String> lines = loadLogo();
        
        // Try to use ComponentLogger for better MiniMessage support
        ComponentLogger logger = null;
        try {
            logger = ComponentLogger.logger(plugin.getClass());
        } catch (Exception e) {
            // Fallback to regular logger if ComponentLogger not available
        }
        
        plugin.getLogger().info(""); // Empty line
        
        for (String line : lines) {
            // Use MiniMessage to parse color tag and apply to line
            // No need for <reset> as each line is printed separately
            String miniMessageText = colorTag + line;
            Component component = MINI_MESSAGE.deserialize(miniMessageText);
            
            if (logger != null) {
                // Use ComponentLogger if available (Paper 1.19+)
                logger.info(component);
            } else {
                // Fallback: convert to legacy text for regular logger
                String legacyText = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacySection()
                        .serialize(component);
                plugin.getLogger().info(legacyText);
            }
        }
        
        plugin.getLogger().info(""); // Empty line
    }
}

