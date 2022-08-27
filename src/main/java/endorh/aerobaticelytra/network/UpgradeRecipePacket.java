package endorh.aerobaticelytra.network;

import endorh.aerobaticelytra.common.recipe.UpgradeRecipe;
import endorh.util.network.ClientPlayerPacket;
import endorh.util.network.PacketBufferUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fmllegacy.network.NetworkEvent.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static endorh.aerobaticelytra.network.NetworkHandler.ID_GEN;

/**
 * Apply the client requested recipes on the server<br>
 * The recipe application is deferred to {@link UpgradeRecipe#apply}
 * which checks that the ingredients match on the server side.
 */
public class UpgradeRecipePacket extends ClientPlayerPacket {
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void register() {
		ClientPlayerPacket.with(NetworkHandler.CHANNEL, ID_GEN)
		  .register(UpgradeRecipePacket::new);
	}
	
	List<ResourceLocation> recipeIDs;
	public UpgradeRecipePacket() {}
	
	public UpgradeRecipePacket(Player player, List<UpgradeRecipe> recipes) {
		super(player);
		recipeIDs = recipes.stream().map(CustomRecipe::getId)
		  .collect(Collectors.toList());
	}
	
	public List<UpgradeRecipe> getRecipes(Player player) {
		return recipeIDs.stream().map(id -> {
			final Optional<? extends Recipe<?>> opt = player.level.getRecipeManager().byKey(id);
			if (opt.isEmpty()) {
				LOGGER.error(
				  "Unknown recipe id found in packet from player \"" + player.getScoreboardName() +
				  "\": \"" + id + "\"\nRecipe will be ignored");
				return null;
			}
			Recipe<?> recipe = opt.get();
			if (recipe instanceof UpgradeRecipe)
				return (UpgradeRecipe) recipe;
			LOGGER.error(
			  "Invalid recipe id found in packet from player \"" + player.getScoreboardName() +
			  "\": \"" + id + "\"\nRecipe will be ignored");
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}
	
	@Override public void onServer(Player player, Context ctx) {
		List<UpgradeRecipe> recipes = getRecipes(player);
		UpgradeRecipe.apply(player, recipes);
	}
	
	@Override public void serialize(FriendlyByteBuf buf) {
		PacketBufferUtil.writeList(buf, recipeIDs, FriendlyByteBuf::writeResourceLocation);
	}
	
	@Override public void deserialize(FriendlyByteBuf buf) {
		recipeIDs = PacketBufferUtil.readList(buf, FriendlyByteBuf::readResourceLocation);
	}
}
