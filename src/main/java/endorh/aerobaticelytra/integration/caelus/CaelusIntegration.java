package endorh.aerobaticelytra.integration.caelus;

import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.caelus.api.CaelusApi;

import java.util.UUID;

public class CaelusIntegration {
	private static final Attribute CAELUS_FLIGHT_ATTRIBUTE =
	  CaelusApi.getInstance().getFlightAttribute();
	private static final AttributeModifier CAELUS_FLIGHT_MODIFIER = new AttributeModifier(
	  UUID.fromString("c433b7b2-7811-4c9a-b891-14bbed6d3dae"),
	  "Aerobatic Elytra Flight", 1.0, Operation.ADDITION);
	
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != Phase.END) return;
		
		Player player = event.player;
		AttributeInstance flight = player.getAttribute(CAELUS_FLIGHT_ATTRIBUTE);
		if (flight == null) return;
		boolean hasCurioElytra =
		  AerobaticElytraLogic.hasAerobaticElytra(player)
		  && !(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof AerobaticElytraItem);
		if (hasCurioElytra) {
			if (!flight.hasModifier(CAELUS_FLIGHT_MODIFIER))
				flight.addTransientModifier(CAELUS_FLIGHT_MODIFIER);
		} else if (flight.hasModifier(CAELUS_FLIGHT_MODIFIER))
			flight.removeModifier(CAELUS_FLIGHT_MODIFIER);
	}
}
