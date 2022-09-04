package endorh.aerobaticelytra.common;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.integration.colytra.ColytraIntegration;
import endorh.aerobaticelytra.integration.curios.CuriosIntegration;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;

public class AerobaticElytraLogic {
	
	/**
	 * Shorthand for {@code !getAerobaticElytra(player).isEmpty()}
	 */
	public static boolean hasAerobaticElytra(Player player) {
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
	
	public static boolean isRemoteLocalPlayer(Player player) {
		if (!player.level.isClientSide) return false;
		return (player instanceof RemotePlayer);
	}
	
	public static boolean isLocalPlayer(Player player) {
		if (!player.level.isClientSide) return false;
		return (player instanceof LocalPlayer);
	}
	
	/**
	 * Get the elytra itemStack, or empty if the entity doesn't have one equipped
	 */
	public static ItemStack getAerobaticElytra(LivingEntity entity) {
		ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
		ItemStack elytra = ItemStack.EMPTY;
		if (chest.getItem() instanceof AerobaticElytraItem)
			return chest;
		if (chest.getItem() instanceof ArmorItem && AerobaticElytra.caelusLoaded) {
			elytra = ColytraIntegration.getColytraSubItem(chest);
			if (elytra != ItemStack.EMPTY) return elytra;
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
