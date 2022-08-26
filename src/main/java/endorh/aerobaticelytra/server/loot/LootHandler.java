package endorh.aerobaticelytra.server.loot;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.flightcore.events.GenerateEndShipItemFrameEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.LootTable;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class LootHandler {
	private static final Logger LOGGER = LogManager.getLogger();
	
	@SubscribeEvent
	public static void onGenerateEndShipElytraItemFrame(
	  final GenerateEndShipItemFrameEvent event
	) {
		LOGGER.debug(event.world);
		LootTable table = event.world.getServer().getLootTables()
		  .get(ModLootTables.END_SHIP_ELYTRA);
		
		List<ItemStack> list = table.getRandomItems(
		  new LootContext.Builder(event.world).withRandom(event.random)
		    .withParameter(LootParameters.ORIGIN, event.getItemFrame().position())
		    .create(LootParameterSets.CHEST));
		
		if (!list.isEmpty()) {
			event.setElytraStack(list.get(0));
			event.setResult(Result.DENY);
		}
	}
}
