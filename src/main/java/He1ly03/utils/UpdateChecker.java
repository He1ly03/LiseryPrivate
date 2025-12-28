package He1ly03.utils;

import He1ly03.LiseryPrivate;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for checking plugin updates from GitHub Releases
 */
public class UpdateChecker {
    
    private final LiseryPrivate plugin;
    private final String currentVersion;
    private final boolean enabled;
    private final String repository;
    
    private String latestVersion;
    private String downloadUrl;
    private boolean updateAvailable = false;
    
    public UpdateChecker(LiseryPrivate plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getPluginMeta().getVersion();
        this.enabled = plugin.getConfigManager().getConfig().getBoolean("update-checker.enabled", true);
        this.repository = plugin.getConfigManager().getConfig().getString("update-checker.repository", "");
    }
    
    /**
     * Check for updates asynchronously
     */
    public void checkForUpdates() {
        if (!enabled) {
            return;
        }
        
        if (repository == null || repository.isEmpty()) {
            plugin.getLogger().warning("Update checker is enabled but repository is not set in config.yml!");
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // Parse GitHub repository (format: owner/repo or full URL)
                String repoPath = parseRepository(repository);
                if (repoPath == null) {
                    plugin.getLogger().warning("Invalid repository format: " + repository);
                    return false;
                }
                
                // GitHub Releases API endpoint
                String apiUrl = "https://api.github.com/repos/" + repoPath + "/releases/latest";
                
                URL url = new URL(apiUrl); // Using URL for compatibility
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    plugin.getLogger().warning("Failed to check for updates: HTTP " + responseCode);
                    return false;
                }
                
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();
                
                // Parse JSON response
                Gson gson = new Gson();
                JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
                
                // Get latest version tag (remove 'v' prefix if present)
                String tagName = jsonResponse.get("tag_name").getAsString();
                this.latestVersion = tagName.replaceFirst("^v", "");
                this.downloadUrl = jsonResponse.get("html_url").getAsString();
                
                // Compare versions
                if (isNewerVersion(this.latestVersion, this.currentVersion)) {
                    this.updateAvailable = true;
                    return true;
                }
                
                return false;
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking for updates: " + e.getMessage());
                if (plugin.getConfigManager().getConfig().getBoolean("update-checker.debug", false)) {
                    e.printStackTrace();
                }
                return false;
            }
        }).thenAccept(updateFound -> {
            if (updateFound) {
                Bukkit.getScheduler().runTask(plugin, this::notifyUpdate);
            } else {
                if (plugin.getConfigManager().getConfig().getBoolean("update-checker.debug", false)) {
                    plugin.getLogger().info("Update check completed. You are using the latest version: " + currentVersion);
                }
            }
        });
    }
    
    /**
     * Parse repository string to owner/repo format
     */
    private String parseRepository(String repo) {
        if (repo == null || repo.isEmpty()) {
            return null;
        }
        
        // Remove trailing slash
        repo = repo.trim().replaceAll("/$", "");
        
        // If it's a full GitHub URL
        if (repo.contains("github.com")) {
            String[] parts = repo.split("github.com/");
            if (parts.length == 2) {
                String path = parts[1].split("\\?")[0].split("#")[0];
                String[] pathParts = path.split("/");
                if (pathParts.length >= 2) {
                    return pathParts[0] + "/" + pathParts[1];
                }
            }
            return null;
        }
        
        // If it's already in owner/repo format
        if (repo.contains("/") && !repo.startsWith("http")) {
            String[] parts = repo.split("/");
            if (parts.length == 2) {
                return parts[0] + "/" + parts[1];
            }
        }
        
        return null;
    }
    
    /**
     * Compare version strings
     */
    private boolean isNewerVersion(String latest, String current) {
        // Simple version comparison (works for versions like 1.0, 1.2.3, etc.)
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        
        int maxLength = Math.max(latestParts.length, currentParts.length);
        
        for (int i = 0; i < maxLength; i++) {
            int latestPart = 0;
            int currentPart = 0;
            
            if (i < latestParts.length) {
                try {
                    // Remove any non-numeric suffixes (e.g., "1.0-SNAPSHOT" -> "1.0")
                    String latestStr = latestParts[i].replaceAll("[^0-9].*", "");
                    if (!latestStr.isEmpty()) {
                        latestPart = Integer.parseInt(latestStr);
                    }
                } catch (NumberFormatException e) {
                    latestPart = 0;
                }
            }
            
            if (i < currentParts.length) {
                try {
                    String currentStr = currentParts[i].replaceAll("[^0-9].*", "");
                    if (!currentStr.isEmpty()) {
                        currentPart = Integer.parseInt(currentStr);
                    }
                } catch (NumberFormatException e) {
                    currentPart = 0;
                }
            }
            
            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }
        
        return false; // Versions are equal
    }
    
    /**
     * Notify console and operators about available update
     */
    private void notifyUpdate() {
        // Console message
        plugin.getLogger().warning("========================================");
        plugin.getLogger().warning("New version available!");
        plugin.getLogger().warning("Current version: " + currentVersion);
        plugin.getLogger().warning("Latest version: " + latestVersion);
        plugin.getLogger().warning("Download: " + downloadUrl);
        plugin.getLogger().warning("========================================");
        
        // Send message to operators in-game
        if (plugin.getConfigManager().getConfig().getBoolean("update-checker.notify-operators", true)) {
            Component message = Component.text()
                .append(Component.text("[" + plugin.getName() + "] ", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text("Доступно обновление! ", NamedTextColor.GREEN))
                .append(Component.text("Текущая версия: ", NamedTextColor.GRAY))
                .append(Component.text(currentVersion, NamedTextColor.WHITE))
                .append(Component.text(" → ", NamedTextColor.GRAY))
                .append(Component.text(latestVersion, NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Нажмите здесь, чтобы скачать", NamedTextColor.YELLOW, TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.openUrl(downloadUrl))
                    .hoverEvent(HoverEvent.showText(Component.text("Открыть в браузере", NamedTextColor.AQUA))))
                .build();
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player.isOp() || player.hasPermission("liseryprivate.admin"))
                    .forEach(player -> player.sendMessage(message));
            }, 60L); // Send after 3 seconds (60 ticks)
        }
    }
    
    /**
     * Get latest version (if checked)
     */
    public String getLatestVersion() {
        return latestVersion;
    }
    
    /**
     * Check if update is available
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    /**
     * Get download URL
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }
}

