package endorh.aerobaticelytra.client.item;

import endorh.aerobaticelytra.client.config.ClientConfig.style.visibility;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import endorh.aerobaticelytra.common.item.ElytraDyement;
import endorh.aerobaticelytra.common.item.IAbility.Ability;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;
import static endorh.aerobaticelytra.common.AerobaticElytraLogic.isAerobaticElytra;
import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;

@OnlyIn(Dist.CLIENT)
public class AerobaticItemProperties {
	public static final ResourceLocation BROKEN_PROPERTY =
	  new ResourceLocation("broken");
	public static final ResourceLocation FUEL_PROPERTY = prefix("fuel");
	public static final ResourceLocation HIDE_FUEL_PROPERTY = prefix("hide_fuel");
	public static final ResourceLocation HIDE_ROCKETS_PROPERTY = prefix("hide_rockets");
	public static final ResourceLocation EQUAL_WINGS_PROPERTY = prefix("equal_wings");
	
	private static final ElytraDyement dyement = new ElytraDyement();
	
	public static void register() {
		reg(AerobaticElytraItems.AEROBATIC_ELYTRA, BROKEN_PROPERTY, AerobaticItemProperties::getBrokenProperty);
		reg(AerobaticElytraItems.AEROBATIC_ELYTRA, FUEL_PROPERTY, AerobaticItemProperties::getFuelProperty);
		reg(AerobaticElytraItems.AEROBATIC_ELYTRA, HIDE_FUEL_PROPERTY, AerobaticItemProperties::getHideFuelProperty);
		reg(AerobaticElytraItems.AEROBATIC_ELYTRA, HIDE_ROCKETS_PROPERTY, AerobaticItemProperties::getHideRocketsProperty);
		reg(AerobaticElytraItems.AEROBATIC_ELYTRA, EQUAL_WINGS_PROPERTY, AerobaticItemProperties::getEqualWingsProperty);
		reg(AerobaticElytraItems.AEROBATIC_ELYTRA_WING, BROKEN_PROPERTY, AerobaticItemProperties::getBrokenProperty);
		reg(AerobaticElytraItems.AEROBATIC_ELYTRA_WING, HIDE_ROCKETS_PROPERTY, AerobaticItemProperties::getHideRocketsProperty);
	}
	
	private static void reg(
	  Item item, ResourceLocation property, ClampedItemPropertyFunction getter
	) {
		ItemProperties.register(item, property, getter);
	}
	
	public static float getFuelProperty(
	  ItemStack stack, ClientLevel world, LivingEntity holder, int seed
	) {
		return getFuelProperty(stack, world, holder, getElytraSpecOrDefault(stack));
	}
	
	/**
	 * How much fuel has the elytra
	 */
	public static float getFuelProperty(
	  ItemStack stack, ClientLevel world, LivingEntity holder, IElytraSpec spec
	) {
		return spec.getAbility(Ability.MAX_FUEL) == 0 ? 0F : spec.getAbility(Ability.FUEL) / spec.getAbility(
		  Ability.MAX_FUEL);
	}
	
	/**
	 * How broken is the elytra
	 */
	public static float getBrokenProperty(ItemStack stack, ClientLevel world, LivingEntity holder, int seed) {
		return Mth.clamp(
		  (float)stack.getDamageValue() / (float)(stack.getMaxDamage() - 1),
		  0F, 1F);
	}
	
	public static float getHideFuelProperty(
	  ItemStack stack, ClientLevel world, LivingEntity holder, int seed
	) {
		if (!(stack.getItem() instanceof AerobaticElytraItem))
			return 0F;
		if (!((AerobaticElytraItem) stack.getItem()).shouldFuelRenderOverRockets(stack))
			return 1F;
		if (holder == null)
			return visibility.fuel_visibility.test() ? 0F : 1F;
		ItemStack chest = holder.getItemBySlot(EquipmentSlot.CHEST);
		if (chest == stack)
			return 0F;
		return visibility.fuel_visibility.test() ? 0F : 1F;
	}
	
	public static float getHideRocketsProperty(
	  ItemStack stack, ClientLevel world, LivingEntity holder, int seed
	) {
		if (!isAerobaticElytra(stack)) return 0F;
		return getElytraSpecOrDefault(stack).getAbility(Ability.ROCKETLESS);
	}
	
	public static float getEqualWingsProperty(
	  ItemStack stack, ClientLevel world, LivingEntity holder, int seed
	) {
		if (!(stack.getItem() instanceof AerobaticElytraItem))
			return 0F;
		dyement.read(stack, false);
		return dyement.hasWingDyement? 0F : 1F;
	}
}
