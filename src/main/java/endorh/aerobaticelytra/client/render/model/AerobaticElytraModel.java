package endorh.aerobaticelytra.client.render.model;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.render.layer.AerobaticRenderData;
import endorh.aerobaticelytra.common.capability.IFlightData;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.util.math.Interpolator;
import endorh.util.math.Vec3f;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Rotations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static endorh.aerobaticelytra.client.render.model.AerobaticElytraModelPose.ModelRotation.*;
import static endorh.aerobaticelytra.common.capability.FlightDataCapability.getFlightDataOrDefault;
import static net.minecraft.util.Mth.abs;
import static net.minecraft.util.Mth.lerp;

/**
 * Acrnet.minecraft.util.Mth Designed with the help of Tabula 8.0.0<br>
 * Larger Elytra model, with rotating ailerons and wing tips, and
 * a rocket under each wing<br>
 * Intended to be combined with {@link AerobaticElytraChestModel}, which also
 * adds rockets to the player's back
 */
@EventBusSubscriber(value=Dist.CLIENT, bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
@OnlyIn(Dist.CLIENT)
public class AerobaticElytraModel<T extends LivingEntity> extends ElytraModel<T> {
    public static final ModelLayerLocation AEROBATIC_ELYTRA_LAYER =
      new ModelLayerLocation(new ResourceLocation("player"), "aerobatic_elytra");
    
    public static final String LEFT_WING = "left_wing";
    public static final String RIGHT_WING = "right_wing";
    public static final String WING = "wing";
    public static final String TIP = "tip";
    public static final String PITCH = "pitch";
    public static final String ROLL = "roll";
    public static final String ROCKET = "rocket";
    
    public final ModelPart leftWing;
    public final ModelPart rightWing;
    public final ModelPart leftWingWing;
    public final ModelPart rightWingWing;
    public final ModelPart leftTip;
    public final ModelPart leftPitch;
    public final ModelPart leftRoll;
    public final ModelPart rightTip;
    public final ModelPart rightPitch;
    public final ModelPart rightRoll;
    public final ModelPart leftRocket;
    public final ModelPart rightRocket;
    
    private static final float DEG_5 = (float) (Math.PI / 36D);
    private static final float DEG_15 = (float) (Math.PI / 12D);
    
    protected final Vec3f bodyRotation = Vec3f.ZERO.get();
    
    // The rendering is done in different phases
    //   In order to not rewrite pointlessly super.render()
    //   we simply call it several times reporting different
    //   body parts each time
    // This model has no head parts
    protected List<ModelPart> reportedBodyParts;
    
    protected final List<ModelPart> leftWingList;
    protected final List<ModelPart> rightWingList;
    protected final List<ModelPart> bothWingsList;
    
    protected static final float DEG_180 = (float) Math.PI;
    public static final float DEFAULT_ANIMATION_LENGTH = 10F;
    
    @SubscribeEvent
    public static void registerLayerDefinition(RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AEROBATIC_ELYTRA_LAYER, AerobaticElytraModel::createLayer);
    }
    
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        
        // @formatter:off
        // Left wing
        PartDefinition left = root.addOrReplaceChild(LEFT_WING, CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition leftWing = left.addOrReplaceChild(
          WING, CubeListBuilder.create()
            .texOffs(44, 0).addBox(-15F, -10F, -1F, 20F, 40F, 4F, new CubeDeformation(-4F, -9F, 0F)),
          PartPose.ZERO);
        // Left parts
        PartDefinition leftTip = leftWing.addOrReplaceChild(
          TIP, CubeListBuilder.create()
            .texOffs(48, 44).addBox(-4F, -3F, -2F, 20F, 16F, 2F, new CubeDeformation(-4F, -3F, 0F)),
          PartPose.offsetAndRotation(-11F, 21F, 3F, -DEG_180, 0F, 0F));
        leftTip.addOrReplaceChild(
          ROLL, CubeListBuilder.create()
            .texOffs(92, 44).addBox(-5F, -3F, -2F, 6F, 16F, 2F, new CubeDeformation(-1F, -3F, 0F)),
          PartPose.offsetAndRotation(0F, 0F, 0F, 0F, -DEG_180, 0F));
        leftWing.addOrReplaceChild(
          PITCH, CubeListBuilder.create()
            .texOffs(92, 12).addBox(-5F, -23.25F, -2F, 6F, 30F, 2F, new CubeDeformation(-1F, -6.75F, 0F)),
          PartPose.offsetAndRotation(-11F, 21F, 3F, 0F, -DEG_180, 0F));
        
        // Right wing
        PartDefinition right = root.addOrReplaceChild(
          RIGHT_WING, CubeListBuilder.create().mirror(),
          PartPose.offset(0F, 0F, 0F));
        PartDefinition rightWing = right.addOrReplaceChild(
          WING, CubeListBuilder.create().mirror()
            .texOffs(44, 0).addBox(-5F, -10F, -1F, 20F, 40F, 4F, new CubeDeformation(-4F, -9F, 0F)),
          PartPose.ZERO);
        // Right parts
        PartDefinition rightTip = rightWing.addOrReplaceChild(
          TIP, CubeListBuilder.create().mirror()
            .texOffs(48, 44).addBox(-16F, -3F, -2F, 20F, 16F, 2F, new CubeDeformation(-4F, -3F, 0F)),
          PartPose.offsetAndRotation(11F, 21F, 3F, -DEG_180, 0F, 0F));
        rightTip.addOrReplaceChild(
          ROLL, CubeListBuilder.create().mirror()
            .texOffs(92, 44).addBox(-1F, -3F, -2F, 6F, 16F, 2F, new CubeDeformation(-1F, -3F, 0F)),
          PartPose.offsetAndRotation(0F, 0F, 0F, 0F, DEG_180, 0F));
        rightWing.addOrReplaceChild(
          PITCH, CubeListBuilder.create().mirror()
            .texOffs(92, 12).addBox(-1F, -23.25F, -2F, 6F, 30F, 2F, new CubeDeformation(-1F, -6.75F, 0F)),
          PartPose.offsetAndRotation(11F, 21F, 3F, 0F, DEG_180, 0F));
        
        // Left rocket
        left.addOrReplaceChild(
          ROCKET, CubeListBuilder.create()
            .texOffs(0,  0).addBox(-1.5F, -7.7F, -0.5F, 3F, 10F, 3F, new CubeDeformation(-0.5F, -1.2F, -0.5F))
            .texOffs(0, 13).addBox(-2.5F, -6.0F, -1.5F, 5F,  2F, 5F, new CubeDeformation(-1.0F, -0.2F, -1.0F))
            .texOffs(0, 13).addBox(-0.5F, -7.3F,  0.5F, 1F,  1F, 1F, new CubeDeformation(-0.1F, -0.1F, -0.1F))
            .texOffs(0, 15).addBox(-0.5F,  1.0F,  0.5F, 1F,  1F, 1F, new CubeDeformation(-0.1F, -0.1F, -0.1F)),
          PartPose.offsetAndRotation(-2.4F, 8.7F, 1.7F, DEG_5, 0F, DEG_15));
        // Right rocket
        right.addOrReplaceChild(
          ROCKET, CubeListBuilder.create()
            .texOffs(20,  0).addBox(-1.5F, -7.7F, -0.5F, 3F, 10F, 3F, new CubeDeformation(-0.5F, -1.2F, -0.5F))
            .texOffs(20, 13).addBox(-2.5F, -6.0F, -1.5F, 5F,  2F, 5F, new CubeDeformation(-1.0F, -0.2F, -1.0F))
            .texOffs(20, 13).addBox(-0.5F, -7.3F,  0.5F, 1F,  1F, 1F, new CubeDeformation(-0.1F, -0.1F, -0.1F))
            .texOffs(20, 15).addBox(-0.5F,  1.0F,  0.5F, 1F,  1F, 1F, new CubeDeformation(-0.1F, -0.1F, -0.1F)),
          PartPose.offsetAndRotation(2.4F, 8.7F, 1.7F, DEG_5, 0F, -DEG_15));
        
        // @formatter:on
        
        // Add empty parent parts
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        
        return LayerDefinition.create(mesh, 128, 64);
    }
    
    /**
     * Model definition
     */
    public AerobaticElytraModel(ModelPart modelPart) {
        super(modelPart);
        
        leftWing = modelPart.getChild(LEFT_WING);
        rightWing = modelPart.getChild(RIGHT_WING);
        
        leftWingWing = leftWing.getChild(WING);
        rightWingWing = rightWing.getChild(WING);
        
        leftTip = leftWingWing.getChild(TIP);
        leftRoll = leftTip.getChild(ROLL);
        leftPitch = leftWingWing.getChild(PITCH);
        
        rightTip = rightWingWing.getChild(TIP);
        rightRoll = rightTip.getChild(ROLL);
        rightPitch = rightWingWing.getChild(PITCH);
        
        leftRocket = leftWing.getChild(ROCKET);
        rightRocket = rightWing.getChild(ROCKET);
        
        leftWingList = ImmutableList.of(leftWing);
        rightWingList = ImmutableList.of(rightWing);
        reportedBodyParts = bothWingsList = ImmutableList.of(leftWing, rightWing);
        
        initVisibility();
    }
    
    protected void initVisibility() {
        leftWingWing.visible = true;
        rightWingWing.visible = true;
        leftTip.visible = false;
        leftPitch.visible = false;
        leftRoll.visible = false;
        rightTip.visible = false;
        rightPitch.visible = false;
        rightRoll.visible = false;
    }
    
    public void renderGlint(
      PoseStack mStack, MultiBufferSource buffer, int packedLight,
      int packedOverlay, ResourceLocation glintTexture,
      float red, float green, float blue, float alpha
    ) {
        reportedBodyParts = bothWingsList;
        setupRenderVisibility(true, false);
        VertexConsumer glintBuilder = ItemRenderer.getFoilBufferDirect(
          buffer, RenderType.entityNoOutline(glintTexture), false, true);
        doRender(mStack, glintBuilder, packedLight, packedOverlay, red, green, blue, alpha);
    }
    
    public void renderWing(
      WingSide side, PoseStack mStack, VertexConsumer buffer, int packedLight,
      int packedOverlay, float red, float green, float blue, float alpha
    ) {
        reportedBodyParts = switch (side) {
            case LEFT -> leftWingList;
            case RIGHT -> rightWingList;
        };
        setupRenderVisibility(true, false);
        doRender(mStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
    
    public void renderRockets(
      @NotNull PoseStack mStack, @NotNull VertexConsumer buffer, int packedLight,
      int packedOverlay, float red, float green, float blue, float alpha
    ) {
        reportedBodyParts = bothWingsList;
        setupRenderVisibility(false, true);
        mStack.pushPose(); {
            prepareRender(mStack);
            super.renderToBuffer(mStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        } mStack.popPose();
    }
    
    protected void doRender(
      PoseStack mStack, VertexConsumer buffer, int packedLight, int packedOverlay,
      float red, float green, float blue, float alpha
    ) {
        mStack.pushPose();
        {
            prepareRender(mStack);
            // Extra translation added by the ElytraLayer, instead applied here
            mStack.translate(0.0D, 0.0D, 0.125D);
            super.renderToBuffer(mStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
        mStack.popPose();
    }
    
    protected void prepareRender(PoseStack mStack) {
        if (bodyRotation.z != 0.0F)
            mStack.mulPose(Vector3f.ZP.rotation(bodyRotation.z));
        if (bodyRotation.y != 0.0F)
            mStack.mulPose(Vector3f.YP.rotation(bodyRotation.y));
        if (bodyRotation.x != 0.0F)
            mStack.mulPose(Vector3f.XP.rotation(bodyRotation.x));
    }
    
    @NotNull @Override
    protected Iterable<ModelPart> headParts() {
        return ImmutableList.of();
    }
    
    @NotNull @Override
    protected Iterable<ModelPart> bodyParts() {
        return reportedBodyParts;
    }
    
    @Override
    public void setupAnim(
      @NotNull T entity, float limbSwing, float limbSwingAmount,
      float ageInTicks, float netHeadYaw, float headPitch
    ) {
        if (entity instanceof AbstractClientPlayer player) {
            IFlightData fd = getFlightDataOrDefault(player);
            IElytraPose newPose = fd.getFlightMode().getElytraPose(player);
            AerobaticRenderData smoother = AerobaticRenderData.getAerobaticRenderData(player);
            if (newPose == null) newPose =
              player.isFallFlying()? IElytraPose.FLYING_POSE :
              player.isCrouching()? IElytraPose.CROUCHING_POSE : IElytraPose.STANDING_POSE;
            final IElytraPose prevPose = smoother.pose;
            if (ageInTicks - smoother.animationStart < 0F) smoother.animationStart = 0F;
            float t = (ageInTicks - smoother.animationStart) / smoother.animationLength;
            if (smoother.updatePose(newPose)) {
                final AerobaticElytraModelPose prev = prevPose.getNonNullPose(
                  entity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, ageInTicks);
                if (t < 1) {
                    interpolate(Interpolator.quadOut(t), smoother.capturedPose, prev);
                } else update(prev);
                captureSnapshot(smoother.capturedPose);
                newPose.modifyPrevious(smoother.capturedPose);
                smoother.animationStart = ageInTicks;
                float temp = newPose.getFadeInTime();
                if (Float.isNaN(temp)) {
                    temp = prevPose.getFadeOutTime();
                    if (Float.isNaN(temp)) temp = DEFAULT_ANIMATION_LENGTH;
                }
                smoother.animationLength = temp;
                t = 0F;
            }
            final AerobaticElytraModelPose targetPose = smoother.pose.getNonNullPose(
              entity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, ageInTicks);
            if (t < 1) {
                interpolate(Interpolator.quadOut(t), smoother.capturedPose, targetPose);
            } else update(targetPose);
            updatePartVisibility();
            
            bodyRotation.set(0F, 0F, 0F);
        } else {
            AerobaticElytraModelPose pose;
            if (entity instanceof ArmorStand) {
                final Rotations leftLegRotation = ((ArmorStand) entity).getLeftLegPose();
                final Rotations rightLegRotation = ((ArmorStand) entity).getRightLegPose();
                IElytraPose p =
                  leftLegRotation.getX() >= 30F? IElytraPose.FLYING_POSE :
                  rightLegRotation.getX() >= 30F? IElytraPose.CROUCHING_POSE : IElytraPose.STANDING_POSE;
                pose = p.getNonNullPose(
                  entity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, ageInTicks);
                Rotations bodyPose = ((ArmorStand) entity).getBodyPose();
                bodyRotation.set(
                  bodyPose.getX() * TO_RAD, bodyPose.getY() * TO_RAD, bodyPose.getZ() * TO_RAD);
                update(pose);
                updatePartVisibility();
            } else {
                pose = IElytraPose.STANDING_POSE.getNonNullPose(
                  entity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, ageInTicks);
                update(pose);
            }
        }
    }
    
    // Transformations to AerobaticElytraModelTargetPose
    @SuppressWarnings("SuspiciousNameCombination")
    public void captureSnapshot(final AerobaticElytraModelPose p) {
        p.leftWing.copyOffsetAndRotation(leftWing);
        p.rightWing.copyOffsetAndRotation(rightWing);
        p.leftTip = leftTip.xRot;
        p.leftRoll = leftRoll.yRot;
        p.leftPitch = leftPitch.yRot;
        p.leftRocket.copyRotation(leftRocket);
        p.rightTip = rightTip.xRot;
        p.rightRoll = rightRoll.yRot;
        p.rightPitch = rightPitch.yRot;
        p.rightRocket.copyRotation(rightRocket);
    }
    
    public void interpolate(
      float t, AerobaticElytraModelPose pre, AerobaticElytraModelPose pos
    ) {
        leftWing.xRot = lerp(t, pre.leftWing.x, pos.leftWing.x);
        leftWing.yRot = lerp(t, pre.leftWing.y, pos.leftWing.y);
        leftWing.zRot = lerp(t, pre.leftWing.z, pos.leftWing.z);
        leftWing.x = lerp(t, pre.leftWing.origin.x, pos.leftWing.origin.x);
        leftWing.y = lerp(t, pre.leftWing.origin.y, pos.leftWing.origin.y);
        leftWing.z = lerp(t, pre.leftWing.origin.z, pos.leftWing.origin.z);
        leftTip.xRot = lerp(t, pre.leftTip, pos.leftTip);
        leftRoll.yRot = lerp(t, pre.leftRoll, pos.leftRoll);
        leftPitch.yRot = lerp(t, pre.leftPitch, pos.leftPitch);
        
        rightWing.xRot = lerp(t, pre.rightWing.x, pos.rightWing.x);
        rightWing.yRot = lerp(t, pre.rightWing.y, pos.rightWing.y);
        rightWing.zRot = lerp(t, pre.rightWing.z, pos.rightWing.z);
        rightWing.x = lerp(t, pre.rightWing.origin.x, pos.rightWing.origin.x);
        rightWing.y = lerp(t, pre.rightWing.origin.y, pos.rightWing.origin.y);
        rightWing.z = lerp(t, pre.rightWing.origin.z, pos.rightWing.origin.z);
        rightTip.xRot = lerp(t, pre.rightTip, pos.rightTip);
        rightRoll.yRot = lerp(t, pre.rightRoll, pos.rightRoll);
        rightPitch.yRot = lerp(t, pre.rightPitch, pos.rightPitch);
        
        leftRocket.xRot = lerp(t, pre.leftRocket.x, pos.leftRocket.x);
        leftRocket.yRot = lerp(t, pre.leftRocket.y, pos.leftRocket.y);
        leftRocket.zRot = lerp(t, pre.leftRocket.z, pos.leftRocket.z);
        rightRocket.xRot = lerp(t, pre.rightRocket.x, pos.rightRocket.x);
        rightRocket.yRot = lerp(t, pre.rightRocket.y, pos.rightRocket.y);
        rightRocket.zRot = lerp(t, pre.rightRocket.z, pos.rightRocket.z);
    }
    
    @SuppressWarnings("SuspiciousNameCombination")
    public void update(
      AerobaticElytraModelPose pose
    ) {
        pose.leftWing.applyOffsetAndRotation(leftWing);
        leftTip.xRot = pose.leftTip;
        leftRoll.yRot = pose.leftRoll;
        leftPitch.yRot = pose.leftPitch;
        
        pose.rightWing.applyOffsetAndRotation(rightWing);
        rightTip.xRot = pose.rightTip;
        rightRoll.yRot = pose.rightRoll;
        rightPitch.yRot = pose.rightPitch;
        
        pose.leftRocket.applyRotation(leftRocket);
        pose.rightRocket.applyRotation(rightRocket);
    }
    
    public void setupRenderVisibility(boolean renderWings, boolean renderRockets) {
        leftWingWing.visible = renderWings;
        rightWingWing.visible = renderWings;
        leftRocket.visible = renderRockets;
        rightRocket.visible = renderRockets;
    }
    
    public void updatePartVisibility() {
        leftPitch.visible = abs(leftPitch.yRot) <= DEG_175;
        rightPitch.visible = abs(rightPitch.yRot) <= DEG_175;
        leftRoll.visible =
          (abs(leftTip.xRot) <= DEG_90 || abs(leftRoll.yRot) <= DEG_90)
          && abs(leftRoll.yRot) <= DEG_175;
        rightRoll.visible =
          (abs(rightTip.xRot) <= DEG_90 || abs(rightRoll.yRot) <= DEG_90)
          && abs(rightRoll.yRot) <= DEG_175;
        leftTip.visible = abs(leftTip.xRot) <= DEG_175;
        rightTip.visible = abs(rightTip.xRot) <= DEG_175;
    }
}
