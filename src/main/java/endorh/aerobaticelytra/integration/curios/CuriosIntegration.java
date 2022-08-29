package endorh.aerobaticelytra.integration.curios;

import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

public class CuriosIntegration {
	/**
	 * Get the first aerobatic elytra found in curio slots or empty
	 */
	public static ItemStack getCurioAerobaticElytra(LivingEntity entity) {
		Optional<ImmutableTriple<String, Integer, ItemStack>> curio =
		  findCurioAerobaticElytra(entity);
		if (curio.isPresent()) {
			return curio.get().getRight();
		}
		return ItemStack.EMPTY;
	}
	
	/**
	 * Find the first curio slot containing an aerobatic elytra
	 */
	public static Optional<ImmutableTriple<String, Integer, ItemStack>> findCurioAerobaticElytra(
	  LivingEntity entity
	) {
		return CuriosApi.getCuriosHelper().findEquippedCurio(AerobaticElytraItems.AEROBATIC_ELYTRA, entity);
	}
	
	// ElytraSlot mod doesn't use RenderElytraEvent from Caelus anymore
	// /**
	//  * Hide elytra render layer if an aerobatic elytra is being worn,
	//  * unless an elytra is also being worn
	//  */
	// @SubscribeEvent(priority = EventPriority.LOW)
	// public static void onRenderElytraEvent(RenderCapeEvent event) {
	// 	final Player player = event.getPlayer();
	// 	Optional<ImmutableTriple<String, Integer, ItemStack>> opt =
	// 	  findCurioAerobaticElytra(player);
	// 	if (opt.isEmpty()) return;
	// 	ImmutableTriple<String, Integer, ItemStack> curio = opt.get();
	// 	ItemStack elytra = curio.right;
	//
	// 	// Allow rendering an elytra at the same time
	// 	if ((elytra.getItem() instanceof AerobaticElytraItem)) {
	// 		event.setCanceled(true);
	// 		final Optional<ImmutableTriple<String, Integer, ItemStack>> elytraOpt =
	// 		  CuriosApi.getCuriosHelper().findEquippedCurio(
	// 		    stack -> stack.getItem() instanceof ElytraItem
	// 		             && !(stack.getItem() instanceof AerobaticElytraItem),
	// 		    player);
	// 		if (elytraOpt.isPresent())
	// 			event.setCanceled(false);
	// 	}
	// }
}
