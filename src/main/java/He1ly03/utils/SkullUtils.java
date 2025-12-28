package He1ly03.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class for working with player skulls and base64 textures
 */
public final class SkullUtils {
    
    private SkullUtils() {}
    
    /**
     * Apply base64 texture to a skull item
     * Preserves existing display name, lore, and item flags
     * @param item The skull item (must be PLAYER_HEAD)
     * @param base64 The base64 texture string
     * @return true if successful, false otherwise
     */
    public static boolean applyBase64Texture(ItemStack item, String base64) {
        if (item == null || item.getType() != Material.PLAYER_HEAD || base64 == null || base64.isEmpty()) {
            return false;
        }
        
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Save existing meta data BEFORE modifying
        Component displayName = meta.displayName();
        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();
        Set<ItemFlag> flags = meta.getItemFlags();
        Integer customModelData = meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        
        try {
            // Create a new player profile with random UUID
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "");
            
            // Add the texture property
            profile.setProperty(new ProfileProperty("textures", base64));
            
            // Apply the profile to the skull meta
            meta.setPlayerProfile(profile);
            
            // Restore all saved meta data
            if (displayName != null) {
                meta.displayName(displayName);
            }
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }
            for (ItemFlag flag : flags) {
                meta.addItemFlags(flag);
            }
            if (customModelData != null) {
                meta.setCustomModelData(customModelData);
            }
            
            // Save meta back to item
            item.setItemMeta(meta);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[LiseryPrivate] Failed to apply base64 texture: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create a skull item with base64 texture
     * @param base64 The base64 texture string
     * @return ItemStack with the texture applied
     */
    public static ItemStack createSkullWithTexture(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        applyBase64Texture(skull, base64);
        return skull;
    }
}
