package dnj.aerobatic_elytra.integration.colytra;

import dnj.aerobatic_elytra.common.item.AerobaticElytraItem;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.caelus.api.RenderElytraEvent;

import java.util.List;

import static dnj.aerobatic_elytra.integration.colytra.ColytraIntegration.getColytraSubItem;

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
		final List<ITextComponent> tooltip = event.getToolTip();
		
		// Undo colytra tooltip
		int index = 0;
		boolean found = false;
		// Try to find the colytra tooltip in the list
		if (elytra.hasDisplayName()) {
			ITextComponent display = elytra.getDisplayName();
			final String name = display.getString();
			for (ITextComponent component : tooltip) {
				if (name.equals(component.getString())) {
					found = true;
					break;
				}
				index++;
			}
		} else {
			for (ITextComponent component : tooltip) {
				if (component instanceof TranslationTextComponent) {
					TranslationTextComponent tr = (TranslationTextComponent) component;
					if ("item.minecraft.elytra".equals(tr.getKey())) {
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
			if (elytra.hasDisplayName())
				tooltip.add(index++, elytra.getDisplayName().copyRaw()
				  .mergeStyle(TextFormatting.AQUA).mergeStyle(TextFormatting.ITALIC));
			else tooltip.add(index++, item.getDisplayName(elytra));
			tooltip.addAll(index, item.getTooltipInfo(elytra, event.getFlags(), "  "));
		}
	}
	
	/**
	 * Hide the default elytra render layer in colytra armors when
	 * their subitem is an Aerobatic Elytra
	 */
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onRenderElytraEvent(RenderElytraEvent event) {
		ItemStack elytra = getColytraSubItem(
		  event.getPlayer().getItemStackFromSlot(EquipmentSlotType.CHEST));
		if ((elytra.getItem() instanceof AerobaticElytraItem))
			event.setRender(false);
	}
}
