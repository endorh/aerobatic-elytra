package dnj.aerobatic_elytra.integration.curios;

import dnj.aerobatic_elytra.common.item.AerobaticElytraItem;
import dnj.aerobatic_elytra.common.item.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import top.theillusivec4.caelus.api.CaelusApi;
import top.theillusivec4.caelus.api.RenderElytraEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

public class CuriosIntegration {
	public static ItemStack getCurioAerobaticElytra(LivingEntity entity) {
		Optional<ImmutableTriple<String, Integer, ItemStack>> curio =
		  findCurioAerobaticElytra(entity);
		if (curio.isPresent()) {
			return curio.get().getRight();
		}
		return ItemStack.EMPTY;
	}
	
	public static Optional<ImmutableTriple<String, Integer, ItemStack>> findCurioAerobaticElytra(
	  LivingEntity entity
	) {
		return CuriosApi.getCuriosHelper().findEquippedCurio(ModItems.AEROBATIC_ELYTRA, entity);
	}
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void renderElytra(RenderElytraEvent event) {
		final PlayerEntity player = event.getPlayer();
		Optional<ImmutableTriple<String, Integer, ItemStack>> opt =
		  findCurioAerobaticElytra(player);
		if (!opt.isPresent())
			return;
		ImmutableTriple<String, Integer, ItemStack> curio = opt.get();
		ItemStack elytra = curio.right;
		
		// Allow rendering an elytra at the same time
		if ((elytra.getItem() instanceof AerobaticElytraItem)) {
			event.setRender(false);
			final Optional<ImmutableTriple<String, Integer, ItemStack>> elytraOpt =
			  CuriosApi.getCuriosHelper().findEquippedCurio(
			    stack -> CaelusApi.isElytra(stack)
			             && !(stack.getItem() instanceof AerobaticElytraItem),
			    player);
			if (elytraOpt.isPresent())
				event.setRender(true);
		}
	}
}
