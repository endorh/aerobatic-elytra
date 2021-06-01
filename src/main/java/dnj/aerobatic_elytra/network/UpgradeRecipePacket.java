package dnj.aerobatic_elytra.network;

import dnj.aerobatic_elytra.common.recipe.UpgradeRecipe;
import dnj.endor8util.network.ClientPlayerPacket;
import dnj.endor8util.network.PacketBufferUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static dnj.aerobatic_elytra.network.NetworkHandler.ID_GEN;

public class UpgradeRecipePacket extends ClientPlayerPacket {
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void register() {
		ClientPlayerPacket.with(NetworkHandler.CHANNEL, ID_GEN)
		  .register(UpgradeRecipePacket::new);
	}
	
	List<ResourceLocation> recipeIDs;
	public UpgradeRecipePacket() {}
	
	public UpgradeRecipePacket(PlayerEntity player, List<UpgradeRecipe> recipes) {
		super(player);
		recipeIDs = recipes.stream().map(SpecialRecipe::getId)
		  .collect(Collectors.toList());
	}
	
	public List<UpgradeRecipe> getRecipes(PlayerEntity player) {
		//noinspection unchecked
		return (List<UpgradeRecipe>) recipeIDs.stream().map(
		  id -> {
			  Optional<?> opt = player.world.getRecipeManager().getRecipe(id);
			  if (!opt.isPresent()) {
			  	LOGGER.error("Unknown recipe id found in packet: '" + id + "'\nRecipe will be ignored");
			  	return null;
			  }
			  return opt.get();
		  }
		).filter(Objects::nonNull).collect(Collectors.toList());
	}
	
	@Override public void onServer(PlayerEntity player, Context ctx) {
		List<UpgradeRecipe> recipes = getRecipes(player);
		UpgradeRecipe.apply(player, recipes);
	}
	
	@Override public void serialize(PacketBuffer buf) {
		PacketBufferUtil.writeList(buf, recipeIDs, PacketBuffer::writeResourceLocation);
	}
	
	@Override public void deserialize(PacketBuffer buf) {
		recipeIDs = PacketBufferUtil.readList(buf, PacketBuffer::readResourceLocation);
	}
}
