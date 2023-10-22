package endorh.aerobaticelytra.integration.jei.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

public class MultiIngredientDrawable<V> implements IDrawable {
	private final V first;
	private final V second;
	private final IIngredientRenderer<V> ingredientRenderer;
	
	public MultiIngredientDrawable(
	  V first, V second, IIngredientRenderer<V> ingredientRenderer
	) {
		this.first = first;
		this.second = second;
		this.ingredientRenderer = ingredientRenderer;
	}
	
	@Override public int getWidth() {
		return 16;
	}
	
	@Override public int getHeight() {
		return 16;
	}
	
	@Override public void draw(@NotNull GuiGraphics gg, int xOffset, int yOffset) {
		RenderSystem.enableDepthTest();
		PoseStack pStack = gg.pose();
		pStack.pushPose(); {
			pStack.translate(xOffset - 2, yOffset - 2, 0D);
			ingredientRenderer.render(gg, first);
		} pStack.popPose();
		pStack.pushPose(); {
			pStack.translate(xOffset + 4, yOffset + 4, 0D);
			ingredientRenderer.render(gg, second);
		} pStack.popPose();
		RenderSystem.disableDepthTest();
	}
}
