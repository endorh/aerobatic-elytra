package endorh.aerobaticelytra.integration.jei.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
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
	
	@Override public void draw(@NotNull MatrixStack matrixStack, int xOffset, int yOffset) {
		RenderSystem.enableDepthTest();
		this.ingredientRenderer.render(matrixStack, xOffset - 2, yOffset - 2, first);
		this.ingredientRenderer.render(matrixStack, xOffset + 4, yOffset + 4, second);
		RenderSystem.enableAlphaTest();
		RenderSystem.disableDepthTest();
	}
}
