package dnj.aerobatic_elytra.common.config;

/**
 * A collection of values used in different parts of the code
 * which don't quite fit in the config, such as texture offsets,
 * settings too specific to be explained properly, or simply
 * settings I haven't yet added to the config
 */
public class Const {
	// Texture offsets
	public static final int FLIGHT_GUI_TEXTURE_WIDTH = 256;
	public static final int FLIGHT_GUI_TEXTURE_HEIGHT = 256;
	public static final int FLIGHT_GUI_CROSSHAIR_SIZE = 25;
	public static final int FLIGHT_MODE_POPUP_WIDTH = 32;
	public static final int FLIGHT_MODE_POPUP_HEIGHT = 32;
	public static final int FLIGHT_MODE_POPUP_U_OFFSET = 100;
	public static final int FLIGHT_MODE_POPUP_V_OFFSET = 0;
	
	// Dynamic crosshair
	public static final float CROSSHAIR_PITCH_RANGE_PX = 3F;
	public static final float CROSSHAIR_ROLL_RANGE_DEG = 60F;
	public static final float CROSSHAIR_YAW_RANGE_PX = 0.5F;
	
	// Player renderer animation
	public static final float TAKEOFF_ANIMATION_LENGTH_TICKS = 10F;
	
	// Tilt applied to the player model
	public static final float TILT_PITCH_RENDER_OFFSET = 3.0F;
	public static final float TILT_ROLL_RENDER_OFFSET = 3.0F;
	public static final float TILT_YAW_RENDER_OFFSET = 3.0F;
	
	// Slime bounce animation
	public static final long SLIME_BOUNCE_CAMERA_ANIMATION_LENGTH_MS = 320L;
	
	// Slime bounce rolling tilt
	public static final float SLIME_BOUNCE_MAX_ROLLING_TILT_DEG = 60F;
	public static final float SLIME_BOUNCE_ROLLING_TILT_SENS = 12F;
	
	// Underwater control interpolation
	public static final float UNDERWATER_CONTROLS_SPEED_THRESHOLD = 0.8F;
	public static final float UNDERWATER_CONTROLS_TILT_FRICTION_MIN = 1F;
	public static final float UNDERWATER_CONTROLS_TILT_FRICTION_MAX = 0.85F;
	public static final float UNDERWATER_CONTROLS_DIRECT_SENSIBILITY_MIN = 0F;
	public static final float UNDERWATER_CONTROLS_DIRECT_SENSIBILITY_MAX = 8F;
	public static final float UNDERWATER_YAW_RANGE_MULTIPLIER = 1.6F;
}
