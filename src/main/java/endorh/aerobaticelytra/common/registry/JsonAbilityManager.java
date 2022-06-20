package endorh.aerobaticelytra.common.registry;

import com.google.gson.*;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.item.IEffectAbility;
import endorh.aerobaticelytra.common.item.IEffectAbility.EffectAbility;
import endorh.aerobaticelytra.common.item.IEffectAbility.EffectAbility.Deserializer;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootContext.Builder;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.LootSerializers;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.max;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class JsonAbilityManager extends JsonReloadListener {
	private static final Gson GSON = LootSerializers.func_237386_a_()
	  .registerTypeAdapter(EffectAbility.class, new Deserializer())
	  .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
	  .create();
	
	public JsonAbilityManager() {
		super(GSON, "aerobaticelytra-abilities");
	}
	
	@Override protected void apply(
	  @NotNull Map<ResourceLocation, JsonElement> map, @NotNull IResourceManager resourceManager,
	  @NotNull IProfiler profiler
	) {
		//noinspection unchecked
		final Set<EffectAbility> abilities = (Set<EffectAbility>) (Set<?>)
		  map.entrySet().stream().map(
		    e -> GSON.fromJson(e.getValue(), EffectAbility.class).setRegistryName(e.getKey())
		  ).collect(Collectors.toSet());
		
		ModRegistries.reloadDatapackAbilities(abilities);
	}
	
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent event) {
		if (event.phase != Phase.END || !AerobaticElytraLogic.hasAerobaticElytra(event.player) || event.side != LogicalSide.SERVER)
			return;
		ServerPlayerEntity player = ((ServerPlayerEntity) event.player);
		ItemStack elytra = AerobaticElytraLogic.getAerobaticElytra(player);
		if (elytra.isEmpty())
			return;
		final IElytraSpec spec = ElytraSpecCapability.getElytraSpecOrDefault(elytra);
		final Map<IEffectAbility, Boolean> abilities = spec.getEffectAbilities();
		if (!abilities.isEmpty()) {
			final LootContext ctx = createEffectAbilityLootContext(player);
			for (IEffectAbility ability : abilities.keySet()) {
				final boolean using = abilities.get(ability);
				if (spec.getAbility(ability) > 0F && ability.testConditions(ctx)) {
					if (!using) {
						ability.applyEffect(player);
						abilities.put(ability, true);
					} else ability.reapplyEffect(player);
					if (ability.getConsumption() != 0F)
						spec.setAbility(ability, max(0F, spec.getAbility(ability) - ability.getConsumption()));
				} else if (using) {
					ability.undoEffect(player);
					abilities.put(ability, false);
				}
			}
		}
	}
	
	public static LootContext createEffectAbilityLootContext(ServerPlayerEntity player) {
		return new Builder(player.getServerWorld()).withRandom(player.getRNG())
		  .withParameter(LootParameters.THIS_ENTITY, player)
		  .withParameter(LootParameters.field_237457_g_, player.getPositionVec())
		  .build(LootParameterSets.GIFT);
	}
}
