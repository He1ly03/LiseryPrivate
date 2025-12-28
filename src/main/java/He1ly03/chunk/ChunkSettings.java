package He1ly03.chunk;

/**
 * Settings for a claimed chunk
 */
public class ChunkSettings {
    
    private boolean buildAllowed;
    private boolean destroyAllowed;
    private boolean useAllowed;
    private boolean switchAllowed;
    private boolean mobsAllowed;
    private boolean pvpAllowed;
    private boolean fireAllowed;
    private boolean explosionAllowed;
    
    public ChunkSettings() {
        // Default: all protections active (nothing allowed for strangers)
        this.buildAllowed = false;
        this.destroyAllowed = false;
        this.useAllowed = false;
        this.switchAllowed = false;
        this.mobsAllowed = false;
        this.pvpAllowed = false;
        this.fireAllowed = false;
        this.explosionAllowed = false;
    }
    
    public ChunkSettings(boolean buildAllowed, boolean destroyAllowed, boolean useAllowed,
                         boolean switchAllowed, boolean mobsAllowed, boolean pvpAllowed,
                         boolean fireAllowed, boolean explosionAllowed) {
        this.buildAllowed = buildAllowed;
        this.destroyAllowed = destroyAllowed;
        this.useAllowed = useAllowed;
        this.switchAllowed = switchAllowed;
        this.mobsAllowed = mobsAllowed;
        this.pvpAllowed = pvpAllowed;
        this.fireAllowed = fireAllowed;
        this.explosionAllowed = explosionAllowed;
    }
    
    // Getters
    public boolean isBuildAllowed() {
        return buildAllowed;
    }
    
    public boolean isDestroyAllowed() {
        return destroyAllowed;
    }
    
    public boolean isUseAllowed() {
        return useAllowed;
    }
    
    public boolean isSwitchAllowed() {
        return switchAllowed;
    }
    
    public boolean isMobsAllowed() {
        return mobsAllowed;
    }
    
    public boolean isPvpAllowed() {
        return pvpAllowed;
    }
    
    public boolean isFireAllowed() {
        return fireAllowed;
    }
    
    public boolean isExplosionAllowed() {
        return explosionAllowed;
    }
    
    // Setters
    public void setBuildAllowed(boolean buildAllowed) {
        this.buildAllowed = buildAllowed;
    }
    
    public void setDestroyAllowed(boolean destroyAllowed) {
        this.destroyAllowed = destroyAllowed;
    }
    
    public void setUseAllowed(boolean useAllowed) {
        this.useAllowed = useAllowed;
    }
    
    public void setSwitchAllowed(boolean switchAllowed) {
        this.switchAllowed = switchAllowed;
    }
    
    public void setMobsAllowed(boolean mobsAllowed) {
        this.mobsAllowed = mobsAllowed;
    }
    
    public void setPvpAllowed(boolean pvpAllowed) {
        this.pvpAllowed = pvpAllowed;
    }
    
    public void setFireAllowed(boolean fireAllowed) {
        this.fireAllowed = fireAllowed;
    }
    
    public void setExplosionAllowed(boolean explosionAllowed) {
        this.explosionAllowed = explosionAllowed;
    }
    
    /**
     * Get setting value by name
     */
    public boolean getSetting(String settingName) {
        return switch (settingName.toLowerCase()) {
            case "build" -> buildAllowed;
            case "destroy" -> destroyAllowed;
            case "use" -> useAllowed;
            case "switch" -> switchAllowed;
            case "mobs" -> mobsAllowed;
            case "pvp" -> pvpAllowed;
            case "fire" -> fireAllowed;
            case "explosion" -> explosionAllowed;
            default -> false;
        };
    }
    
    /**
     * Set setting value by name
     */
    public void setSetting(String settingName, boolean value) {
        switch (settingName.toLowerCase()) {
            case "build" -> buildAllowed = value;
            case "destroy" -> destroyAllowed = value;
            case "use" -> useAllowed = value;
            case "switch" -> switchAllowed = value;
            case "mobs" -> mobsAllowed = value;
            case "pvp" -> pvpAllowed = value;
            case "fire" -> fireAllowed = value;
            case "explosion" -> explosionAllowed = value;
        }
    }
    
    /**
     * Copy settings from another ChunkSettings
     */
    public void copyFrom(ChunkSettings other) {
        this.buildAllowed = other.buildAllowed;
        this.destroyAllowed = other.destroyAllowed;
        this.useAllowed = other.useAllowed;
        this.switchAllowed = other.switchAllowed;
        this.mobsAllowed = other.mobsAllowed;
        this.pvpAllowed = other.pvpAllowed;
        this.fireAllowed = other.fireAllowed;
        this.explosionAllowed = other.explosionAllowed;
    }
    
    /**
     * Create a copy of this settings
     */
    public ChunkSettings copy() {
        return new ChunkSettings(
                buildAllowed, destroyAllowed, useAllowed, switchAllowed,
                mobsAllowed, pvpAllowed, fireAllowed, explosionAllowed
        );
    }
}

