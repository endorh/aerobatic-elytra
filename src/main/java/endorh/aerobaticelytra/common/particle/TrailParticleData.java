package endorh.aerobaticelytra.common.particle;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import endorh.aerobaticelytra.client.trail.AerobaticTrail.RocketSide;
import endorh.aerobaticelytra.common.capability.IElytraSpec.TrailData;
import endorh.lazulib.math.Vec3f;
import endorh.lazulib.network.PacketBufferUtil;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Locale;

public class TrailParticleData implements ParticleOptions {
	
	public final float size;
	public final int life;
	public final float partialTick;
	public final boolean ownPlayer;
	
	public final Color color;
	public final Color fadeColor;
	public final boolean flicker;
	public final boolean trail;
	public final byte type;
	
	public final RocketSide side;
	
	public final Vec3f rollVec;
	public final TrailData trailData;
	
	private TrailParticleData(
	  int colorIn, int fadeColorIn, byte typeIn, boolean flickerIn, boolean trailIn, int lifeIn,
	  float sizeIn, boolean ownPlayerIn, List<Float> rollVecIn, int sideIn
	) {
		this(new Color(colorIn), new Color(fadeColorIn), typeIn, flickerIn, trailIn, lifeIn,
		     sizeIn, 0F, ownPlayerIn, new Vec3f(rollVecIn), RocketSide.values()[sideIn], null);
	}
	public TrailParticleData(
	  Color colorIn, Color fadeColorIn, byte typeIn, boolean flickerIn, boolean trailIn, int lifeIn,
	  float sizeIn, float partialTickIn, boolean ownPlayerIn, @Nullable Vec3f rollVecIn,
	  @Nullable RocketSide sideIn, @Nullable TrailData trailDataIn
	) {
		color = colorIn;
		fadeColor = fadeColorIn;
		type = typeIn;
		flicker = flickerIn;
		trail = trailIn;
		
		life = lifeIn;
		size = Mth.clamp(sizeIn, 0F, 1F);
		partialTick = partialTickIn;
		ownPlayer = ownPlayerIn;
		rollVec = rollVecIn != null? rollVecIn.copy() : null;
		side = sideIn;
		trailData = trailDataIn;
	}
	
	@NotNull @Override
	public ParticleType<TrailParticleData> getType() {
		return AerobaticElytraParticles.TRAIL_PARTICLES.get(type);
	}
	
	@Override public void writeToNetwork(@NotNull FriendlyByteBuf buf) {
		buf.writeInt(color.getRGB());
		buf.writeInt(fadeColor.getRGB());
		buf.writeByte(type);
		buf.writeBoolean(flicker);
		buf.writeBoolean(trail);
		buf.writeInt(life);
		buf.writeFloat(size);
		buf.writeFloat(partialTick);
		buf.writeBoolean(ownPlayer);
		rollVec.write(buf);
		buf.writeEnum(side);
		PacketBufferUtil.writeNullable(trailData, buf, TrailData::write);
	}
	
	@NotNull @Override public String writeToString() {
		return String.format(
		  Locale.ROOT, "%s %d %d %d %d %d %d %d %b %b %f %f %b %f %f %f %d",
		  ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()),
		  color.getRed(), color.getGreen(), color.getBlue(),
		  fadeColor.getRed(), fadeColor.getGreen(), fadeColor.getBlue(),
		  type, flicker, trail, size, partialTick, ownPlayer,
		  rollVec.x, rollVec.y, rollVec.z, side.ordinal());
	}
	
	public static final Codec<TrailParticleData> CODEC = RecordCodecBuilder.create(
	  instance -> instance.group(
	    Codec.INT.fieldOf("color").forGetter(d -> d.color.getRGB()),
	    Codec.INT.fieldOf("fadeColor").forGetter(d -> d.fadeColor.getRGB()),
	    Codec.BYTE.fieldOf("type").forGetter(d -> d.type),
	    Codec.BOOL.fieldOf("flicker").forGetter(d -> d.flicker),
	    Codec.BOOL.fieldOf("trail").forGetter(d -> d.trail),
	    Codec.INT.fieldOf("life").forGetter(d -> d.life),
	    Codec.FLOAT.fieldOf("size").forGetter(d -> d.size),
	    Codec.BOOL.fieldOf("ownPlayer").forGetter(d -> d.ownPlayer),
	    Codec.list(Codec.FLOAT).fieldOf("rollVec").forGetter(d -> d.rollVec.asList()),
	    Codec.INT.fieldOf("side").forGetter(d -> d.side.ordinal())
	  ).apply(instance, TrailParticleData::new)
	);
	
	@SuppressWarnings("deprecation")
	public static final Deserializer<TrailParticleData> DESERIALIZER = new Deserializer<>() {
		@NotNull @Override public TrailParticleData fromCommand(
		  @NotNull ParticleType<TrailParticleData> type, @NotNull StringReader reader
		) throws CommandSyntaxException {
			reader.expect(' ');
			
			int red = reader.readInt() & 0xFF;
			reader.expect(' ');
			int green = reader.readInt() & 0xFF;
			reader.expect(' ');
			int blue = reader.readInt() & 0xFF;
			reader.expect(' ');
			Color color = new Color(red, green, blue);
			red = reader.readInt() & 0xFF;
			reader.expect(' ');
			green = reader.readInt() & 0xFF;
			reader.expect(' ');
			blue = reader.readInt() & 0xFF;
			reader.expect(' ');
			Color fadeColor = new Color(red, green, blue);
			byte typ = (byte) (reader.readInt() & 0xFF);
			reader.expect(' ');
			boolean flicker = reader.readBoolean();
			reader.expect(' ');
			boolean trail = reader.readBoolean();
			reader.expect(' ');
			
			float size = Mth.clamp(reader.readFloat(), 0F, 1F);
			reader.expect(' ');
			int life = reader.readInt();
			reader.expect(' ');
			
			float partialTick = reader.readFloat();
			reader.expect(' ');
			boolean ownPlayer = reader.readBoolean();
			reader.expect(' ');
			
			final float x = reader.readFloat();
			reader.expect(' ');
			final float y = reader.readFloat();
			reader.expect(' ');
			final float z = reader.readFloat();
			reader.expect(' ');
			final Vec3f rollVec = new Vec3f(x, y, z);
			
			final int s = reader.readInt();
			RocketSide side;
			try {
				side = RocketSide.values()[s];
			} catch (IndexOutOfBoundsException e) {
				Message msg = Component.literal(
				  "Unknown rocket side: " + s
				  + ", Valid sides are 0-" + RocketSide.values().length);
				throw new CommandSyntaxException(new SimpleCommandExceptionType(msg), msg);
			}
			
			return new TrailParticleData(
			  color, fadeColor, typ, flicker, trail, life, size,
			  partialTick, ownPlayer, rollVec, side, null);
		}
		
		@Override public @NotNull TrailParticleData fromNetwork(
		  @NotNull ParticleType<TrailParticleData> type, FriendlyByteBuf buf
		) {
			int rgb = buf.readInt();
			int fadeRGB = buf.readInt();
			byte typ = buf.readByte();
			boolean flicker = buf.readBoolean();
			boolean trail = buf.readBoolean();
			int life = buf.readInt();
			float size = buf.readFloat();
			float partialTick = buf.readFloat();
			boolean ownPlayer = buf.readBoolean();
			Vec3f rollVec = Vec3f.read(buf);
			RocketSide side = buf.readEnum(RocketSide.class);
			TrailData trailData = PacketBufferUtil.readNullable(buf, TrailData::read);
			
			return new TrailParticleData(
			  new Color(rgb), new Color(fadeRGB), typ, flicker, trail,
			  life, size, partialTick, ownPlayer, rollVec, side, trailData);
		}
	};
	
	public static boolean ALWAYS_SHOW_TRAIL = true;
	public static class TrailParticleType extends ParticleType<TrailParticleData> {
		public TrailParticleType() {
			this(ALWAYS_SHOW_TRAIL);
		}
		public TrailParticleType(boolean alwaysShow) {
			super(alwaysShow, DESERIALIZER);
		}
		@NotNull @Override public Codec<TrailParticleData> codec() {
			return CODEC;
		}
	}
	public static class StarTrailParticleType extends TrailParticleType {}
	public static class CreeperTrailParticleType extends TrailParticleType {}
	public static class BurstTrailParticleType extends TrailParticleType {}
	public static class BubbleTrailParticleType extends TrailParticleType {}
}
