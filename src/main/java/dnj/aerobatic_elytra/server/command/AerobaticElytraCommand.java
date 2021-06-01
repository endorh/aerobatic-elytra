package dnj.aerobatic_elytra.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.common.AerobaticElytraLogic;
import dnj.aerobatic_elytra.common.capability.ElytraSpecCapability;
import dnj.aerobatic_elytra.common.capability.IElytraSpec;
import dnj.aerobatic_elytra.common.item.IAbility;
import dnj.aerobatic_elytra.common.registry.ModRegistries;
import dnj.aerobatic_elytra.debug.Debug;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static dnj.endor8util.util.TextUtil.stc;
import static dnj.endor8util.util.TextUtil.ttc;
import static org.apache.commons.lang3.StringUtils.countMatches;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class AerobaticElytraCommand {
	private static final String DATAPACK_LOCATION = "data/aerobatic-elytra/datapacks";
	
	public static final SuggestionProvider<CommandSource> SUGGEST_PACKS =
	  ((context, builder) -> ISuggestionProvider.suggest(
	    getAvailablePacks().stream().map(StringArgumentType::escapeIfRequired),
	    builder));
	public static final SuggestionProvider<CommandSource> SUGGEST_ABILITIES =
	  ((context, builder) -> ISuggestionProvider.suggest(
	    ModRegistries.ABILITY_REGISTRY.getValues().stream().map(IAbility::jsonName),
	    builder));
	public static final SimpleCommandExceptionType NO_ELYTRA_HOLDING_TARGETS =
	  new SimpleCommandExceptionType(ttc(
	    "commands.aerobatic-elytra.error.no_elytra"));
	public static final SimpleCommandExceptionType UNKNOWN_ABILITY =
	  new SimpleCommandExceptionType(ttc(
	    "commands.aerobatic-elytra.error.unknown_ability"));
	
	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		register(event.getDispatcher());
	}
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		LiteralArgumentBuilder<CommandSource> installDatapackCommand =
		  Commands.literal("aerobatic-elytra")
		    .requires(cs -> cs.hasPermissionLevel(2))
		    .then(
		      Commands.literal("datapack").then(
		        Commands.literal("install").then(
		          Commands.argument("datapack", StringArgumentType.string())
		            .suggests(SUGGEST_PACKS)
		            .executes(cc -> installPack(
		              cc, StringArgumentType.getString(cc, "datapack")))))
		    ).then(
		      Commands.literal("debug").then(
		        Commands.argument("enable", BoolArgumentType.bool())
		          .executes(cc -> enableDebug(cc, BoolArgumentType.getBool(cc, "enable"))))
		    ).then(
		      Commands.literal("ability")
		        .then(
		          Commands.literal("get").then(
		            Commands.argument("target", EntityArgument.entity())
		              .executes(AerobaticElytraCommand::getAbilities)
		              .then(
		                Commands.argument("ability_name", StringArgumentType.word())
		                  .suggests(SUGGEST_ABILITIES)
		                  .executes(cc -> getAbility(
		                    cc, StringArgumentType.getString(cc, "ability_name")))))
		        ).then(
		          Commands.literal("set").then(
		            Commands.argument("target", EntityArgument.entities()).then(
		              Commands.argument("ability_name", StringArgumentType.word())
		                .suggests(SUGGEST_ABILITIES)
		                .then(
		                  Commands.argument("ability_value", FloatArgumentType.floatArg()).executes(
		                    cc -> setAbility(
		                      cc, StringArgumentType.getString(cc, "ability_name"),
		                      FloatArgumentType.getFloat(cc, "ability_value"))))))
		      ).then(
		        Commands.literal("reset").then(
		            Commands.argument("target", EntityArgument.entities())
		              .executes(AerobaticElytraCommand::resetAbilities)
		              .then(
			             Commands.argument("ability_name", StringArgumentType.word())
			               .suggests(SUGGEST_ABILITIES)
				            .executes(cc -> resetAbility(cc, StringArgumentType.getString(cc, "ability_name")))
		              )
		          )
		      ));
		dispatcher.register(installDatapackCommand);
	}
	
	public static List<IElytraSpec> getElytraSpecs(CommandContext<CommandSource> context)
	  throws CommandSyntaxException {
		final List<IElytraSpec> list = EntityArgument.getEntities(context, "target").stream()
		  .filter(e -> e instanceof LivingEntity)
		  .map(e -> ((LivingEntity) e).getItemStackFromSlot(EquipmentSlotType.CHEST))
		  .filter(AerobaticElytraLogic::isAerobaticElytra)
		  .map(ElytraSpecCapability::getElytraSpecOrDefault)
		  .collect(Collectors.toList());
		if (list.isEmpty())
			throw NO_ELYTRA_HOLDING_TARGETS.create();
		return list;
	}
	
	public static ITextComponent displayFloat(float value) {
		return new StringTextComponent(String.format("%5.2f", value))
		  .mergeStyle(TextFormatting.AQUA);
	}
	
	public static int getAbility(CommandContext<CommandSource> context, String name)
	  throws CommandSyntaxException {
		if (!IAbility.isDefined(name))
			throw UNKNOWN_ABILITY.create();
		final IAbility ability = IAbility.fromJsonName(name);
		final List<IElytraSpec> specs = getElytraSpecs(context);
		if (specs.size() != 1)
			throw new IllegalStateException("Expected a single entity target");
		final IElytraSpec spec = specs.get(0);
		final float value = spec.getAbility(ability);
		context.getSource().sendFeedback(
		  ttc("commands.aerobatic-elytra.data.get.success")
		    .appendString("\n")
		    .append(ttc("commands.aerobatic-elytra.data.get.ability", ability.getDisplayName(),
		                displayFloat(value))),
		  true);
		return 0;
	}
	
	public static int getAbilities(CommandContext<CommandSource> context)
	  throws CommandSyntaxException {
		IFormattableTextComponent msg = ttc("commands.aerobatic-elytra.data.get.all.success",
		                                    ModRegistries.ABILITY_REGISTRY.getValues().size());
		final List<IElytraSpec> specs = getElytraSpecs(context);
		if (specs.size() != 1)
			throw new IllegalStateException("Excepted a single entity target");
		IElytraSpec spec = specs.get(0);
		for (IAbility ability : ModRegistries.ABILITY_REGISTRY.getValues())
			msg = msg.appendString("\n").append(
			  ttc("commands.aerobatic-elytra.data.get.ability", ability.getDisplayName(),
			      displayFloat(spec.getAbility(ability))));
		context.getSource().sendFeedback(msg, true);
		return 0;
	}
	
	public static int setAbility(CommandContext<CommandSource> context, String name, float value)
	  throws CommandSyntaxException {
		if (!IAbility.isDefined(name))
			throw UNKNOWN_ABILITY.create();
		final IAbility ability = IAbility.fromJsonName(name);
		getElytraSpecs(context).forEach(
		  s -> s.setAbility(ability, value)
		);
		context.getSource().sendFeedback(
		  ttc(
		  "commands.aerobatic-elytra.data.set.success")
		    .appendString("\n")
		    .append(ttc(
		      "commands.aerobatic-elytra.data.set.ability", ability.getDisplayName(),
		      displayFloat(value))), true);
		return 0;
	}
	
	public static int resetAbility(CommandContext<CommandSource> context, String name)
	  throws CommandSyntaxException {
		if (!IAbility.isDefined(name))
			throw UNKNOWN_ABILITY.create();
		final IAbility ability = IAbility.fromJsonName(name);
		getElytraSpecs(context).forEach(
		  s -> s.resetAbility(ability)
		);
		context.getSource().sendFeedback(
		  ttc(
			 "commands.aerobatic-elytra.data.reset.success")
			 .appendString("\n")
			 .append(ttc(
				"commands.aerobatic-elytra.data.reset.ability", ability.getDisplayName(),
				displayFloat(ability.getDefault()))), true
		);
		return 0;
	}
	
	public static int resetAbilities(CommandContext<CommandSource> context)
	  throws CommandSyntaxException {
		getElytraSpecs(context).forEach(
		  s -> ModRegistries.ABILITY_REGISTRY.getValues().forEach(s::resetAbility));
		IFormattableTextComponent msg = ttc("commands.aerobatic-elytra.data.reset.all.success",
		                                    ModRegistries.ABILITY_REGISTRY.getValues().size());
		for (IAbility ability : ModRegistries.ABILITY_REGISTRY.getValues())
			msg = msg.appendString("\n").append(
			  ttc("commands.aerobatic-elytra.data.reset.ability", ability.getDisplayName(),
			      displayFloat(ability.getDefault())));
		context.getSource().sendFeedback(msg, true);
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
	
	public static int installPack(CommandContext<CommandSource> context, String name) {
		CommandSource source = context.getSource();
		if (!getAvailablePacks().contains(name))
			source.sendErrorMessage(ttc("commands.aerobatic-elytra.datapack.install.unknown"));
		Path datapacksFolder = source.getServer().func_240776_a_(FolderName.DATAPACKS);
		final ZipFile file = getJar();
		if (file == null) {
			source.sendErrorMessage(ttc("command.failed"));
			return 0;
		}
		final Path destination = datapacksFolder.resolve(AerobaticElytra.MOD_ID + "_" + name);
		if (destination.toFile().exists()) {
			source.sendErrorMessage(ttc("commands.aerobatic-elytra.datapack.install.overwrite"));
			return 0;
		}
		final String datapackFolder = new File(DATAPACK_LOCATION).toPath().resolve(name).toString();
		try {
			extractArchive(file, datapackFolder, destination);
		} catch (IOException e) {
			e.printStackTrace();
			source.sendErrorMessage(ttc("command.failed"));
			return 0;
		}
		final String command = "/datapack enable \"file/" + destination.getFileName() + "\"";
		ITextComponent suggestedCommand = TextComponentUtils.wrapWithSquareBrackets(
		  stc(command).modifyStyle(
		    style -> style.setFormatting(TextFormatting.GREEN).setClickEvent(
		      new ClickEvent(Action.SUGGEST_COMMAND, command))
		  .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, stc(command).mergeStyle(TextFormatting.AQUA)))));
		source.sendFeedback(
		  ttc("commands.aerobatic-elytra.datapack.install.success", suggestedCommand), true);
		return 1;
	}
	
	public static List<String> getAvailablePacks() {
		ZipFile file = getJar();
		if (file == null)
			return Collections.emptyList();
		return file.stream().filter(
		  entry -> isDirectSubFolder(entry.getName(), DATAPACK_LOCATION)
		).sorted(Comparator.comparing(ZipEntry::getName))
		  .map(entry -> stripSeparator(entry.getName().substring(DATAPACK_LOCATION.length())))
		  .collect(Collectors.toList());
	}
	
	public static void extractArchive(ZipFile jar, String location, Path target) throws IOException {
		final List<ZipEntry> datapackFiles = jar.stream().filter(
		  entry -> isChild(entry.getName(), location)
		).sorted(Comparator.comparing(ZipEntry::getName))
		  .collect(Collectors.toList());
		Path sourceRoot = path(location);
		for (ZipEntry entry : datapackFiles) {
			Path entryTarget = target.resolve(sourceRoot.relativize(path(entry.getName())));
			if (entry.isDirectory()) {
				Files.createDirectory(entryTarget);
			} else {
				Files.copy(jar.getInputStream(entry), entryTarget);
			}
		}
	}
	
	private static Path path(String path) {
		return new File(path).toPath();
	}
	
	private static ZipFile getJar() {
		try {
			final URL res = AerobaticElytraCommand.class
			  .getClassLoader().getResource(DATAPACK_LOCATION);
			URL jar = null;
			if (res != null) {
				String path = res.getPath();
				path = path.substring(
				  0, path.length() - stripSeparator(DATAPACK_LOCATION).length() - 2);
				jar = new URL(path);
			}
			if (jar == null) {
				return null;
				//jar = new File("test.jar").toURI().toURL();
			}
			return new ZipFile(new File(jar.toURI()));
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("SameParameterValue")
	private static boolean isDirectSubFolder(String path, String parent) {
		path = stripSeparator(normalizeSeparator(path));
		parent = stripSeparator(normalizeSeparator(parent));
		return path.startsWith(parent) &&
		       countMatches(path, "/") == countMatches(parent, "/") + 1;
	}
	
	private static boolean isChild(String path, String parent) {
		return stripSeparator(normalizeSeparator(path))
		  .startsWith(stripSeparator(normalizeSeparator(parent)));
	}
	
	private static String normalizeSeparator(String path) {
		return path.replace("\\", "/");
	}
	
	private static String stripSeparator(String path) {
		if (path.startsWith("/"))
			path = path.substring(1);
		if (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		return path;
	}
}
