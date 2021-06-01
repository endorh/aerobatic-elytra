package dnj.aerobatic_elytra.common.capability;

import dnj.aerobatic_elytra.common.capability.IElytraSpec.TrailData;
import dnj.aerobatic_elytra.common.config.Config;
import dnj.aerobatic_elytra.common.item.IAbility;
import dnj.aerobatic_elytra.common.registry.ModRegistries;
import dnj.endor8util.capability.CapabilityProviderSerializable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Capability for {@link IElytraSpec}
 */
public class ElytraSpecCapability {
	/**
	 * The {@link Capability} instance
	 */
	@CapabilityInject(IElytraSpec.class)
	public static Capability<IElytraSpec> CAPABILITY = null;
	private static final Storage storage = new Storage();
	
	public static void register() {
		CapabilityManager.INSTANCE.register(
		  IElytraSpec.class, storage, ElytraSpec::new);
	}
	
	public static IElytraSpec fromNBT(CompoundNBT nbt) {
		IElytraSpec spec = new ElytraSpec();
		storage.readNBT(CAPABILITY, spec, null, nbt);
		return spec;
	}
	
	public static CompoundNBT asNBT(IElytraSpec spec) {
		return (CompoundNBT) storage.writeNBT(CAPABILITY, spec, null);
	}
	
	public static LazyOptional<IElytraSpec> getElytraSpec(ItemStack stack) {
		assert CAPABILITY != null;
		return stack.getCapability(CAPABILITY);
	}
	
	public static IElytraSpec getElytraSpecOrDefault(ItemStack stack) {
		assert CAPABILITY != null;
		return stack.getCapability(CAPABILITY).orElse(new ElytraSpec());
	}
	
	public static ICapabilitySerializable<INBT> createProvider() {
		if (CAPABILITY == null)
			return null;
		return new CapabilityProviderSerializable<>(CAPABILITY, null);
	}
	
	public static ICapabilitySerializable<INBT> createProvider(IElytraSpec spec) {
		if (CAPABILITY == null)
			return null;
		return new CapabilityProviderSerializable<>(CAPABILITY, null, spec);
	}
	
	/**
	 * Default implementation of {@link IElytraSpec}
	 */
	public static class ElytraSpec implements IElytraSpec {
		private final Map<IAbility, Float> properties = new HashMap<>();
		private final TrailData trailData = new TrailData();
		private final Map<String, Float> unknownProperties = new HashMap<>();
		
		public ElytraSpec() {}
		
		@Override public float getAbility(IAbility prop) {
			return properties.getOrDefault(prop, 0F);
		}
		@Override public void setAbility(IAbility prop, float value) {
			if (value == 0F)
				properties.remove(prop);
			else properties.put(prop, value);
		}
		@Override public Map<IAbility, Float> getAbilities() {
			return properties;
		}
		
		@Override public Map<String, Float> getUnknownAbilities() {
			return unknownProperties;
		}
		
		@Override public @Nonnull TrailData getTrailData() {
			return trailData;
		}
		
		@Override public String toString() {
			return String.format(
			  "FlightSpec: {%s, TrailData: %s}",
			  properties.entrySet().stream().map(
			    entry -> String.format("%s: %2.2f", entry.getKey(), entry.getValue())
			  ).collect(Collectors.joining(", ")),
			  trailData);
		}
	}
	
	public static boolean compareNoTrail(CompoundNBT leftCapNBT, CompoundNBT rightCapNBT) {
		if (leftCapNBT == null && rightCapNBT == null)
			return true;
		else if (leftCapNBT == null || rightCapNBT == null)
			return false;
		CompoundNBT left = leftCapNBT.copy();
		CompoundNBT right = rightCapNBT.copy();
		left.getCompound("Parent").getCompound(Storage.TAG_BASE).remove(Storage.TAG_TRAIL);
		right.getCompound("Parent").getCompound(Storage.TAG_BASE).remove(Storage.TAG_TRAIL);
		return left.equals(right);
	}
	
	/** Default Storage implementation */
	public static class Storage implements IStorage<IElytraSpec> {
		public static final String TAG_BASE = "aerobatic-elytra:spec";
		public static final String TAG_ABILITIES = "Ability";
		public static final String TAG_TRAIL = "Trail";
		
		@Nullable
		@Override
		public INBT writeNBT(Capability<IElytraSpec> cap, IElytraSpec inst, Direction side) {
			CompoundNBT nbt = new CompoundNBT();
			CompoundNBT data = new CompoundNBT();
			CompoundNBT ability = new CompoundNBT();
			
			for (Map.Entry<String, Float> unknown : inst.getUnknownAbilities().entrySet())
				ability.putFloat(unknown.getKey(), unknown.getValue());
			
			for (IAbility type : inst.getAbilities().keySet()) {
				final float val = inst.getAbility(type);
				if (val != type.getDefault())
					ability.putFloat(type.jsonName(), val);
			}
			
			data.put(TAG_ABILITIES, ability);
			
			CompoundNBT trailNBT = inst.getTrailData().write();
			if (trailNBT != null)
				data.put(TAG_TRAIL, trailNBT);
			
			nbt.put(TAG_BASE, data);
			return nbt;
		}
		@Override
		public void readNBT(Capability<IElytraSpec> cap, IElytraSpec inst, Direction side, INBT nbt) {
			CompoundNBT dat = (CompoundNBT) nbt;
			CompoundNBT data = dat.getCompound(TAG_BASE);
			CompoundNBT ability = data.getCompound(TAG_ABILITIES);
			
			for (IAbility type : ModRegistries.ABILITY_REGISTRY) {
				if (ability.contains(type.jsonName())) {
					float value = ability.getFloat(type.jsonName());
					if (Config.fix_nan_elytra_abilities && Float.isNaN(value))
						value = type.getDefault();
					inst.setAbility(type, value);
				} else inst.setAbility(type, type.getDefault());
			}
			
			Map<String, Float> unknownProperties = inst.getUnknownAbilities();
			unknownProperties.clear();
			for (String name : ability.keySet()) {
				if (!IAbility.isDefined(name)) {
					unknownProperties.put(name, ability.getFloat(name));
				}
			}
			
			TrailData trail = inst.getTrailData();
			if (data.contains(TAG_TRAIL)) {
				trail.read(data.getCompound(TAG_TRAIL));
			}
		}
	}
}