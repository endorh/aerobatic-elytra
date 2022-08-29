package endorh.aerobaticelytra.integration.colytra;

import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

import static endorh.aerobaticelytra.integration.colytra.ColytraIntegration.getColytraSubItem;

/**
 * Client compatibility handler for Colytra mod<br>
 * The methods are only subscribed to the event bus if Colytra is loaded
 */
public class ClientColytraIntegration {
	
	/**
	 * Replace Colytra's elytra durability tooltip with
	 * our own advanced tooltip
	 */
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onItemTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (!(stack.getItem() instanceof ArmorItem))
			return;
		ItemStack elytra = getColytraSubItem(stack);
		if (elytra.isEmpty())
			return;
		final AerobaticElytraItem item = (AerobaticElytraItem) elytra.getItem();
		final List<Component> tooltip = event.getToolTip();
		
		// Undo colytra tooltip
		int index = 0;
		boolean found = false;
		// Try to find the colytra tooltip in the list
		if (elytra.hasCustomHoverName()) {
			Component display = elytra.getHoverName();
			final String name = display.getString();
			for (Component component : tooltip) {
				if (name.equals(component.getString())) {
					found = true;
					break;
				}
				index++;
			}
		} else {
			for (Component component : tooltip) {
				ComponentContents contents = component.getContents();
				if (contents instanceof TranslatableContents) {
					if ("item.minecraft.elytra".equals(((TranslatableContents) contents).getKey())) {
						found = true;
						break;
					}
				}
				index++;
			}
		}
		// Replace the found tooltip in place
		if (found) {
			tooltip.remove(index);
			if (elytra.hasCustomHoverName())
				tooltip.add(index++, elytra.getHoverName().plainCopy()
				  .withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.ITALIC));
			else tooltip.add(index++, item.getName(elytra));
			tooltip.addAll(index, item.getTooltipInfo(elytra, event.getFlags(), "  "));
		}
	}
	
	// Colytra no longer uses the RenderElytraEvent from Caelus, so we
	// can't hide its vanilla wings unless we render all ourselves.
	// /**
	//  * Hide the default elytra render layer in colytra armors when
	//  * their subitem is an Aerobatic Elytra
	//  */
	// @SubscribeEvent(priority = EventPriority.LOW)
	// public static void onRenderElytraEvent(RenderElytraEvent event) {
	// 	//
	// 	ItemStack elytra = getColytraSubItem(
	// 	  event.getPlayer().getItemBySlot(EquipmentSlot.CHEST));
	// 	if ((elytra.getItem() instanceof AerobaticElytraItem))
	// 		event.setCanceled(true);
	// }
}
