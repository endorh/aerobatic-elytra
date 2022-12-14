package endorh.aerobaticelytra.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;

import java.util.UUID;

import static endorh.util.network.PacketBufferUtil.readNullable;
import static endorh.util.network.PacketBufferUtil.writeNullable;

public class Debug {
	public static Debug DEBUG = new Debug();
	private static boolean registered = false;
	
	public boolean enabled = false;
	public boolean suppressParticles;
	public boolean invertFreeze;
	public boolean persistentParticles = false;
	public float freezeParticleSpeed = 1F;
	public float particleSpeed = 0.1F;
	public UUID targetPlayer = null;
	
	public static void register() {
		if (registered || !DEBUG.enabled) return;
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
		  MinecraftForge.EVENT_BUS.register(DebugOverlay.class));
		DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () ->
		  MinecraftForge.EVENT_BUS.register(DebugTicker.class));
		registered = true;
	}
	
	@OnlyIn(Dist.CLIENT)
	public PlayerEntity getTargetPlayer() {
		Minecraft mc = Minecraft.getInstance();
		if (targetPlayer == null || mc.world == null) return mc.player;
		PlayerEntity player = mc.world.getPlayerByUuid(targetPlayer);
		return player == null? mc.player : player;
	}
	
	public void serialize(PacketBuffer buf) {
		buf.writeBoolean(enabled);
		buf.writeBoolean(suppressParticles);
		buf.writeBoolean(invertFreeze);
		buf.writeBoolean(persistentParticles);
		buf.writeFloat(freezeParticleSpeed);
		buf.writeFloat(particleSpeed);
		writeNullable(buf, targetPlayer, PacketBuffer::writeUniqueId);
	}
	
	public static Debug deserialize(PacketBuffer buf) {
		Debug d = new Debug();
		d.enabled = buf.readBoolean();
		d.suppressParticles = buf.readBoolean();
		d.invertFreeze = buf.readBoolean();
		d.persistentParticles = buf.readBoolean();
		d.freezeParticleSpeed = buf.readFloat();
		d.particleSpeed = buf.readFloat();
		d.targetPlayer = readNullable(buf, PacketBuffer::readUniqueId);
		return d;
	}
	
	public static void update(Debug debug) {
		DEBUG = debug;
		register();
	}
}
