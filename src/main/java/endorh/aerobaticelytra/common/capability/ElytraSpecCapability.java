package endorh.aerobaticelytra.common.capability;

import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.item.IAbility;
import endorh.aerobaticelytra.common.item.IEffectAbility;
import endorh.aerobaticelytra.common.registry.AerobaticElytraRegistries;
import endorh.lazulib.capability.SerializableCapabilityWrapperProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
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
	public static Capability<IElytraSpec> CAPABILITY =
	  CapabilityManager.get(new CapabilityToken<>() {});
	
	public static IElytraSpec fromNBT(CompoundTag nbt) {
		IElytraSpec spec = new ElytraSpec();
		spec.deserializeCapability(nbt);
		return spec;
	}
	
	public static CompoundTag asNBT(IElytraSpec spec) {
		return spec.serializeCapability();
	}
	
	public static LazyOptional<IElytraSpec> getElytraSpec(ItemStack stack) {
		assert CAPABILITY != null;
		return stack.getCapability(CAPABILITY);
	}
	
	public static IElytraSpec getElytraSpecOrDefault(ItemStack stack) {
		assert CAPABILITY != null;
		return stack.getCapability(CAPABILITY).orElse(new ElytraSpec());
	}
	
	public static ICapabilitySerializable<CompoundTag> createProvider() {
		if (CAPABILITY == null) return null;
		return new SerializableCapabilityWrapperProvider<>(CAPABILITY, null, new ElytraSpec());
	}
	
	public static ICapabilitySerializable<CompoundTag> createProvider(IElytraSpec spec) {
		if (CAPABILITY == null) return null;
		return new SerializableCapabilityWrapperProvider<>(CAPABILITY, null, spec);
	}
	
	/**
	 * Default implementation of {@link IElytraSpec}
	 */
	public static class ElytraSpec implements IElytraSpec {
		public static final String TAG_BASE = "aerobaticelytra:spec";
		public static final String TAG_ABILITIES = "Ability";
		public static final String TAG_TRAIL = "Trail";
		
		protected WeakReference<ServerPlayer> player;
		protected final Map<IAbility, Float> properties = new HashMap<>();
		protected final Map<IEffectAbility, Boolean> effectAbilities = new HashMap<>();
		protected final TrailData trailData = new TrailData();
		protected final Map<String, Float> unknownProperties = new HashMap<>();
		
		public ElytraSpec() {
			registerAerobaticElytraDatapackAbilityReloadListener(); // Must call this on every instance
		}
		
		@Override public void updatePlayerEntity(ServerPlayer player) {
			this.player = new WeakReference<>(player);
		}
		
		@Override public @Nullable ServerPlayer getPlayerEntity() {
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
				final ServerPlayer player = this.player.get();
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
			for (IAbility ability: abilities.keySet())
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
		
		@Override public boolean areAbilitiesEqual(IElytraSpec other) {
			return properties.equals(other.getAbilities()) && effectAbilities.equals(other.getEffectAbilities()) && unknownProperties.equals(other.getUnknownAbilities());
		}
		
		@Override public @Nonnull TrailData getTrailData() {
			return trailData;
		}
		
		@Override public CompoundTag serializeCapability() {
			CompoundTag nbt = new CompoundTag();
			CompoundTag data = new CompoundTag();
			CompoundTag ability = new CompoundTag();
			
			for (Map.Entry<String, Float> unknown: getUnknownAbilities().entrySet())
				ability.putFloat(unknown.getKey(), unknown.getValue());
			
			for (Entry<IAbility, Float> entry: getAbilities().entrySet()) {
				final IAbility type = entry.getKey();
				final float val = entry.getValue();
				if (val != type.getDefault())
					ability.putFloat(type.fullName(), val);
			}
			
			data.put(TAG_ABILITIES, ability);
			
			CompoundTag trailNBT = getTrailData().write();
			if (!trailNBT.isEmpty())
				data.put(TAG_TRAIL, trailNBT);
			
			nbt.put(TAG_BASE, data);
			return nbt;
		}
		
		@Override public void deserializeCapability(CompoundTag nbt) {
			CompoundTag data = nbt.getCompound(TAG_BASE);
			CompoundTag ability = data.getCompound(TAG_ABILITIES);
			
			for (IAbility type: AerobaticElytraRegistries.getAbilities().values()) {
				if (ability.contains(type.fullName())) {
					float value = ability.getFloat(type.fullName());
					if (Config.item.fix_nan_elytra_abilities && Float.isNaN(value))
						value = type.getDefault();
					setAbility(type, value);
				} else setAbility(type, type.getDefault());
			}
			
			Map<String, Float> unknown = getUnknownAbilities();
			unknown.clear();
			for (String name: ability.getAllKeys()) {
				if (!IAbility.isDefined(name))
					unknown.put(name, ability.getFloat(name));
			}
			
			TrailData trail = getTrailData();
			if (data.contains(TAG_TRAIL)) {
				trail.read(data.getCompound(TAG_TRAIL));
			}
		}
		
		@Override public String toString() {
			return String.format(
			  "FlightSpec: {%s, TrailData: %s}",
			  properties.entrySet().stream().map(
				 entry -> String.format("%s: %2.2f", entry.getKey(), entry.getValue())
			  ).collect(Collectors.joining(", ")), trailData);
		}
	}
	
	public static boolean compareNoTrail(CompoundTag leftCapNBT, CompoundTag rightCapNBT) {
		if (leftCapNBT == null && rightCapNBT == null)
			return true;
		else if (leftCapNBT == null || rightCapNBT == null)
			return false;
		CompoundTag left = leftCapNBT.copy();
		CompoundTag right = rightCapNBT.copy();
		left.getCompound("Parent").getCompound(ElytraSpec.TAG_BASE).remove(ElytraSpec.TAG_TRAIL);
		right.getCompound("Parent").getCompound(ElytraSpec.TAG_BASE).remove(ElytraSpec.TAG_TRAIL);
		return left.equals(right);
	}
}