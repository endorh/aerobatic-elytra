package endorh.aerobaticelytra.client.render.layer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig.style.visibility;
import endorh.aerobaticelytra.client.config.ClientConfig.style.visual;
import endorh.aerobaticelytra.client.render.model.AerobaticElytraChestModel;
import endorh.aerobaticelytra.client.render.model.AerobaticElytraModel;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.ElytraDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static endorh.aerobaticelytra.client.AerobaticElytraResources.TEXTURE_AEROBATIC_ELYTRA;
import static java.lang.Math.min;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class AerobaticElytraLayer<T extends LivingEntity, M extends BipedModel<T>>
  extends ElytraLayer<T, M> {
	private final AerobaticElytraModel<T> modelElytra = new AerobaticElytraModel<>();
	private final AerobaticElytraChestModel<T> modelBack = new AerobaticElytraChestModel<>();
	private final ElytraDyement dyement = new ElytraDyement();
	
	public AerobaticElytraLayer(IEntityRenderer<T, M> rendererIn) {
		super(rendererIn);
	}
	
	@Override public boolean shouldRender(@NotNull ItemStack stack, @NotNull T entity) {
		return true;
	}
	
	/**
	 * Derived from ElytraModel
	 */
	@Override public void render(
	  @NotNull MatrixStack mStack, @NotNull IRenderTypeBuffer buffer,
	  int packedLight, @NotNull T entity, float limbSwing, float limbSwingAmount,
	  float partialTicks, float ageInTicks, float netHeadYaw, float headPitch
	) {
		ItemStack elytra = AerobaticElytraLogic.getAerobaticElytra(entity);
		final Item it = elytra.getItem();
		if (elytra.isEmpty() || !(it instanceof AerobaticElytraItem))
			return;
		AerobaticElytraItem item = (AerobaticElytraItem) it;
		if (!shouldRender(elytra, entity)
		    || !item.shouldRenderAerobaticElytraLayer(elytra, entity))
			return;
		
		dyement.read(elytra);
		
		getEntityModel().setModelAttributes(this.modelBack);
		modelBack.setRotationAngles(
		  entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		if (item.shouldRenderAerobaticElytraBackRockets(elytra, entity))
			renderBackRockets(
			  mStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks,
			  ageInTicks, netHeadYaw, headPitch, elytra);
		
		// This translation added by the ElytraLayer is instead added by the model itself,
		// which allows the model to be rotated along with armor stand entities
		//   mStack.translate(0.0D, 0.0D, 0.125D);
		getEntityModel().copyModelAttributesTo(modelElytra);
		modelElytra.setRotationAngles(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		
		final boolean effect = !visibility.disable_wing_glint && item.hasModelEffect(elytra);
		if (effect) {
			ResourceLocation glintTexture = getElytraTexture(elytra, entity);
			modelElytra.renderGlint(
			  mStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, glintTexture,
			  1F, 1F, 1F, 1F); // This alpha value affects nothing (unless 0)
		}
		
		for (WingSide side : WingSide.values()) {
			WingDyement wingDyement = dyement.getWing(side);
			if (wingDyement.hasPattern) {
				renderWingBanner(
				  mStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks,
				  ageInTicks, netHeadYaw, headPitch, elytra, side, item, wingDyement.patternColorData);
			} else {
				renderWingDyed(
				  mStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks,
				  ageInTicks, netHeadYaw, headPitch, elytra, side, wingDyement.color, effect);
			}
		}
		
		if (item.shouldRenderAerobaticElytraRockets(elytra, entity))
			renderRockets(
			  mStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks,
			  ageInTicks, netHeadYaw, headPitch, elytra);
	}
	
	public void renderRockets(
	  MatrixStack mStack, IRenderTypeBuffer buffer, int packedLight, T entity,
	  float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
	  float netHeadYaw, float headPitch, ItemStack elytra
	) {
		AerobaticElytraItem item = (AerobaticElytraItem)elytra.getItem();
		ResourceLocation texture = getElytraTexture(elytra, entity);
		
		IVertexBuilder rocketBuilder = ItemRenderer.getArmorVertexBuilder(
		  buffer, RenderType.getArmorCutoutNoCull(texture), false, item.hasModelEffect(elytra));
		modelElytra.rocketsModel.render(
		  mStack, rocketBuilder, packedLight, OverlayTexture.NO_OVERLAY,
		  1F, 1F, 1F, 1F);
	}
	
	public void renderBackRockets(
	  MatrixStack mStack, IRenderTypeBuffer buffer, int packedLight, T entity,
	  float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
	  float netHeadYaw, float headPitch, ItemStack elytra
	) {
		AerobaticElytraItem item = (AerobaticElytraItem)elytra.getItem();
		IVertexBuilder backBuilder = ItemRenderer.getArmorVertexBuilder(
		  buffer, RenderType.getArmorCutoutNoCull(TEXTURE_AEROBATIC_ELYTRA), false, item.hasModelEffect(elytra));
		modelBack.render(mStack, backBuilder, packedLight, OverlayTexture.NO_OVERLAY,
		                 1F, 1F, 1F, 1F);
	}
	
	public void renderWingDyed(
	  MatrixStack mStack, IRenderTypeBuffer buffer, int packedLight, T entity,
	  float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
	  float netHeadYaw, float headPitch, ItemStack elytra, WingSide side, int color, boolean effect
	) {
		float[] components = componentArray(color);
		ResourceLocation texture = getElytraTexture(elytra, entity);
		
		IVertexBuilder elytraBuilder = ItemRenderer.getArmorVertexBuilder(
		  buffer, RenderType.getArmorCutoutNoCull(texture), false, effect);
		modelElytra.renderWing(
		  side, mStack, elytraBuilder, packedLight, OverlayTexture.NO_OVERLAY,
		  components[0], components[1], components[2], 1F);
	}
	
	public void renderWingBanner(
	  MatrixStack mStack, IRenderTypeBuffer buffer, int packedLight, T entity,
	  float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
	  float netHeadYaw, float headPitch, ItemStack elytra, WingSide side,
	  AerobaticElytraItem item, List<Pair<BannerPattern, DyeColor>> patternColorData
	) {
		final int size = min(visual.max_rendered_banner_layers + 1, patternColorData.size());
		for (int i = 0; i < size; ++i) {
			Pair<BannerPattern, DyeColor> pair = patternColorData.get(i);
			float[] color = pair.getSecond().getColorComponentValues();
			RenderMaterial material = item.getBannerMaterial(pair.getFirst());
			// Unknown patterns are omitted
			if (material.getSprite().getName() != MissingTextureSprite.getLocation()) modelElytra.renderWing(
			  side, mStack, material.getBuffer(buffer, RenderType::getEntityTranslucent),
			  packedLight, OverlayTexture.NO_OVERLAY, color[0], color[1], color[2], 1F);
		}
	}
	
	public float[] componentArray(int color) {
		return new float[]{
		  (color >> 16 & 0xFF) / 255F,
		  (color >> 8 & 0xFF) / 255F,
		  (color & 0xFF) / 255F};
	}
	
	@Override @NotNull
	public ResourceLocation getElytraTexture(@NotNull ItemStack stack, @NotNull T entity) {
			return TEXTURE_AEROBATIC_ELYTRA;
	}
	
	private static boolean addedToArmorStands = false;
	
	/**
	 * Add the layer to armor stands once
	 */
	@SubscribeEvent
	public static void onEntityConstructing(EntityEvent.EntityConstructing event) {
		if (!addedToArmorStands && event.getEntity() instanceof ArmorStandEntity) {
			ArmorStandEntity entity = (ArmorStandEntity) event.getEntity();
			ArmorStandRenderer renderer = (ArmorStandRenderer) Minecraft.getInstance()
			  .getRenderManager().getRenderer(entity);
			renderer.addLayer(new AerobaticElytraLayer<>(renderer));
			addedToArmorStands = true;
			AerobaticElytra.logRegistered("Armor Stand Layer");
		}
	}
}
