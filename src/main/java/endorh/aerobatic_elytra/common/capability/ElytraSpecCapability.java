package endorh.aerobatic_elytra.common.capability;

import endorh.aerobatic_elytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobatic_elytra.common.config.Config;
import endorh.aerobatic_elytra.common.item.IAbility;
import endorh.aerobatic_elytra.common.item.IEffectAbility;
import endorh.aerobatic_elytra.common.registry.ModRegistries;
import endorh.util.capability.CapabilityProviderSerializable;
import net.minecraft.entity.player.ServerPlayerEntity;
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
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
		protected WeakReference<ServerPlayerEntity> player;
		protected final Map<IAbility, Float> properties = new HashMap<>();
		protected final Map<IEffectAbility, Boolean> effectAbilities = new HashMap<>();
		protected final TrailData trailData = new TrailData();
		protected final Map<String, Float> unknownProperties = new HashMap<>();
		
		public ElytraSpec() {
			registerAerobaticElytraDatapackAbilityReloadListener(); // Must call this on every instance
		}
		
		@Override public void updatePlayerEntity(ServerPlayerEntity player) {
			this.player = new WeakReference<>(player);
		}
		
		@Override public @Nullable ServerPlayerEntity getPlayerEntity() {
			return player.get();
		}
		
		@Override public float getAbility(IAbility prop) {
			return properties.getOrDefault(prop, prop.getDefault());
		}
		@Override public void setAbility(IAbility prop, float value) {
			properties.put(prop, value);
			if (prop instanceof IEffectAbility && !effectAbilities.containsKey(prop))
				effectAbilities.put((IEffectAbility) prop, false);
		}
		
		@Override public Float removeAbility(IAbility ability) {
			if (ability instanceof IEffectAbility && effectAbilities.remove(ability)) {
				final ServerPlayerEntity player = this.player.get();
				if (player != null)
					((IEffectAbility) ability).undoEffect(player);
			}
			return properties.remove(ability);
		}
		
		@Override public Map<IAbility, Float> getAbilities() {
			return Collections.unmodifiableMap(properties);
		}
		
		@Override public void putAbilities(Map<IAbility, Float> abilities) {
			properties.putAll(abilities);
			for (IAbility ability : abilities.keySet())
				if (ability instanceof IEffectAbility && !effectAbilities.containsKey(ability))
					effectAbilities.put((IEffectAbility) ability, false);
		}
		
		@Override public void setAbilities(Map<IAbility, Float> abilities) {
			properties.clear();
			effectAbilities.clear();
			putAbilities(abilities);
		}
		
		@Override public boolean hasAbility(IAbility ability) {
			return properties.containsKey(ability);
		}
		
		@Override public Map<IEffectAbility, Boolean> getEffectAbilities() {
			return effectAbilities;
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
			  ).collect(Collectors.joining(", ")), trailData);
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
			
			for (Entry<IAbility, Float> entry : inst.getAbilities().entrySet()) {
				final IAbility type = entry.getKey();
				final float val = entry.getValue();
				if (val != type.getDefault())
					ability.putFloat(type.fullName(), val);
			}
			
			data.put(TAG_ABILITIES, ability);
			
			CompoundNBT trailNBT = inst.getTrailData().write();
			if (!trailNBT.isEmpty())
				data.put(TAG_TRAIL, trailNBT);
			
			nbt.put(TAG_BASE, data);
			return nbt;
		}
		@Override
		public void readNBT(Capability<IElytraSpec> cap, IElytraSpec inst, Direction side, INBT nbt) {
			CompoundNBT dat = (CompoundNBT) nbt;
			CompoundNBT data = dat.getCompound(TAG_BASE);
			CompoundNBT ability = data.getCompound(TAG_ABILITIES);
			
			for (IAbility type : ModRegistries.getAbilities().values()) {
				if (ability.contains(type.fullName())) {
					float value = ability.getFloat(type.fullName());
					if (Config.item.fix_nan_elytra_abilities && Float.isNaN(value))
						value = type.getDefault();
					inst.setAbility(type, value);
				} else inst.setAbility(type, type.getDefault());
			}
			
			Map<String, Float> unknown = inst.getUnknownAbilities();
			unknown.clear();
			for (String name : ability.keySet()) {
				if (!IAbility.isDefined(name))
					unknown.put(name, ability.getFloat(name));
			}
			
			TrailData trail = inst.getTrailData();
			if (data.contains(TAG_TRAIL)) {
				trail.read(data.getCompound(TAG_TRAIL));
			}
		}
	}
}