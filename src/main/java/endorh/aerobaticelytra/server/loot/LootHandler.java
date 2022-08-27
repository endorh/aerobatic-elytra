package endorh.aerobaticelytra.server.loot;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.flightcore.events.GenerateEndShipItemFrameEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
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
		    .withParameter(LootContextParams.ORIGIN, event.getItemFrame().position())
		    .create(LootContextParamSets.CHEST));
		
		if (!list.isEmpty()) {
			event.setElytraStack(list.get(0));
			event.setResult(Result.DENY);
		}
	}
}
