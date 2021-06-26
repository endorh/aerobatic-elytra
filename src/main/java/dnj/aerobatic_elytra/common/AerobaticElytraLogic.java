package dnj.aerobatic_elytra.common;

import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.common.capability.ElytraSpecCapability;
import dnj.aerobatic_elytra.common.capability.IElytraSpec;
import dnj.aerobatic_elytra.common.item.AerobaticElytraItem;
import dnj.aerobatic_elytra.integration.colytra.ColytraIntegration;
import dnj.aerobatic_elytra.integration.curios.CuriosIntegration;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;

public class AerobaticElytraLogic {
	
	/**
	 * Shorthand for {@code !getAerobaticElytra(player).isEmpty()}
	 */
	public static boolean hasAerobaticElytra(PlayerEntity player) {
		return !getAerobaticElytra(player).isEmpty();
	}
	
	/**
	 * Check if an item is an Aerobatic Elytra or derived item<br>
	 * Note that this method may succeed for items that don't
	 * subclass {@link AerobaticElytraItem}, such as colytra
	 * aerobatic elytras<br>
	 * Items for which true is returned should have an
	 * {@link IElytraSpec}
	 */
	public static boolean isAerobaticElytra(ItemStack stack) {
		return !stack.isEmpty()
		       && (stack.getItem() instanceof AerobaticElytraItem
		           || !ColytraIntegration.getColytraSubItem(stack).isEmpty());
	}
	
	public static boolean isRemoteClientPlayerEntity(PlayerEntity player) {
		if (!player.world.isRemote)
			return false;
		return (player instanceof RemoteClientPlayerEntity);
	}
	
	public static boolean isClientPlayerEntity(PlayerEntity player) {
		if (!player.world.isRemote)
			return false;
		return (player instanceof ClientPlayerEntity);
	}
	
	/**
	 * Get the elytra itemStack, or empty if the entity doesn't have one equipped
	 */
	public static ItemStack getAerobaticElytra(LivingEntity entity) {
		ItemStack chest = entity.getItemStackFromSlot(EquipmentSlotType.CHEST);
		ItemStack elytra = ItemStack.EMPTY;
		if (chest.getItem() instanceof AerobaticElytraItem)
			return chest;
		if (chest.getItem() instanceof ArmorItem && AerobaticElytra.caelusLoaded) {
			elytra = ColytraIntegration.getColytraSubItem(chest);
			if (elytra != ItemStack.EMPTY)
				return elytra;
		}
		if (AerobaticElytra.curiosLoaded)
			elytra = CuriosIntegration.getCurioAerobaticElytra(entity);
		return elytra;
	}
	
	/**
	 * Shorthand for {@code getElytraSpec(getAerobaticElytra(entity))}
	 */
	@SuppressWarnings("unused")
	public static LazyOptional<IElytraSpec> getElytraSpec(LivingEntity entity) {
		return ElytraSpecCapability.getElytraSpec(getAerobaticElytra(entity));
	}
	
	/**
	 * Shorthand for {@code getElytraSpecOrDefault(getAerobaticElytra(entity))}
	 */
	public static IElytraSpec getElytraSpecOrDefault(LivingEntity entity) {
		return ElytraSpecCapability.getElytraSpecOrDefault(getAerobaticElytra(entity));
	}
}
