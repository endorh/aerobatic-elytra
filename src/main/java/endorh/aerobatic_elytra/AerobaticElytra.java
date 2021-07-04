package endorh.aerobatic_elytra;

import endorh.aerobatic_elytra.common.ModInit;
import endorh.aerobatic_elytra.common.registry.JsonAbilityManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Mod loading
 */
@Mod(AerobaticElytra.MOD_ID)
public final class AerobaticElytra {
    public static final String MOD_ID = "aerobatic-elytra";
    
    protected static final Logger LOGGER = LogManager.getLogger();
    protected static final Marker MAIN = MarkerManager.getMarker("MAIN");
    protected static final Marker REGISTER = MarkerManager.getMarker("REGISTER");
    
    public static boolean caelusLoaded = false;
    public static boolean curiosLoaded = false;
    public static boolean colytraLoaded = false;
    
    public static final JsonAbilityManager JSON_ABILITY_MANAGER = new JsonAbilityManager();
    
    /**
     * Run setup tasks
     */
    public AerobaticElytra() {
        // Compatibility
        final ModList modList = ModList.get();
        if (modList.isLoaded("caelus")) {
            caelusLoaded = true;
            if (modList.isLoaded("colytra"))
                colytraLoaded = true;
        }
        curiosLoaded = modList.isLoaded("curios");
        
        // Initialization
        ModInit.setup();
        LOGGER.debug(MAIN, "Mod loading started");
    }
    
    public static void logRegistered(String kind) {
        LOGGER.debug(REGISTER, "Registered " + kind);
    }
    
    public static ResourceLocation prefix(String key) {
        return new ResourceLocation(MOD_ID, key);
    }
}
