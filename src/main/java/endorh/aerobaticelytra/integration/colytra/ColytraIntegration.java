package endorh.aerobaticelytra.integration.colytra;

import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.caelus.api.CaelusApi;

import java.util.UUID;

/**
 * Compatibility handler for Colytra mod<br>
 * The methods are only subscribed to the event bus if Colytra is loaded
 */
public class ColytraIntegration {
	/**
	 * Caelus flight modifier for aerobatic elytras in Colytra armors
	 */
	public static final AttributeModifier COLYTRA_CAELUS_FLIGHT_MODIFIER = new AttributeModifier(
	  UUID.fromString("668bdbee-32b6-4c4b-bf6a-5a30f4d02e37"), "Flight modifier", 1.0d,
	  AttributeModifier.Operation.ADDITION);
	
	/**
	 * Get the Colytra subitem from an armor, only if it's an aerobatic elytra
	 */
	public static ItemStack getColytraSubItem(ItemStack chest) {
		CompoundTag colytraTag = chest.getTagElement("colytra:ElytraUpgrade");
		if (colytraTag != null) {
			ItemStack elytra = ItemStack.of(colytraTag);
			if (elytra.getItem() instanceof AerobaticElytraItem)
				return elytra;
		}
		return ItemStack.EMPTY;
	}
	
	/**
	 * Get the Colytra subitem from an armor, only if it's an aerobatic elytra
	 */
	public static ItemStack getColytraSubItem(LivingEntity entity) {
		return getColytraSubItem(entity.getItemBySlot(EquipmentSlot.CHEST));
	}
	
	/**
	 * Apply our own Caelus flight modifier when the aerobatic elytra
	 * subitem can provide flight
	 */
	@SubscribeEvent(priority=EventPriority.LOW)
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != Phase.END) return;
		
		Player player = event.player;
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		if (!(chest.getItem() instanceof ArmorItem))
			return;
		ItemStack elytra = getColytraSubItem(chest);
		if (elytra.isEmpty())
			return;
		assert elytra.getItem() instanceof AerobaticElytraItem;
		
		AttributeInstance flightAttribute = player
		  .getAttribute(CaelusApi.getInstance().getFlightAttribute());
		
		assert flightAttribute != null;
		flightAttribute.removeModifier(COLYTRA_CAELUS_FLIGHT_MODIFIER);
		if (elytra.getItem().canElytraFly(elytra, player))
			flightAttribute.addTransientModifier(COLYTRA_CAELUS_FLIGHT_MODIFIER);
	}
}
