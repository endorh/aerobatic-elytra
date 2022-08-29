package endorh.aerobaticelytra.common.registry;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.item.IEffectAbility;
import endorh.aerobaticelytra.common.item.IEffectAbility.EffectAbility;
import endorh.aerobaticelytra.common.item.IEffectAbility.EffectAbility.Deserializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContext.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.lang.Math.max;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class JsonAbilityManager extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = Deserializers.createConditionSerializer()
	  .registerTypeAdapter(EffectAbility.class, new Deserializer())
	  .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
	  .create();
	
	public JsonAbilityManager() {
		super(GSON, "aerobaticelytra-abilities");
	}
	
	@Override protected void apply(
	  @NotNull Map<ResourceLocation, JsonElement> map, @NotNull ResourceManager resourceManager,
	  @NotNull ProfilerFiller profiler
	) {
		AerobaticElytraRegistries.reloadDatapackAbilities(map.entrySet().stream().collect(Collectors.toMap(
		  Entry::getKey, e -> GSON.fromJson(e.getValue(), EffectAbility.class))));
	}
	
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent event) {
		if (event.phase != Phase.END || !AerobaticElytraLogic.hasAerobaticElytra(event.player) || event.side != LogicalSide.SERVER)
			return;
		ServerPlayer player = ((ServerPlayer) event.player);
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
	
	public static LootContext createEffectAbilityLootContext(ServerPlayer player) {
		return new Builder(player.getLevel()).withRandom(player.getRandom())
		  .withParameter(LootContextParams.THIS_ENTITY, player)
		  .withParameter(LootContextParams.ORIGIN, player.position())
		  .create(LootContextParamSets.GIFT);
	}
}
