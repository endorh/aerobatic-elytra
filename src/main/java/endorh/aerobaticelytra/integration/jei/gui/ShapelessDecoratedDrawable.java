package endorh.aerobaticelytra.integration.jei.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import org.jetbrains.annotations.NotNull;

public class ShapelessDecoratedDrawable implements IDrawable {
	protected static final int scale = 4;
	protected static IDrawable shapelessIcon;
	protected static IDrawable darkShapelessIcon;
	protected final IDrawable decorated;
	protected boolean dark;
	
	public ShapelessDecoratedDrawable(IDrawable decorated, IGuiHelper guiHelper, boolean dark) {
		this.decorated = decorated;
		this.dark = dark;
		if (shapelessIcon == null) {
			shapelessIcon = guiHelper.createDrawable(JeiResources.TEXTURE_RECIPES, 0, 144, 36, 34);
			darkShapelessIcon = guiHelper.createDrawable(JeiResources.TEXTURE_RECIPES, 36, 144, 36, 34);
		}
	}
	
	public static int shapelessIconWidth() {
		return shapelessIcon.getWidth() / scale;
	}
	
	public static int shapelessIconHeight() {
		return shapelessIcon.getHeight() / scale;
	}
	
	@Override public int getWidth() {
		return decorated.getWidth();
	}
	
	@Override public int getHeight() {
		return decorated.getHeight();
	}
	
	@Override public void draw(@NotNull PoseStack mStack, int xOffset, int yOffset) {
		decorated.draw(mStack, xOffset, yOffset);
		final int shapelessIconX = getWidth() - (shapelessIcon.getWidth() / scale);
		
		mStack.pushPose(); {
			mStack.translate(shapelessIconX, 0, 0);
			mStack.scale(1F / scale, 1F / scale, 1);
			(dark ? darkShapelessIcon : shapelessIcon).draw(mStack);
		} mStack.popPose();
	}
}
