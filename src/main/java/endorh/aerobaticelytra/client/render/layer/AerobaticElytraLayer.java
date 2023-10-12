package endorh.aerobaticelytra.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static endorh.aerobaticelytra.client.AerobaticElytraResources.TEXTURE_AEROBATIC_ELYTRA;
import static java.lang.Math.min;

@EventBusSubscriber(value=Dist.CLIENT, bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
public class AerobaticElytraLayer<T extends LivingEntity, M extends EntityModel<T>>
  extends ElytraLayer<T, M> {
	private final AerobaticElytraModel<T> modelElytra;
	private final AerobaticElytraChestModel<T> modelBack;
	private final ElytraDyement dyement = new ElytraDyement();
	
	public AerobaticElytraLayer(RenderLayerParent<T, M> rendererIn, EntityModelSet modelSet) {
		super(rendererIn, modelSet);
		if (!(rendererIn.getModel() instanceof HumanoidModel<?>)) throw new IllegalArgumentException(
		  "Illegal parent renderer. Aerobatic Elytra Layer cannot be applied to non-humanoid models.");
		modelElytra = new AerobaticElytraModel<>(
		  modelSet.bakeLayer(AerobaticElytraModel.AEROBATIC_ELYTRA_LAYER));
		modelBack = new AerobaticElytraChestModel<>(
		  modelSet.bakeLayer(AerobaticElytraChestModel.AEROBATIC_ELYTRA_CHEST_LAYER));
	}
	
	@Override public boolean shouldRender(@NotNull ItemStack stack, @NotNull T entity) {
		return true;
	}
	
	/**
	 * Derived from ElytraModel
	 */
	@Override public void render(
	  @NotNull PoseStack mStack, @NotNull MultiBufferSource buffer,
	  int packedLight, @NotNull T entity, float limbSwing, float limbSwingAmount,
	  float partialTicks, float ageInTicks, float netHeadYaw, float headPitch
	) {
		ItemStack elytra = AerobaticElytraLogic.getAerobaticElytra(entity);
		final Item it = elytra.getItem();
		if (elytra.isEmpty() || !(it instanceof AerobaticElytraItem item))
			return;
		if (!shouldRender(elytra, entity)
		    || !item.shouldRenderAerobaticElytraLayer(elytra, entity))
			return;
		
		dyement.read(elytra);
		
		//noinspection unchecked
		((HumanoidModel<T>) getParentModel()).copyPropertiesTo(modelBack);
		modelBack.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		if (item.shouldRenderAerobaticElytraBackRockets(elytra, entity))
			renderBackRockets(
			  mStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks,
			  ageInTicks, netHeadYaw, headPitch, elytra);
		
		// This translation added by the ElytraLayer is instead added by the model itself,
		// which allows the model to be rotated along with armor stand entities
		//   mStack.translate(0.0D, 0.0D, 0.125D);
		getParentModel().copyPropertiesTo(modelElytra);
		modelElytra.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		
		final boolean effect = !visibility.disable_wing_glint && item.hasModelEffect(elytra);
		if (effect) {
			ResourceLocation glintTexture = getElytraTexture(elytra, entity);
			modelElytra.renderGlint(
			  mStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, glintTexture,
			  1F, 1F, 1F, 1F); // This alpha value affects nothing (unless 0)
		}
		
		for (WingSide side: WingSide.values()) {
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
	  PoseStack mStack, MultiBufferSource buffer, int packedLight, T entity,
	  float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
	  float netHeadYaw, float headPitch, ItemStack elytra
	) {
		AerobaticElytraItem item = (AerobaticElytraItem) elytra.getItem();
		ResourceLocation texture = getElytraTexture(elytra, entity);
		
		VertexConsumer rocketBuilder = ItemRenderer.getArmorFoilBuffer(
		  buffer, RenderType.armorCutoutNoCull(texture), false, item.hasModelEffect(elytra));
		modelElytra.renderRockets(
		  mStack, rocketBuilder, packedLight, OverlayTexture.NO_OVERLAY,
		  1F, 1F, 1F, 1F);
	}
	
	public void renderBackRockets(
	  PoseStack mStack, MultiBufferSource buffer, int packedLight, T entity,
	  float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
	  float netHeadYaw, float headPitch, ItemStack elytra
	) {
		AerobaticElytraItem item = (AerobaticElytraItem) elytra.getItem();
		VertexConsumer backBuilder = ItemRenderer.getArmorFoilBuffer(
		  buffer, RenderType.armorCutoutNoCull(TEXTURE_AEROBATIC_ELYTRA), false,
		  item.hasModelEffect(elytra));
		modelBack.renderToBuffer(mStack, backBuilder, packedLight, OverlayTexture.NO_OVERLAY,
		                         1F, 1F, 1F, 1F);
	}
	
	public void renderWingDyed(
	  PoseStack mStack, MultiBufferSource buffer, int packedLight, T entity,
	  float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
	  float netHeadYaw, float headPitch, ItemStack elytra, WingSide side, int color, boolean effect
	) {
		float[] components = componentArray(color);
		ResourceLocation texture = getElytraTexture(elytra, entity);
		
		VertexConsumer elytraBuilder = ItemRenderer.getArmorFoilBuffer(
		  buffer, RenderType.armorCutoutNoCull(texture), false, effect);
		modelElytra.renderWing(
		  side, mStack, elytraBuilder, packedLight, OverlayTexture.NO_OVERLAY,
		  components[0], components[1], components[2], 1F);
	}
	
	public void renderWingBanner(
	  PoseStack mStack, MultiBufferSource buffer, int packedLight, T entity,
	  float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
	  float netHeadYaw, float headPitch, ItemStack elytra, WingSide side,
	  AerobaticElytraItem item, List<Pair<BannerPattern, DyeColor>> patternColorData
	) {
		final int size = min(visual.max_rendered_banner_layers + 1, patternColorData.size());
		for (int i = 0; i < size; ++i) {
			Pair<BannerPattern, DyeColor> pair = patternColorData.get(i);
			float[] color = pair.getSecond().getTextureDiffuseColors();
			Material material = item.getBannerMaterial(pair.getFirst());
			// Unknown patterns are omitted
			if (material.texture() != MissingTextureAtlasSprite.getLocation()) modelElytra.renderWing(
			  side, mStack, material.buffer(buffer, RenderType::entityTranslucent),
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
	
	@SubscribeEvent
	public static void onAddLayersEvent(EntityRenderersEvent.AddLayers event) {
		final EntityModelSet entityModels = event.getEntityModels();
		final LivingEntityRenderer<ArmorStand, ? extends EntityModel<ArmorStand>> armorStandRenderer =
		  event.getRenderer(EntityType.ARMOR_STAND);
		assert armorStandRenderer != null;
		event.getSkins().forEach(s -> {
			final LivingEntityRenderer<? extends Player, ? extends EntityModel<? extends Player>>
			  skin = event.getSkin(s);
			//noinspection unchecked,rawtypes
			skin.addLayer(new AerobaticElytraLayer<>((LivingEntityRenderer) skin, entityModels));
		});
		//noinspection unchecked,rawtypes
		armorStandRenderer.addLayer(
		  new AerobaticElytraLayer<>((LivingEntityRenderer) armorStandRenderer, entityModels));
		AerobaticElytra.logRegistered("Render Layers");
	}
	
	private static <T extends LivingEntity, M extends EntityModel<T>> void addAerobaticElytraLayer(LivingEntityRenderer<T, M> renderer, EntityModelSet modelSet) {
		renderer.addLayer(new AerobaticElytraLayer<>(renderer, modelSet));
	}
}
