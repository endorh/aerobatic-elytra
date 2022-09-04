package endorh.aerobaticelytra;

import endorh.aerobaticelytra.client.item.AerobaticElytraBannerTextureManager;
import endorh.aerobaticelytra.common.AerobaticElytraInit;
import endorh.aerobaticelytra.common.registry.JsonAbilityManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Mod loading
 */
@Mod(AerobaticElytra.MOD_ID)
public final class AerobaticElytra {
    public static final String MOD_ID = "aerobaticelytra";
    
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker MAIN = MarkerManager.getMarker("MAIN");
    private static final Marker REGISTER = MarkerManager.getMarker("REGISTER");
    
    public static boolean caelusLoaded = false;
    public static boolean curiosLoaded = false;
    public static boolean colytraLoaded = false;
    
    public static final JsonAbilityManager JSON_ABILITY_MANAGER = new JsonAbilityManager();
    public static AerobaticElytraBannerTextureManager BANNER_TEXTURE_MANAGER;
    
    /**
     * Run setup tasks
     */
    public AerobaticElytra() {
        // Integrations
        final ModList modList = ModList.get();
        if (modList.isLoaded("caelus")) {
            caelusLoaded = true;
            if (modList.isLoaded("colytra"))
                colytraLoaded = true;
        }
        curiosLoaded = modList.isLoaded("curios");
        
        // Initialization
        AerobaticElytraInit.setup();
        LOGGER.debug(MAIN, "Mod loading started");
    }
    
    public static void logRegistered(String kind) {
        LOGGER.debug(REGISTER, "Registered " + kind);
    }
    
    @Internal public static ResourceLocation prefix(String key) {
        return new ResourceLocation(MOD_ID, key);
    }
}
