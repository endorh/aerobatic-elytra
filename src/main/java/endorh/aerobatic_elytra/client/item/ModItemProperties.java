package endorh.aerobatic_elytra.client.item;

import endorh.aerobatic_elytra.client.config.ClientConfig;
import endorh.aerobatic_elytra.common.item.ElytraDyementReader;
import endorh.aerobatic_elytra.common.capability.IElytraSpec;
import endorh.aerobatic_elytra.common.item.AerobaticElytraItem;
import endorh.aerobatic_elytra.common.item.ModItems;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static endorh.aerobatic_elytra.AerobaticElytra.prefix;
import static endorh.aerobatic_elytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static endorh.aerobatic_elytra.common.item.IAbility.Ability.FUEL;
import static endorh.aerobatic_elytra.common.item.IAbility.Ability.MAX_FUEL;

@OnlyIn(Dist.CLIENT)
public class ModItemProperties {
	public static final ResourceLocation BROKEN_PROPERTY =
	  new ResourceLocation("broken");
	public static final ResourceLocation FUEL_PROPERTY = prefix("fuel");
	public static final ResourceLocation HIDE_FUEL_PROPERTY = prefix("hide_fuel");
	public static final ResourceLocation EQUAL_WINGS_PROPERTY = prefix("equal_wings");
	
	private static final ElytraDyementReader dyement = new ElytraDyementReader();
	
	public static void register() {
		reg(ModItems.AEROBATIC_ELYTRA, BROKEN_PROPERTY, ModItemProperties::getBrokenProperty);
		reg(ModItems.AEROBATIC_ELYTRA, FUEL_PROPERTY, ModItemProperties::getFuelProperty);
		reg(ModItems.AEROBATIC_ELYTRA, HIDE_FUEL_PROPERTY, ModItemProperties::getHideFuelProperty);
		reg(ModItems.AEROBATIC_ELYTRA, EQUAL_WINGS_PROPERTY, ModItemProperties::getEqualWingsProperty);
		reg(ModItems.AEROBATIC_ELYTRA_WING, BROKEN_PROPERTY, ModItemProperties::getBrokenProperty);
	}
	
	private static void reg(
	  Item item, ResourceLocation property, IItemPropertyGetter getter
	) {
		// FIXME: This can throw a ConcurrentModificationException (?)
		ItemModelsProperties.registerProperty(item, property, getter);
	}
	
	public static float getFuelProperty(ItemStack stack, ClientWorld world, LivingEntity holder) {
		return getFuelProperty(stack, world, holder, getElytraSpecOrDefault(stack));
	}
	
	/**
	 * How much fuel has the elytra
	 */
	public static float getFuelProperty(
	  ItemStack stack, ClientWorld world, LivingEntity holder, IElytraSpec spec
	) {
		return spec.getAbility(MAX_FUEL) == 0 ? 0F : spec.getAbility(FUEL) / spec.getAbility(MAX_FUEL);
	}
	
	/**
	 * How broken is the elytra
	 */
	public static float getBrokenProperty(ItemStack stack, ClientWorld world, LivingEntity holder) {
		return MathHelper.clamp(
		  (float)stack.getDamage() / (float)(stack.getMaxDamage() - 1),
		  0F, 1F);
	}
	
	public static float getHideFuelProperty(
	  ItemStack stack, ClientWorld world, LivingEntity holder
	) {
		if (!(stack.getItem() instanceof AerobaticElytraItem))
			return 0F;
		if (!((AerobaticElytraItem) stack.getItem()).shouldFuelRenderOverRockets(stack))
			return 1F;
		if (holder == null)
			return ClientConfig.fuel_visibility.test() ? 0F : 1F;
		ItemStack chest = holder.getItemStackFromSlot(EquipmentSlotType.CHEST);
		if (chest == stack)
			return 0F;
		return ClientConfig.fuel_visibility.test() ? 0F : 1F;
	}
	
	public static float getEqualWingsProperty(
	  ItemStack stack, ClientWorld world, LivingEntity holder
	) {
		if (!(stack.getItem() instanceof AerobaticElytraItem))
			return 0F;
		dyement.read(stack, false);
		return dyement.hasWingDyement? 0F : 1F;
	}
}
