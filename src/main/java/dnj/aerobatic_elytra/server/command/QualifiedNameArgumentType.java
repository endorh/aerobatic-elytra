package dnj.aerobatic_elytra.server.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.arguments.IArgumentSerializer;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

import static dnj.endor8util.util.TextUtil.ttc;

public class QualifiedNameArgumentType implements ArgumentType<String> {
	private static final SimpleCommandExceptionType MISSING_QUALIFIER =
	  new SimpleCommandExceptionType(ttc("commands.aerobatic-elytra.error.missing_qualifier"));
	private static final SimpleCommandExceptionType UNEXPECTED_QUALIFIER =
	  new SimpleCommandExceptionType(ttc("commands.aerobatic-elytra.error.unexpected_qualifier"));
	private static final SimpleCommandExceptionType UNEXPECTED_SEPARATOR =
	  new SimpleCommandExceptionType(ttc("commands.aerobatic-elytra.error.unexpected_separator"));
	
	protected final QualifiedNameType type;
	
	public static QualifiedNameArgumentType optionallyQualified() {
		return new QualifiedNameArgumentType(QualifiedNameType.OPTIONALLY_QUALIFIED_NAME);
	}
	
	public static QualifiedNameArgumentType qualified() {
		return new QualifiedNameArgumentType(QualifiedNameType.QUALIFIED_NAME);
	}
	
	public static QualifiedNameArgumentType unqualified() {
		return new QualifiedNameArgumentType(QualifiedNameType.UNQUALIFIED_NAME);
	}
	
	private QualifiedNameArgumentType(QualifiedNameType type) {
		this.type = type;
	}
	
	/**
	 * Get a name argument<br>
	 * Equivalent to {@link com.mojang.brigadier.arguments.StringArgumentType#getString(CommandContext, String)}
	 */
	public static String getName(final CommandContext<?> context, final String name) {
		return context.getArgument(name, String.class);
	}
	
	@Override public String parse(StringReader reader) throws CommandSyntaxException {
		final String name = reader.getString().substring(reader.getCursor())
		  .split("\\s", 1)[0];
		reader.setCursor(reader.getCursor() + name.length());
		if (!type.optionalQualifier && !name.contains(":"))
			throw MISSING_QUALIFIER.createWithContext(reader);
		else if (!type.allowQualifier && name.contains(":"))
			throw UNEXPECTED_QUALIFIER.createWithContext(reader);
		else if (name.contains(":") && name.substring(name.indexOf(':')+1).contains(":"))
			throw UNEXPECTED_SEPARATOR.createWithContext(reader);
		return name;
	}
	
	@Override public Collection<String> getExamples() {
		if (!type.allowQualifier)
			return Arrays.asList("name", "hyphened-name");
		if (type.optionalQualifier)
			return Arrays.asList("name", "qualified:name");
		return Arrays.asList("qualified:name", "qualified:hyphened-name");
	}
	
	public enum QualifiedNameType {
		UNQUALIFIED_NAME(false, true),
		QUALIFIED_NAME(true, false),
		OPTIONALLY_QUALIFIED_NAME(true, true);
		
		public final boolean allowQualifier;
		public final boolean optionalQualifier;
		
		QualifiedNameType(boolean allowQualifier, boolean optionalQualifier) {
			this.allowQualifier = allowQualifier;
			this.optionalQualifier = optionalQualifier;
		}
	}
	
	public static class Serializer implements IArgumentSerializer<QualifiedNameArgumentType> {
		@Override public void write(
		  @NotNull QualifiedNameArgumentType arg, @NotNull PacketBuffer buf
		) {
			buf.writeEnumValue(arg.type);
		}
		
		@Override public @NotNull QualifiedNameArgumentType read(@NotNull PacketBuffer buf) {
			return new QualifiedNameArgumentType(buf.readEnumValue(QualifiedNameType.class));
		}
		
		@Override public void write(
		  @NotNull QualifiedNameArgumentType arg, @NotNull JsonObject json
		) {
			json.add("type", new JsonPrimitive(arg.type.name().toLowerCase()));
		}
	}
}
