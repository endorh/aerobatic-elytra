package dnj.aerobatic_elytra.integration.colytra;

import dnj.aerobatic_elytra.common.AerobaticElytraLogic;
import dnj.aerobatic_elytra.common.item.AerobaticElytraItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import top.theillusivec4.caelus.api.CaelusApi;

import java.util.UUID;

// TODO: Extract a ClientColytraIntegration
public class ColytraIntegration {
	
	public static final AttributeModifier COLYTRA_CAELUS_FLIGHT_MODIFIER = new AttributeModifier(
	  UUID.fromString("668bdbee-32b6-4c4b-bf6a-5a30f4d02e37"), "Flight modifier", 1.0d,
	  AttributeModifier.Operation.ADDITION);
	
	public static ItemStack getColytraSubItem(ItemStack chest) {
		CompoundNBT colytraTag = chest.getChildTag("colytra:ElytraUpgrade");
		if (colytraTag != null) {
			ItemStack elytra = ItemStack.read(colytraTag);
			if (elytra.getItem() instanceof AerobaticElytraItem)
				return elytra;
		}
		return ItemStack.EMPTY;
	}
	
	public static ItemStack getColytraSubItem(LivingEntity entity) {
		return getColytraSubItem(entity.getItemStackFromSlot(EquipmentSlotType.CHEST));
	}
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.side != LogicalSide.SERVER || event.phase != Phase.END)
			return;
		
		PlayerEntity player = event.player;
		ItemStack chest = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
		if (!(chest.getItem() instanceof ArmorItem))
			return;
		ItemStack elytra = getColytraSubItem(chest);
		if (elytra.isEmpty())
			return;
		assert elytra.getItem() instanceof AerobaticElytraItem;
		
		ModifiableAttributeInstance flightAttribute = player
		  .getAttribute(CaelusApi.ELYTRA_FLIGHT.get());
		
		assert flightAttribute != null;
		flightAttribute.removeModifier(COLYTRA_CAELUS_FLIGHT_MODIFIER);
		if (elytra.getItem().canElytraFly(elytra, player))
			flightAttribute.applyNonPersistentModifier(COLYTRA_CAELUS_FLIGHT_MODIFIER);
	}
}
