package dnj.aerobatic_elytra.common;

import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.common.capability.ElytraSpecCapability;
import dnj.aerobatic_elytra.common.capability.IFlightData;
import dnj.aerobatic_elytra.common.capability.IElytraSpec;
import dnj.aerobatic_elytra.common.flight.mode.FlightModeTags;
import dnj.aerobatic_elytra.common.item.AerobaticElytraItem;
import dnj.aerobatic_elytra.integration.colytra.ColytraIntegration;
import dnj.aerobatic_elytra.integration.curios.CuriosIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Optional;

import static dnj.aerobatic_elytra.common.capability.FlightDataCapability.getFlightData;
import static dnj.aerobatic_elytra.common.capability.FlightDataCapability.getFlightDataOrDefault;
import static dnj.aerobatic_elytra.common.item.IAbility.Ability.AQUATIC;
import static dnj.aerobatic_elytra.common.item.IAbility.Ability.FUEL;

/**
 * Contains the logic granting Aerobatic Flight to players
 */
public class AerobaticElytraLogic {
	
	// TODO: Cache this in a capability or hashmap and compute only once per tick
	public static boolean shouldElytraFly(PlayerEntity player) {
		if (!player.isElytraFlying() || player.abilities.isFlying
		    || !getFlightDataOrDefault(player).getFlightMode().is(FlightModeTags.ELYTRA))
			return false;
		ItemStack elytra = getAerobaticElytra(player);
		if (elytra.isEmpty())
			return false;
		IElytraSpec spec = ElytraSpecCapability.getElytraSpecOrDefault(elytra);
		return (elytra.getDamage() < elytra.getMaxDamage() - 1 && spec.getAbility(FUEL) > 0
		        || player.isCreative())
		       && !player.isInLava() && !player.isInWater();
	}
	
	// TODO: Compute once per tick
	public static boolean shouldAerobaticFly(PlayerEntity player) {
		if (!player.isElytraFlying() || player.abilities.isFlying
		    || !getFlightDataOrDefault(player).getFlightMode().is(FlightModeTags.AEROBATIC))
			return false;
		final ItemStack elytra = getAerobaticElytra(player);
		if (elytra.isEmpty())
			return false;
		final IElytraSpec spec = ElytraSpecCapability.getElytraSpecOrDefault(elytra);
		return (elytra.getDamage() < elytra.getMaxDamage() - 1 && spec.getAbility(FUEL) > 0
		        || player.isCreative())
		       && !player.isInLava() && (!player.isInWater() || spec.getAbility(AQUATIC) != 0);
	}
	
	public static boolean hasAerobaticElytra(PlayerEntity player) {
		ItemStack elytra = getAerobaticElytra(player);
		return !elytra.isEmpty() && ElytraSpecCapability.getElytraSpec(elytra).isPresent();
	}
	
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
	
	// Change
	public static boolean isTheClientPlayer(PlayerEntity player) {
		if (!player.world.isRemote)
			return false;
		return (player instanceof ClientPlayerEntity);
	}
	
	public static boolean isAbstractClientPlayerEntity(PlayerEntity player) {
		return player.world.isRemote;
		/*if (!player.world.isRemote)
			return false;
		return player instanceof AbstractClientPlayerEntity;*/
	}
	
	/**
	 * Determines if the entity can use fall flying
	 * @param stack Chest equipment stack
	 * @param entity Entity attempting to fall fly
	 */
	public static boolean canFallFly(ItemStack stack, LivingEntity entity) {
		if (entity instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity)entity;
			Optional<IFlightData> dat = getFlightData(player);
			if (!dat.isPresent())
				return false;
			IFlightData fd = dat.get();
			if (!fd.getFlightMode().is(FlightModeTags.ELYTRA))
				return false;
			if (player.isCreative())
				return true;
		}
		return stack.getDamage() < stack.getMaxDamage() - 1;
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
	
	public static LazyOptional<IElytraSpec> getElytraSpec(LivingEntity entity) {
		return ElytraSpecCapability.getElytraSpec(getAerobaticElytra(entity));
	}
	
	public static IElytraSpec getElytraSpecOrDefault(LivingEntity entity) {
		return ElytraSpecCapability.getElytraSpecOrDefault(getAerobaticElytra(entity));
	}
}
