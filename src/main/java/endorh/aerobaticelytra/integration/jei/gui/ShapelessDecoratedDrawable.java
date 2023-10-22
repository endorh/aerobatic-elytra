package endorh.aerobaticelytra.integration.jei.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.gui.GuiGraphics;
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
	
	@Override public void draw(@NotNull GuiGraphics gg, int xOffset, int yOffset) {
		decorated.draw(gg, xOffset, yOffset);
		final int shapelessIconX = getWidth() - shapelessIcon.getWidth() / scale;

		PoseStack pStack = gg.pose();
		pStack.pushPose(); {
			pStack.translate(shapelessIconX, 0, 0);
			pStack.scale(1F / scale, 1F / scale, 1);
			(dark ? darkShapelessIcon : shapelessIcon).draw(gg);
		} pStack.popPose();
	}
}
