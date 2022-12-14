package endorh.aerobaticelytra.server.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import endorh.aerobaticelytra.common.item.IAbility;
import endorh.aerobaticelytra.common.registry.AerobaticElytraRegistries;
import endorh.aerobaticelytra.debug.Debug;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static endorh.util.command.QualifiedNameArgumentType.optionallyQualified;
import static endorh.util.text.TextUtil.stc;
import static endorh.util.text.TextUtil.ttc;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class AerobaticElytraCommand {
	private static final String DATAPACK_LOCATION = "datapacks";
	
	public static final SuggestionProvider<CommandSourceStack> SUGGEST_PACKS =
	  ((context, builder) -> SharedSuggestionProvider.suggest(
	    getAvailablePacks(context.getSource()).values().stream()
	      .filter(bd -> !isPackInstalled(context.getSource(), bd.getTitle()))
	      .map(bd -> escapeIfRequired(bd.getTitle())), builder));
	public static final SuggestionProvider<CommandSourceStack> SUGGEST_ABILITIES =
	  ((context, builder) -> SharedSuggestionProvider.suggest(
	    AerobaticElytraRegistries.getAbilitiesByName().keySet(), builder));
	public static final SimpleCommandExceptionType NO_ELYTRA_HOLDING_TARGETS =
	  new SimpleCommandExceptionType(ttc(
	    "commands.aerobaticelytra.error.no_elytra"));
	public static final SimpleCommandExceptionType UNKNOWN_ABILITY =
	  new SimpleCommandExceptionType(ttc(
	    "commands.aerobaticelytra.error.unknown_ability"));
	private static final Logger LOGGER = LogManager.getLogger();
	
	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		register(event.getDispatcher());
	}
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> aerobaticElytraCommand =
		  literal("aerobaticelytra")
		    .requires(cs -> cs.hasPermission(2))
		    .then(
		      literal("datapack").then(
			     literal("install").then(
			       argument("datapack", string())
		            .suggests(SUGGEST_PACKS)
		            .executes(cc -> installPack(cc, getString(cc, "datapack"))))
		      ).then(literal("list").executes(AerobaticElytraCommand::listPacks))
		    ).then(
			   literal("debug")
			     .then(literal("show").executes(cc -> enableDebug(cc, true)))
			     .then(literal("hide").executes(cc -> enableDebug(cc, false)))
			     .then(literal("give").executes(AerobaticElytraCommand::giveDebugWing))
			     .then(literal("particles")
			             .then(literal("show").executes(cc -> enableParticles(cc, true)))
			             .then(literal("hide").executes(cc -> enableParticles(cc, false))))
			     .then(literal("freeze")
			             .then(literal("invert").executes(cc -> invertFreeze(cc, true)))
			             .then(literal("normal").executes(cc -> invertFreeze(cc, false))))
		  ).then(
			   literal("ability").then(
				  literal("get").then(
				    argument("target", EntityArgument.entity())
		          .executes(AerobaticElytraCommand::getAbilities).then(
						  argument("ability_name", optionallyQualified())
			           .suggests(SUGGEST_ABILITIES)
			           .executes(cc -> getAbility(
			             cc, getString(cc, "ability_name")))))
		    ).then(
				  literal("set").then(
				    argument("target", EntityArgument.entities()).then(
					   argument("ability_name", optionallyQualified())
				      .suggests(SUGGEST_ABILITIES).then(
						    argument("ability_value", floatArg()).executes(
						    cc -> setAbility(
						      cc, getString(cc, "ability_name"), getFloat(cc, "ability_value"))))))
		    ).then(
				  literal("reset").then(
				    argument("target", EntityArgument.entities())
				    .executes(AerobaticElytraCommand::resetAbilities)
				    .then(
				      argument("ability_name", optionallyQualified())
					     .suggests(SUGGEST_ABILITIES)
					     .executes(
						    cc -> resetAbility(cc, getString(cc, "ability_name")))))
		    ).then(
				  literal("remove").then(
				    argument("target", EntityArgument.entities())
				    .executes(AerobaticElytraCommand::removeAbilities)
				    .then(
				      argument("ability_name", optionallyQualified())
					     .suggests(SUGGEST_ABILITIES)
					     .executes(cc -> removeAbility(cc, getString(cc, "ability_name")))))
		    ).then(
				  literal("unknown").then(
				    literal("get").then(
				      argument("target", EntityArgument.entity())
				      .executes(AerobaticElytraCommand::getUnknownAbilities).then(
						    argument("ability_name", optionallyQualified())
					     .executes(cc -> getUnknownAbility(cc, getString(cc, "ability_name")))))
		      ).then(
				    literal("set").then(
				      argument("target", EntityArgument.entities()).then(
					     argument("ability_name", optionallyQualified()).then(
						    argument("ability_value", floatArg())
				          .executes(cc -> setUnknownAbility(
				            cc, getString(cc, "ability_name"), getFloat(cc, "ability_value"))))))
		      ).then(
				    literal("remove").then(
				      argument("target", EntityArgument.entities())
			         .executes(AerobaticElytraCommand::removeUnknownAbilities).then(
						    argument("ability_name", optionallyQualified())
			             .executes(cc -> removeUnknownAbility(cc, getString(cc, "ability_name"))))))));
		dispatcher.register(aerobaticElytraCommand);
	}
	
	public static Entity getTarget(
	  CommandContext<CommandSourceStack> cc
	) throws CommandSyntaxException {
		return EntityArgument.getEntity(cc, "target");
	}
	
	public static List<IElytraSpec> getElytraSpecs(CommandContext<CommandSourceStack> cc)
	  throws CommandSyntaxException {
		final List<IElytraSpec> list = EntityArgument.getEntities(cc, "target").stream()
		  .filter(e -> e instanceof LivingEntity)
		  .map(e -> ((LivingEntity) e).getItemBySlot(EquipmentSlot.CHEST))
		  .filter(AerobaticElytraLogic::isAerobaticElytra)
		  .map(ElytraSpecCapability::getElytraSpecOrDefault)
		  .collect(Collectors.toList());
		if (list.isEmpty())
			throw NO_ELYTRA_HOLDING_TARGETS.create();
		return list;
	}
	
	public static IElytraSpec getElytraSpec(CommandContext<CommandSourceStack> cc)
	  throws CommandSyntaxException{
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		if (specs.size() != 1)
			throw new IllegalStateException("Expected a single entity target");
		return specs.get(0);
	}
	
	public static Component displayFloat(float value) {
		return Component.literal(String.format("%5.2f", value))
		  .withStyle(ChatFormatting.AQUA);
	}
	
	public static int getAbility(
	  CommandContext<CommandSourceStack> cc, String name
	) throws CommandSyntaxException {
		final IElytraSpec spec = getElytraSpec(cc);
		if (!IAbility.isDefined(name))
			throw UNKNOWN_ABILITY.create();
		final IAbility ability = IAbility.fromName(name);
		if (spec.hasAbility(ability)) {
			final float value = spec.getAbility(ability);
			cc.getSource().sendSuccess(
			  ttc("commands.aerobaticelytra.ability.get.success", getTarget(cc).getDisplayName())
				 .append("\n")
				 .append(ttc("commands.aerobaticelytra.ability.get.ability",
				             ability.getDisplayName(), displayFloat(value))), true);
		} else {
			cc.getSource().sendSuccess(
			  ttc("commands.aerobaticelytra.ability.get.default",
			      ability.getDisplayName(), getTarget(cc).getDisplayName(),
			      displayFloat(ability.getDefault())), true);
		}
		return 0;
	}
	
	private static int getUnknownAbility(
	  CommandContext<CommandSourceStack> cc, String name
	) throws CommandSyntaxException {
		final IElytraSpec spec = getElytraSpec(cc);
		if (spec.getUnknownAbilities().containsKey(name)) {
			final float value = spec.getUnknownAbilities().get(name);
			cc.getSource().sendSuccess(
			  ttc("commands.aerobaticelytra.ability.get.success.unknown", getTarget(cc).getDisplayName())
				 .append("\n")
				 .append(ttc("commands.aerobaticelytra.ability.get.ability.unknown",
				             name, displayFloat(value))), true);
		} else {
			cc.getSource().sendFailure(
			  ttc("commands.aerobaticelytra.ability.get.failure.unknown", name, getTarget(cc).getDisplayName()));
		}
		return 0;
	}
	
	public static int getAbilities(CommandContext<CommandSourceStack> cc)
	  throws CommandSyntaxException { return getAbilities(cc, true); }
	
	public static int getAbilities(CommandContext<CommandSourceStack> cc, boolean show_unknown)
	  throws CommandSyntaxException { return getAbilities(cc, true, show_unknown); }
	
	public static int getUnknownAbilities(CommandContext<CommandSourceStack> cc)
	  throws CommandSyntaxException {
		return getAbilities(cc, false, true);
	}
	
	public static int getAbilities(CommandContext<CommandSourceStack> cc, boolean show_known, boolean show_unknown)
	  throws CommandSyntaxException {
		IElytraSpec spec = getElytraSpec(cc);
		final Component name = getTarget(cc).getDisplayName();
		MutableComponent msg = stc("");
		if (show_known) {
			msg = msg.append(
			  ttc("commands.aerobaticelytra.ability.get.all.success",
			      spec.getAbilities().size(), name));
			for (IAbility ability : AerobaticElytraRegistries.getAbilities().values())
				msg = msg.append("\n").append(
				  ttc("commands.aerobaticelytra.ability.get.ability", ability.getDisplayName(),
				      displayFloat(spec.getAbility(ability))));
		}
		if (show_unknown) {
			final Map<String, Float> unknown = spec.getUnknownAbilities();
			if (unknown.isEmpty()) {
				msg = msg.append("\n").append(
				  ttc("commands.aerobaticelytra.ability.get.all.unknown.empty", name));
			} else {
				msg = msg.append("\n").append(
				  ttc("commands.aerobaticelytra.ability.get.all.unknown", unknown.size(), name));
				for (Entry<String, Float> entry : unknown.entrySet()) {
					msg = msg.append("\n").append(
					  ttc("commands.aerobaticelytra.ability.get.ability.unknown",
					      entry.getKey(), displayFloat(entry.getValue())));
				}
			}
		}
		cc.getSource().sendSuccess(msg, true);
		return 0;
	}
	
	public static int setAbility(CommandContext<CommandSourceStack> cc, String name, float value)
	  throws CommandSyntaxException {
		if (!IAbility.isDefined(name))
			throw UNKNOWN_ABILITY.create();
		final IAbility ability = IAbility.fromName(name);
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		specs.forEach(s -> s.setAbility(ability, value));
		cc.getSource().sendSuccess(
		  ttc("commands.aerobaticelytra.ability.set.success", specs.size())
		    .append("\n")
		    .append(ttc(
		      "commands.aerobaticelytra.ability.set.ability", ability.getDisplayName(),
		      displayFloat(value))), true);
		return 0;
	}
	
	public static int setUnknownAbility(
	  CommandContext<CommandSourceStack> cc, String name, float value
	) throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		specs.forEach(s -> s.getUnknownAbilities().put(name, value));
		cc.getSource().sendSuccess(
		  ttc("commands.aerobaticelytra.ability.set.success.unknown", specs.size())
		    .append("\n").append(
		      ttc("commands.aerobaticelytra.ability.set.ability.unknown", name, displayFloat(value))),
		  true
		);
		return 0;
	}
	
	public static int resetAbility(CommandContext<CommandSourceStack> cc, String name)
	  throws CommandSyntaxException {
		if (!IAbility.isDefined(name))
			throw UNKNOWN_ABILITY.create();
		final IAbility ability = IAbility.fromName(name);
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		specs.forEach(s -> s.resetAbility(ability));
		cc.getSource().sendSuccess(
		  ttc(
			 "commands.aerobaticelytra.ability.reset.success", specs.size())
			 .append("\n")
			 .append(ttc(
				"commands.aerobaticelytra.ability.reset.ability", ability.getDisplayName(),
				displayFloat(ability.getDefault()))), true
		);
		return 0;
	}
	
	public static int resetAbilities(CommandContext<CommandSourceStack> context)
	  throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(context);
		specs.forEach(
		  s -> AerobaticElytraRegistries.getAbilities().values().forEach(s::resetAbility));
		MutableComponent msg = ttc(
		  "commands.aerobaticelytra.ability.reset.all.success",
		  AerobaticElytraRegistries.getAbilities().values().size(), specs.size());
		for (IAbility ability : AerobaticElytraRegistries.getAbilities().values())
			msg = msg.append("\n").append(
			  ttc("commands.aerobaticelytra.ability.reset.ability", ability.getDisplayName(),
			      displayFloat(ability.getDefault())));
		context.getSource().sendSuccess(msg, true);
		return 0;
	}
	
	public static int removeAbilities(
	  CommandContext<CommandSourceStack> cc
	) throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		specs.forEach(s -> {
			AerobaticElytraRegistries.getAbilities().values().forEach(s::removeAbility);
			s.getUnknownAbilities().clear();
		});
		cc.getSource().sendSuccess(
		  ttc("commands.aerobaticelytra.ability.remove.all.success", specs.size()), true
		);
		return 0;
	}
	
	private static int removeUnknownAbilities(
	  CommandContext<CommandSourceStack> cc
	) throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		specs.forEach(s -> s.getUnknownAbilities().clear());
		cc.getSource().sendSuccess(
		  ttc("commands.aerobaticelytra.ability.remove.all.unknown.success", specs.size()), true);
		return 0;
	}
	
	public static int removeAbility(
	  CommandContext<CommandSourceStack> cc, String name
	) throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		if (!IAbility.isDefined(name))
			throw UNKNOWN_ABILITY.create();
		IAbility ability = IAbility.fromName(name);
		final long count = specs.stream().map(
		  s -> s.removeAbility(ability)).filter(Objects::nonNull).count();
		cc.getSource().sendSuccess(
		  ttc("commands.aerobaticelytra.ability.remove.success",
		      ability.getDisplayName(), count, specs.size()), true);
		return 0;
	}
	
	public static int removeUnknownAbility(
	  CommandContext<CommandSourceStack> cc, String name
	) throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		final long count = specs.stream().map(
		  s -> s.getUnknownAbilities().remove(name)).filter(Objects::nonNull).count();
		cc.getSource().sendSuccess(
		  ttc("commands.aerobaticelytra.ability.remove.success.unknown",
		      name, count, specs.size()), true);
		return 0;
	}
	
	public static int enableDebug(CommandContext<CommandSourceStack> context, boolean enable) {
		try {
			Debug.toggleDebug(context.getSource().getPlayerOrException(), enable);
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}
	
	public static int enableParticles(CommandContext<CommandSourceStack> context, boolean enable) {
		Debug.enableParticles(enable);
		return 0;
	}
	
	public static int invertFreeze(CommandContext<CommandSourceStack> context, boolean invert) {
		Debug.setInvertFreeze(invert);
		return 0;
	}
	
	public static int giveDebugWing(CommandContext<CommandSourceStack> context) {
		try {
			final ServerPlayer player = context.getSource().getPlayerOrException();
			final ItemStack debugWing = AerobaticElytraWingItem.createDebugWing();
			if (!player.getInventory().add(debugWing))
				player.drop(debugWing, false);
			return 0;
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public static int listPacks(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		final List<BundledDatapack> packs = getAvailablePacks(source)
		  .values().stream().sorted(Comparator.comparing(bd -> bd.name)).toList();
		if (packs.isEmpty()) {
			source.sendFailure(ttc("commands.aerobaticelytra.datapack.list.empty"));
		} else {
			MutableComponent msg = ttc("commands.aerobaticelytra.datapack.list.success");
			for (BundledDatapack pack : packs) msg = msg.append("\n  ").append(
			  pack.getDisplayName().withStyle(s -> {
				  boolean isInstalled = isPackInstalled(source, pack.getTitle());
				  boolean isEnabled = isInstalled && source.getServer().getPackRepository()
				    .getSelectedPacks().stream().anyMatch(p -> p.getId().equals(pack.getPackName()));
				  s = s.withColor(isInstalled? isEnabled? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA)
				    .withHoverEvent(new HoverEvent(
				      HoverEvent.Action.SHOW_TEXT, stc(pack.getDescription()).append("\n")
				      .append(ttc("commands.aerobaticelytra.datapack.list.link."
				                  + (isInstalled? isEnabled? "disable" : "enable" : "install")))));
				  String command =
				    isInstalled
				    ? "/datapack " + (isEnabled? "disable" : "enable") + " \"" + pack.getPackName() + "\""
				    : "/aerobaticelytra datapack install " + StringArgumentType.escapeIfRequired(pack.getTitle());
				  s = s.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
				  return s;
			  }));
			source.sendSuccess(msg, true);
		}
		return 0;
	}
	
	public static String packPrefix = AerobaticElytra.MOD_ID + " - ";
	
	public static boolean isPackInstalled(CommandSourceStack source, String name) {
		final Map<String, BundledDatapack> availablePacks = getAvailablePacksByTitle(source);
		if (!availablePacks.containsKey(name))
			source.sendFailure(ttc("commands.aerobaticelytra.datapack.install.unknown"));
		Path datapacksFolder = source.getServer().getWorldPath(LevelResource.DATAPACK_DIR);
		final Path destination = datapacksFolder.resolve(packPrefix + name);
		return destination.toFile().exists();
	}
	
	public static int installPack(CommandContext<CommandSourceStack> context, String name) {
		CommandSourceStack source = context.getSource();
		final Map<String, BundledDatapack> availablePacks = getAvailablePacksByTitle(source);
		if (!availablePacks.containsKey(name))
			source.sendFailure(ttc("commands.aerobaticelytra.datapack.install.unknown"));
		final BundledDatapack pack = availablePacks.get(name);
		Path datapacksFolder = source.getServer().getWorldPath(LevelResource.DATAPACK_DIR);
		final Path destination = datapacksFolder.resolve(packPrefix + name);
		final String enableCommandText = "/datapack enable \"file/" + destination.getFileName() + "\"";
		Component enableCommand = ComponentUtils.wrapInSquareBrackets(
		  stc(ellipsis(enableCommandText, 40)).withStyle(
			 style -> style.withColor(ChatFormatting.GREEN).withClickEvent(
				  new ClickEvent(Action.SUGGEST_COMMAND, enableCommandText))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, stc(enableCommandText).withStyle(ChatFormatting.AQUA)))));
		if (destination.toFile().exists()) {
			source.sendFailure(ttc("commands.aerobaticelytra.datapack.install.overwrite", enableCommand));
			return 1;
		}
		final Path anchor = new File(DATAPACK_LOCATION).toPath().resolve(pack.name);
		try {
			for (Map.Entry<ResourceLocation, InputStream> entry : pack.getStreams().entrySet()) {
				final File dest = destination.resolve(
				  anchor.relativize(new File(entry.getKey().getPath()).toPath())).toFile();
				assert dest.getParentFile().mkdirs();
				assert dest.createNewFile();
				FileUtils.copyInputStreamToFile(entry.getValue(), dest);
			}
		} catch (IOException | AssertionError e) {
			e.printStackTrace();
			source.sendFailure(ttc("commands.aerobaticelytra.datapack.install.failure.copy", e.getLocalizedMessage()));
			try { // Undo
				FileUtils.deleteDirectory(destination.toFile());
			} catch (IOException f) {
				f.printStackTrace();
				source.sendFailure(ttc("commands.aerobaticelytra.datapack.install.failure.undo", f.getLocalizedMessage()));
			}
			return 2;
		}
		
		// Refresh the pack list so that '/datapack enable' works without calling '/datapack list' before
		source.getServer().getPackRepository().reload();
		
		// Send feedback
		source.sendSuccess(
		  ttc("commands.aerobaticelytra.datapack.install.success", enableCommand), true);
		return 0;
	}
	
	public static String ellipsis(String str, int length) {
		return str.length() < length? str : str.substring(0, length - 3) + "...";
	}
	
	public static Map<String, BundledDatapack> getAvailablePacksByTitle(CommandSourceStack source) {
		return getAvailablePacks(source).values().stream()
		  .collect(Collectors.toMap(BundledDatapack::getTitle, bd -> bd, (a, b) -> a));
	}
	
	public static Map<String, BundledDatapack> getAvailablePacks(CommandSourceStack source) {
		final Pack pack = source.getServer().getPackRepository().getPack("mod:" + AerobaticElytra.MOD_ID);
		
		if (pack == null) {
			LOGGER.warn("Could not find mod datapack");
			return Collections.emptyMap();
		}
		
		try (final PackResources resourcePack = pack.open()) {
			return resourcePack.getResources(
				 PackType.SERVER_DATA, AerobaticElytra.MOD_ID, "datapacks", s -> {
					 String path = normalizePath(s.getPath());
					 return path.lastIndexOf("/") == 9;
				 }
			).stream().map(rl -> new BundledDatapack(
			  resourcePack, new ResourceLocation(rl.getNamespace(), normalizePath(rl.getPath())))
			).collect(Collectors.toMap(bd -> bd.name, bd -> bd, (a, b) -> a));
		}
	}
	
	public static String normalizePath(String path) {
		return path.replace('\\', '/').replaceAll("^/|/$", "");
	}
	
	public static class BundledDatapack {
		public final String name;
		public final ResourceLocation location;
		public final PackResources pack;
		protected final String description;
		protected final MutableComponent title;
		
		public BundledDatapack(PackResources pack, ResourceLocation location) {
			this.pack = pack;
			this.location = location;
			final String[] split = location.getPath().split("/");
			name = split[split.length - 1];
			String title = name;
			String description = "";
			try {
				final JsonParser parser = new JsonParser();
				final JsonElement json = parser.parse(new InputStreamReader(
				  pack.getResource(PackType.SERVER_DATA, getMcMetaLocation())));
				try {
					if (json.isJsonObject()) {
						final JsonObject packInfo = GsonHelper.getAsJsonObject(json.getAsJsonObject(), "pack");
						title = GsonHelper.getAsString(packInfo, "title");
						description = GsonHelper.getAsString(packInfo, "description");
					}
				} catch (JsonSyntaxException ignored) {}
			} catch (IOException ignored) {}
			this.title = Component.literal(title);
			this.description = description;
		}
		
		public String getTitle() {
			return title.getString();
		}
		
		public String getPackName() {
			return "file/" + packPrefix + getTitle();
		}
		
		public String getDescription() {
			return description;
		}
		
		public MutableComponent getDisplayName() {
			return title;
		}
		
		public ResourceLocation getMcMetaLocation() {
			return new ResourceLocation(location.getNamespace(), location.getPath() + "/pack.mcmeta");
		}
		
		public Collection<ResourceLocation> getAllResourceLocations() {
			final Collection<ResourceLocation> locations = pack.getResources(
			  PackType.SERVER_DATA, location.getNamespace(),
			  location.getPath(), s -> !s.getPath().equals(location.getPath()));
			locations.add(getMcMetaLocation());
			return locations;
		}
		
		public Map<ResourceLocation, InputStream> getStreams() {
			Map<ResourceLocation, InputStream> streams = new HashMap<>();
			for (ResourceLocation rl : getAllResourceLocations()) {
				try {
					streams.put(rl, pack.getResource(PackType.SERVER_DATA, rl));
				} catch (IOException e) {
					LOGGER.warn("Could not get resource stream for bundled datapack resource " + rl);
					e.printStackTrace();
				}
			}
			return streams;
		}
	}
}
