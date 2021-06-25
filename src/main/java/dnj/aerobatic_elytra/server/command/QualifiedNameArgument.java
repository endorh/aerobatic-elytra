package dnj.aerobatic_elytra.server.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.Arrays;
import java.util.Collection;

import static dnj.endor8util.util.TextUtil.ttc;

public class QualifiedNameArgument implements ArgumentType<String> {
	private static final SimpleCommandExceptionType MISSING_QUALIFIER =
	  new SimpleCommandExceptionType(ttc("commands.aerobatic-elytra.error.missing_qualifier"));
	private static final SimpleCommandExceptionType UNEXPECTED_QUALIFIER =
	  new SimpleCommandExceptionType(ttc("commands.aerobatic-elytra.error.unexpected_qualifier"));
	private static final SimpleCommandExceptionType UNEXPECTED_SEPARATOR =
	  new SimpleCommandExceptionType(ttc("commands.aerobatic-elytra.error.unexpected_separator"));
	
	protected final boolean allowQualifier;
	protected final boolean optionalQualifier;
	
	public static QualifiedNameArgument optionallyQualified() {
		return new QualifiedNameArgument(true, true);
	}
	
	public static QualifiedNameArgument qualified() {
		return new QualifiedNameArgument(true, false);
	}
	
	public static QualifiedNameArgument unqualified() {
		return new QualifiedNameArgument(false, true);
	}
	
	private QualifiedNameArgument(boolean allowQualifier, boolean optionalQualifier) {
		this.allowQualifier = allowQualifier;
		this.optionalQualifier = optionalQualifier;
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
		if (!optionalQualifier && !name.contains(":"))
			throw MISSING_QUALIFIER.createWithContext(reader);
		else if (!allowQualifier && name.contains(":"))
			throw UNEXPECTED_QUALIFIER.createWithContext(reader);
		else if (name.contains(":") && name.substring(name.indexOf(':')+1).contains(":"))
			throw UNEXPECTED_SEPARATOR.createWithContext(reader);
		return name;
	}
	
	@Override public Collection<String> getExamples() {
		if (!allowQualifier)
			return Arrays.asList("name", "hyphened-name");
		if (optionalQualifier)
			return Arrays.asList("name", "qualified:name");
		return Arrays.asList("qualified:name", "qualified:hyphened-name");
	}
}
