package endorh.aerobaticelytra.common.recipe;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.network.UpgradeRecipePacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.List;

/**
 * React to using items on the Acrobatic Elytra
 */
@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class UpgradeHandler {
	/**
	 * Filter events
	 */
	@SubscribeEvent
	public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
		Player player = event.getEntity();
		if (player.onGround()) {
			ItemStack elytra = player.getItemBySlot(EquipmentSlot.OFFHAND);
			if (elytra.getItem() instanceof AerobaticElytraItem) {
				ItemStack itemStack = event.getItemStack();
				if (onItemUse(player, elytra, itemStack)) {
					event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
					event.setCanceled(true);
				}
			}
		}
	}
	
	/**
	 * Main item use logic
	 */
	public static boolean onItemUse(Player player, ItemStack elytra, ItemStack stack) {
		if (player.isCrouching()) {
			List<UpgradeRecipe> upgrades = UpgradeRecipe.getUpgradeRecipes(
			  elytra, stack);
			if (upgrades.size() == 0)
				return false;
			if (player.level().isClientSide)
				new UpgradeRecipePacket(player, upgrades).send();
			return true;
		}
		return false;
	}
}
