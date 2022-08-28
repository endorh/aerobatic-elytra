package endorh.aerobaticelytra.integration.jei.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredientRenderer;
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
	
	@Override public void draw(@NotNull PoseStack mStack, int xOffset, int yOffset) {
		RenderSystem.enableDepthTest();
		mStack.pushPose(); {
			mStack.translate(xOffset - 2, yOffset - 2, 0D);
			ingredientRenderer.render(mStack, first);
		} mStack.popPose();
		mStack.pushPose(); {
			mStack.translate(xOffset + 4, yOffset + 4, 0D);
			ingredientRenderer.render(mStack, second);
		} mStack.popPose();
		RenderSystem.disableDepthTest();
	}
}
