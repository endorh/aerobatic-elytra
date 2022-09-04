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
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.storage.FolderName;
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
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;
import static net.minecraft.command.arguments.EntityArgument.entities;
import static net.minecraft.command.arguments.EntityArgument.entity;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class AerobaticElytraCommand {
	private static final String DATAPACK_LOCATION = "datapacks";
	
	public static final SuggestionProvider<CommandSource> SUGGEST_PACKS =
	  ((context, builder) -> ISuggestionProvider.suggest(
	    getAvailablePacks(context.getSource()).values().stream()
	      .filter(bd -> !isPackInstalled(context.getSource(), bd.getTitle()))
	      .map(bd -> escapeIfRequired(bd.getTitle())), builder));
	public static final SuggestionProvider<CommandSource> SUGGEST_ABILITIES =
	  ((context, builder) -> ISuggestionProvider.suggest(
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
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		LiteralArgumentBuilder<CommandSource> installDatapackCommand =
		  literal("aerobaticelytra")
		    .requires(cs -> cs.hasPermissionLevel(2))
		    .then(
		      literal("datapack").then(
		        literal("install").then(
		          argument("datapack", string())
		            .suggests(SUGGEST_PACKS)
		            .executes(cc -> installPack(cc, getString(cc, "datapack"))))
		      ).then(literal("list").executes(AerobaticElytraCommand::listPacks))
		    ).then(
		      literal("debug").then(
				  literal("show").executes(cc -> enableDebug(cc, true))
		      ).then(
				  literal("hide").executes(cc -> enableDebug(cc, false))
		      ).then(
				  literal("give").executes(AerobaticElytraCommand::giveDebugWing))
		  ).then(
		    literal("ability").then(
		      literal("get").then(
		        argument("target", entity())
		          .executes(AerobaticElytraCommand::getAbilities).then(
			         argument("ability_name", optionallyQualified())
			           .suggests(SUGGEST_ABILITIES)
			           .executes(cc -> getAbility(
			             cc, getString(cc, "ability_name")))))
		    ).then(
		      literal("set").then(
			     argument("target", entities()).then(
				    argument("ability_name", optionallyQualified())
				      .suggests(SUGGEST_ABILITIES).then(
					     argument("ability_value", floatArg()).executes(
						    cc -> setAbility(
						      cc, getString(cc, "ability_name"), getFloat(cc, "ability_value"))))))
		    ).then(
		      literal("reset").then(
			     argument("target", entities())
				    .executes(AerobaticElytraCommand::resetAbilities)
				    .then(
				      argument("ability_name", optionallyQualified())
					     .suggests(SUGGEST_ABILITIES)
					     .executes(
						    cc -> resetAbility(cc, getString(cc, "ability_name")))))
		    ).then(
		      literal("remove").then(
			     argument("target", entities())
				    .executes(AerobaticElytraCommand::removeAbilities)
				    .then(
				      argument("ability_name", optionallyQualified())
					     .suggests(SUGGEST_ABILITIES)
					     .executes(cc -> removeAbility(cc, getString(cc, "ability_name")))))
		    ).then(
		      literal("unknown").then(
			     literal("get").then(
				    argument("target", entity())
				      .executes(AerobaticElytraCommand::getUnknownAbilities).then(
				      argument("ability_name", optionallyQualified())
					     .executes(cc -> getUnknownAbility(cc, getString(cc, "ability_name")))))
		      ).then(
			     literal("set").then(
				    argument("target", entities()).then(
				      argument("ability_name", optionallyQualified()).then(
				        argument("ability_value", floatArg())
				          .executes(cc -> setUnknownAbility(
				            cc, getString(cc, "ability_name"), getFloat(cc, "ability_value"))))))
		      ).then(
			     literal("remove").then(
			       argument("target", entities())
			         .executes(AerobaticElytraCommand::removeUnknownAbilities).then(
			           argument("ability_name", optionallyQualified())
			             .executes(cc -> removeUnknownAbility(cc, getString(cc, "ability_name"))))))));
		dispatcher.register(installDatapackCommand);
	}
	
	public static Entity getTarget(
	  CommandContext<CommandSource> cc
	) throws CommandSyntaxException {
		return EntityArgument.getEntity(cc, "target");
	}
	
	public static List<IElytraSpec> getElytraSpecs(CommandContext<CommandSource> cc)
	  throws CommandSyntaxException {
		final List<IElytraSpec> list = EntityArgument.getEntities(cc, "target").stream()
		  .filter(e -> e instanceof LivingEntity)
		  .map(e -> ((LivingEntity) e).getItemStackFromSlot(EquipmentSlotType.CHEST))
		  .filter(AerobaticElytraLogic::isAerobaticElytra)
		  .map(ElytraSpecCapability::getElytraSpecOrDefault)
		  .collect(Collectors.toList());
		if (list.isEmpty())
			throw NO_ELYTRA_HOLDING_TARGETS.create();
		return list;
	}
	
	public static IElytraSpec getElytraSpec(CommandContext<CommandSource> cc)
	  throws CommandSyntaxException{
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		if (specs.size() != 1)
			throw new IllegalStateException("Expected a single entity target");
		return specs.get(0);
	}
	
	public static ITextComponent displayFloat(float value) {
		return new StringTextComponent(String.format("%5.2f", value))
		  .mergeStyle(TextFormatting.AQUA);
	}
	
	public static int getAbility(
	  CommandContext<CommandSource> cc, String name
	) throws CommandSyntaxException {
		final IElytraSpec spec = getElytraSpec(cc);
		if (!IAbility.isDefined(name))
			throw UNKNOWN_ABILITY.create();
		final IAbility ability = IAbility.fromName(name);
		if (spec.hasAbility(ability)) {
			final float value = spec.getAbility(ability);
			cc.getSource().sendFeedback(
			  ttc("commands.aerobaticelytra.ability.get.success", getTarget(cc).getDisplayName())
				 .appendString("\n")
				 .append(ttc("commands.aerobaticelytra.ability.get.ability",
				             ability.getDisplayName(), displayFloat(value))), true);
		} else {
			cc.getSource().sendFeedback(
			  ttc("commands.aerobaticelytra.ability.get.default",
			      ability.getDisplayName(), getTarget(cc).getDisplayName(),
			      displayFloat(ability.getDefault())), true);
		}
		return 0;
	}
	
	private static int getUnknownAbility(
	  CommandContext<CommandSource> cc, String name
	) throws CommandSyntaxException {
		final IElytraSpec spec = getElytraSpec(cc);
		if (spec.getUnknownAbilities().containsKey(name)) {
			final float value = spec.getUnknownAbilities().get(name);
			cc.getSource().sendFeedback(
			  ttc("commands.aerobaticelytra.ability.get.success.unknown", getTarget(cc).getDisplayName())
				 .appendString("\n")
				 .append(ttc("commands.aerobaticelytra.ability.get.ability.unknown",
				             name, displayFloat(value))), true);
		} else {
			cc.getSource().sendErrorMessage(
			  ttc("commands.aerobaticelytra.ability.get.failure.unknown", name, getTarget(cc).getDisplayName()));
		}
		return 0;
	}
	
	public static int getAbilities(CommandContext<CommandSource> cc)
	  throws CommandSyntaxException { return getAbilities(cc, true); }
	
	public static int getAbilities(CommandContext<CommandSource> cc, boolean show_unknown)
	  throws CommandSyntaxException { return getAbilities(cc, true, show_unknown); }
	
	public static int getUnknownAbilities(CommandContext<CommandSource> cc)
	  throws CommandSyntaxException {
		return getAbilities(cc, false, true);
	}
	
	public static int getAbilities(CommandContext<CommandSource> cc, boolean show_known, boolean show_unknown)
	  throws CommandSyntaxException {
		IElytraSpec spec = getElytraSpec(cc);
		final ITextComponent name = getTarget(cc).getDisplayName();
		IFormattableTextComponent msg = stc("");
		if (show_known) {
			msg = msg.append(
			  ttc("commands.aerobaticelytra.ability.get.all.success",
			      spec.getAbilities().size(), name));
			for (IAbility ability : AerobaticElytraRegistries.getAbilities().values())
				msg = msg.appendString("\n").append(
				  ttc("commands.aerobaticelytra.ability.get.ability", ability.getDisplayName(),
				      displayFloat(spec.getAbility(ability))));
		}
		if (show_unknown) {
			final Map<String, Float> unknown = spec.getUnknownAbilities();
			if (unknown.isEmpty()) {
				msg = msg.appendString("\n").append(
				  ttc("commands.aerobaticelytra.ability.get.all.unknown.empty", name));
			} else {
				msg = msg.appendString("\n").append(
				  ttc("commands.aerobaticelytra.ability.get.all.unknown", unknown.size(), name));
				for (Entry<String, Float> entry : unknown.entrySet()) {
					msg = msg.appendString("\n").append(
					  ttc("commands.aerobaticelytra.ability.get.ability.unknown",
					      entry.getKey(), displayFloat(entry.getValue())));
				}
			}
		}
		cc.getSource().sendFeedback(msg, true);
		return 0;
	}
	
	public static int setAbility(CommandContext<CommandSource> cc, String name, float value)
	  throws CommandSyntaxException {
		if (!IAbility.isDefined(name))
			throw UNKNOWN_ABILITY.create();
		final IAbility ability = IAbility.fromName(name);
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		specs.forEach(s -> s.setAbility(ability, value));
		cc.getSource().sendFeedback(
		  ttc("commands.aerobaticelytra.ability.set.success", specs.size())
		    .appendString("\n")
		    .append(ttc(
		      "commands.aerobaticelytra.ability.set.ability", ability.getDisplayName(),
		      displayFloat(value))), true);
		return 0;
	}
	
	public static int setUnknownAbility(
	  CommandContext<CommandSource> cc, String name, float value
	) throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		specs.forEach(s -> s.getUnknownAbilities().put(name, value));
		cc.getSource().sendFeedback(
		  ttc("commands.aerobaticelytra.ability.set.success.unknown", specs.size())
		    .appendString("\n").append(
		      ttc("commands.aerobaticelytra.ability.set.ability.unknown", name, displayFloat(value))),
		  true
		);
		return 0;
	}
	
	public static int resetAbility(CommandContext<CommandSource> cc, String name)
	  throws CommandSyntaxException {
		if (!IAbility.isDefined(name))
			throw UNKNOWN_ABILITY.create();
		final IAbility ability = IAbility.fromName(name);
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		specs.forEach(s -> s.resetAbility(ability));
		cc.getSource().sendFeedback(
		  ttc(
			 "commands.aerobaticelytra.ability.reset.success", specs.size())
			 .appendString("\n")
			 .append(ttc(
				"commands.aerobaticelytra.ability.reset.ability", ability.getDisplayName(),
				displayFloat(ability.getDefault()))), true
		);
		return 0;
	}
	
	public static int resetAbilities(CommandContext<CommandSource> context)
	  throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(context);
		specs.forEach(
		  s -> AerobaticElytraRegistries.getAbilities().values().forEach(s::resetAbility));
		IFormattableTextComponent msg = ttc(
		  "commands.aerobaticelytra.ability.reset.all.success",
		  AerobaticElytraRegistries.getAbilities().values().size(), specs.size());
		for (IAbility ability : AerobaticElytraRegistries.getAbilities().values())
			msg = msg.appendString("\n").append(
			  ttc("commands.aerobaticelytra.ability.reset.ability", ability.getDisplayName(),
			      displayFloat(ability.getDefault())));
		context.getSource().sendFeedback(msg, true);
		return 0;
	}
	
	public static int removeAbilities(
	  CommandContext<CommandSource> cc
	) throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		specs.forEach(s -> {
			AerobaticElytraRegistries.getAbilities().values().forEach(s::removeAbility);
			s.getUnknownAbilities().clear();
		});
		cc.getSource().sendFeedback(
		  ttc("commands.aerobaticelytra.ability.remove.all.success", specs.size()), true
		);
		return 0;
	}
	
	private static int removeUnknownAbilities(
	  CommandContext<CommandSource> cc
	) throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		specs.forEach(s -> s.getUnknownAbilities().clear());
		cc.getSource().sendFeedback(
		  ttc("commands.aerobaticelytra.ability.remove.all.unknown.success", specs.size()), true);
		return 0;
	}
	
	public static int removeAbility(
	  CommandContext<CommandSource> cc, String name
	) throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		if (!IAbility.isDefined(name))
			throw UNKNOWN_ABILITY.create();
		IAbility ability = IAbility.fromName(name);
		final long count = specs.stream().map(
		  s -> s.removeAbility(ability)).filter(Objects::nonNull).count();
		cc.getSource().sendFeedback(
		  ttc("commands.aerobaticelytra.ability.remove.success",
		      ability.getDisplayName(), count, specs.size()), true);
		return 0;
	}
	
	public static int removeUnknownAbility(
	  CommandContext<CommandSource> cc, String name
	) throws CommandSyntaxException {
		final List<IElytraSpec> specs = getElytraSpecs(cc);
		final long count = specs.stream().map(
		  s -> s.getUnknownAbilities().remove(name)).filter(Objects::nonNull).count();
		cc.getSource().sendFeedback(
		  ttc("commands.aerobaticelytra.ability.remove.success.unknown",
		      name, count, specs.size()), true);
		return 0;
	}
	
	public static int enableDebug(CommandContext<CommandSource> context, boolean enable) {
		try {
			Debug.toggleDebug(context.getSource().asPlayer(), enable);
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}
	
	public static int giveDebugWing(CommandContext<CommandSource> context) {
		try {
			final ServerPlayerEntity player = context.getSource().asPlayer();
			final ItemStack debugWing = AerobaticElytraWingItem.createDebugWing();
			if (!player.inventory.addItemStackToInventory(debugWing))
				player.dropItem(debugWing, false);
			return 0;
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public static int listPacks(CommandContext<CommandSource> context) {
		CommandSource source = context.getSource();
		final List<BundledDatapack> packs = getAvailablePacks(source)
		  .values().stream().sorted(Comparator.comparing(bd -> bd.name))
		  .collect(Collectors.toList());
		if (packs.isEmpty()) {
			source.sendErrorMessage(ttc("commands.aerobaticelytra.datapack.list.empty"));
		} else {
			IFormattableTextComponent msg = ttc("commands.aerobaticelytra.datapack.list.success");
			for (BundledDatapack pack : packs)
				msg = msg.appendString("\n  ").append(
				  pack.getDisplayName().modifyStyle(
				    s -> {
						 boolean isInstalled = isPackInstalled(source, pack.getTitle());
						 boolean isEnabled = isInstalled
						   && source.getServer().getResourcePacks().getEnabledPacks().stream().anyMatch(
							  p -> p.getName().equals(pack.getPackName()));
					    s = s.setFormatting(isInstalled? isEnabled? TextFormatting.GREEN : TextFormatting.DARK_RED :TextFormatting.AQUA)
					      .setHoverEvent(new HoverEvent(
						     HoverEvent.Action.SHOW_TEXT, stc(pack.getDescription()).appendString("\n")
					        .append(ttc("commands.aerobaticelytra.datapack.list.link."
					                    + (isInstalled? isEnabled? "disable" : "enable" : "install")))));
						 String command =
						   isInstalled ?
						   "/datapack " + (isEnabled? "disable" : "enable") + " \"" + pack.getPackName() + "\""
						   : "/aerobaticelytra datapack install " + StringArgumentType.escapeIfRequired(pack.getTitle());
						 s = s.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
					    return s;
				    }));
			source.sendFeedback(msg, true);
		}
		return 0;
	}
	
	public static String packPrefix = AerobaticElytra.MOD_ID + " - ";
	
	public static boolean isPackInstalled(CommandSource source, String name) {
		final Map<String, BundledDatapack> availablePacks = getAvailablePacksByTitle(source);
		if (!availablePacks.containsKey(name))
			source.sendErrorMessage(ttc("commands.aerobaticelytra.datapack.install.unknown"));
		Path datapacksFolder = source.getServer().func_240776_a_(FolderName.DATAPACKS);
		final Path destination = datapacksFolder.resolve(packPrefix + name);
		return destination.toFile().exists();
	}
	
	public static int installPack(CommandContext<CommandSource> context, String name) {
		CommandSource source = context.getSource();
		final Map<String, BundledDatapack> availablePacks = getAvailablePacksByTitle(source);
		if (!availablePacks.containsKey(name))
			source.sendErrorMessage(ttc("commands.aerobaticelytra.datapack.install.unknown"));
		final BundledDatapack pack = availablePacks.get(name);
		Path datapacksFolder = source.getServer().func_240776_a_(FolderName.DATAPACKS);
		final Path destination = datapacksFolder.resolve(packPrefix + name);
		final String enableCommandText = "/datapack enable \"file/" + destination.getFileName() + "\"";
		ITextComponent enableCommand = TextComponentUtils.wrapWithSquareBrackets(
		  stc(ellipsis(enableCommandText, 40)).modifyStyle(
			 style -> style.setFormatting(TextFormatting.GREEN).setClickEvent(
				  new ClickEvent(Action.SUGGEST_COMMAND, enableCommandText))
				.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, stc(enableCommandText).mergeStyle(TextFormatting.AQUA)))));
		if (destination.toFile().exists()) {
			source.sendErrorMessage(ttc("commands.aerobaticelytra.datapack.install.overwrite", enableCommand));
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
			source.sendErrorMessage(ttc("commands.aerobaticelytra.datapack.install.failure.copy", e.getLocalizedMessage()));
			try { // Undo
				FileUtils.deleteDirectory(destination.toFile());
			} catch (IOException f) {
				f.printStackTrace();
				source.sendErrorMessage(ttc("commands.aerobaticelytra.datapack.install.failure.undo", f.getLocalizedMessage()));
			}
			return 2;
		}
		
		// Refresh the pack list so that '/datapack enable' works without calling '/datapack list' before
		source.getServer().getResourcePacks().reloadPacksFromFinders();
		
		// Send feedback
		source.sendFeedback(
		  ttc("commands.aerobaticelytra.datapack.install.success", enableCommand), true);
		return 0;
	}
	
	public static String ellipsis(String str, int length) {
		return str.length() < length? str : str.substring(0, length - 3) + "...";
	}
	
	public static Map<String, BundledDatapack> getAvailablePacksByTitle(CommandSource source) {
		return getAvailablePacks(source).values().stream()
		  .collect(Collectors.toMap(BundledDatapack::getTitle, bd -> bd, (a, b) -> a));
	}
	
	public static Map<String, BundledDatapack> getAvailablePacks(CommandSource source) {
		final ResourcePackInfo pack = source.getServer().getResourcePacks()
		  .getPackInfo("mod:" + AerobaticElytra.MOD_ID);
		
		if (pack == null) {
			LOGGER.warn("Could not find mod datapack");
			return Collections.emptyMap();
		}
		
		final IResourcePack resourcePack = pack.getResourcePack();
		
		return resourcePack.getAllResourceLocations(
		  ResourcePackType.SERVER_DATA, AerobaticElytra.MOD_ID, "datapacks", 2,
		  s -> !stripPath(s).equals("datapacks")
		).stream()
		  .map(rl -> new BundledDatapack(
		    resourcePack, new ResourceLocation(rl.getNamespace(), stripPath(rl.getPath()))))
		  .collect(Collectors.toMap(bd -> bd.name, bd -> bd, (a, b) -> a));
	}
	
	public static String stripPath(String path) {
		return path.replaceAll("^[\\\\/]|[\\\\/]$", "");
	}
	
	public static class BundledDatapack {
		public final String name;
		public final ResourceLocation location;
		public final IResourcePack pack;
		protected final String description;
		protected final IFormattableTextComponent title;
		
		public BundledDatapack(IResourcePack pack, ResourceLocation location) {
			this.pack = pack;
			this.location = location;
			final String[] split = location.getPath().split("/");
			name = split[split.length - 1];
			String title = name;
			String description = "";
			try {
				final JsonParser parser = new JsonParser();
				final JsonElement json = parser.parse(new InputStreamReader(
				  pack.getResourceStream(ResourcePackType.SERVER_DATA, getMcMetaLocation())));
				try {
					if (json.isJsonObject()) {
						final JsonObject packInfo = JSONUtils.getJsonObject(json.getAsJsonObject(), "pack");
						title = JSONUtils.getString(packInfo, "title");
						description = JSONUtils.getString(packInfo, "description");
					}
				} catch (JsonSyntaxException ignored) {}
			} catch (IOException ignored) {}
			this.title = new StringTextComponent(title);
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
		
		public IFormattableTextComponent getDisplayName() {
			return title;
		}
		
		public ResourceLocation getMcMetaLocation() {
			return new ResourceLocation(location.getNamespace(), location.getPath() + "/pack.mcmeta");
		}
		
		public Collection<ResourceLocation> getAllResourceLocations() {
			final Collection<ResourceLocation> locations = pack.getAllResourceLocations(
			  ResourcePackType.SERVER_DATA, location.getNamespace(),
			  location.getPath(), Integer.MAX_VALUE, s -> !s.equals(location.getPath()));
			locations.add(getMcMetaLocation());
			return locations;
		}
		
		public Map<ResourceLocation, InputStream> getStreams() {
			Map<ResourceLocation, InputStream> streams = new HashMap<>();
			for (ResourceLocation rl : getAllResourceLocations()) {
				try {
					streams.put(rl, pack.getResourceStream(ResourcePackType.SERVER_DATA, rl));
				} catch (IOException e) {
					LOGGER.warn("Could not get resource stream for bundled datapack resource " + rl);
					e.printStackTrace();
				}
			}
			return streams;
		}
	}
}
