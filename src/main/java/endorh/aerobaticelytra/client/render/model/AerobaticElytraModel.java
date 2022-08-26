package endorh.aerobaticelytra.client.render.model;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import endorh.aerobaticelytra.client.render.layer.AerobaticRenderData;
import endorh.aerobaticelytra.common.capability.IFlightData;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.util.math.Interpolator;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.ElytraModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Rotations;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static endorh.aerobaticelytra.client.render.model.AerobaticElytraModelPose.ModelRotation.*;
import static endorh.aerobaticelytra.common.capability.FlightDataCapability.getFlightDataOrDefault;

/**
 * AcrobaticElytraModel - EndorH<br>
 * Designed with the help of Tabula 8.0.0<br>
 * Larger Elytra model, with rotating ailerons and wing tips, and
 * a rocket under each wing<br>
 * Uses a child {@link RocketsModel} to render rockets under the wings<br>
 * Intended to be combined with {@link AerobaticElytraChestModel}, which also
 * adds rockets to the player's back
 */
@OnlyIn(Dist.CLIENT)
public class AerobaticElytraModel<T extends LivingEntity> extends ElytraModel<T> {
    public final ModelRenderer leftWing;
    public final ModelRenderer rightWing;
    public final ModelRenderer leftTip;
    public final ModelRenderer leftPitch;
    public final ModelRenderer leftRoll;
    public final ModelRenderer rightTip;
    public final ModelRenderer rightPitch;
    public final ModelRenderer rightRoll;
    protected final ModelRenderer referenceBodyModel;
    
    public RocketsModel<T> rocketsModel;
    
    // The rendering is done in different phases
    //   In order to not rewrite pointlessly super.render()
    //   we simply call it several times reporting different
    //   body parts each time
    // This model has no head parts
    protected List<ModelRenderer> reportedBodyParts;
    
    protected final List<ModelRenderer> leftWingList;
    protected final List<ModelRenderer> rightWingList;
    protected final List<ModelRenderer> bothWingsList;
    
    protected static final float DEG_180 = (float)Math.PI;
    public static final float DEFAULT_ANIMATION_LENGTH = 10F;
    
    /**
     * Build model
     */
    public AerobaticElytraModel() {
        texWidth = 128;
        texHeight = 64;
        
        leftWing = new ModelRenderer(this, 44, 0);
        leftWing.setPos(0F, 0F, 0F);
        leftWing.addBox(-15F, -10F, -1F, 20F, 40F, 4F, -4F, -9F, 0F);
        
        leftTip = new ModelRenderer(this, 0, 0);
        leftTip.setPos(-11F, 21F, 3F);
        leftTip.texOffs(48, 44).addBox(-4F, -3F, -2F, 20F, 16F, 2F, -4F, -3F, 0F);
        setRotateAngle(leftTip, -DEG_180, 0F, 0F);
        leftRoll = new ModelRenderer(this, 0, 0);
        leftRoll.setPos(0F, 0F, 0F);
        leftRoll.texOffs(92, 44).addBox(-5F, -3F, -2F, 6F, 16F, 2F, -1F, -3F, 0F);
        setRotateAngle(leftRoll, 0F, -DEG_180, 0F);
        leftPitch = new ModelRenderer(this, 0, 0);
        leftPitch.setPos(-11F, 21F, 3F);
        leftPitch.texOffs(92, 12).addBox(-5F, -23.25F, -2F, 6F, 30F, 2F, -1F, -6.75F, 0F);
        setRotateAngle(leftPitch, 0F, -DEG_180, 0F);
        
        
        rightWing = new ModelRenderer(this, 44, 0);
        rightWing.mirror = true;
        rightWing.setPos(0F, 0F, 0F);
        rightWing.addBox(-5F, -10F, -1F, 20F, 40F, 4F, -4F, -9F, 0F);
        
        rightTip = new ModelRenderer(this, 0, 0);
        rightTip.mirror = true;
        rightTip.setPos(11F, 21F, 3F);
        rightTip.texOffs(48, 44).addBox(-16F, -3F, -2F, 20F, 16F, 2F, -4F, -3F, 0F);
        setRotateAngle(rightTip, -DEG_180, 0F, 0F);
        rightRoll = new ModelRenderer(this, 0, 0);
        rightRoll.mirror = true;
        rightRoll.setPos(0F, 0F, 0F);
        rightRoll.texOffs(92, 44).addBox(-1F, -3F, -2F, 6F, 16F, 2F, -1F, -3F, 0F);
        setRotateAngle(rightRoll, 0F, DEG_180, 0F);
        rightPitch = new ModelRenderer(this, 0, 0);
        rightPitch.mirror = true;
        rightPitch.setPos(11F, 21F, 3F);
        rightPitch.texOffs(92, 12).addBox(-1F, -23.25F, -2F, 6F, 30F, 2F, -1F, -6.75F, 0F);
        setRotateAngle(rightPitch, 0F, DEG_180, 0F);
    
        leftWing.addChild(this.leftTip);
        leftTip.addChild(this.leftRoll);
        leftWing.addChild(this.leftPitch);
        rightWing.addChild(this.rightTip);
        rightTip.addChild(this.rightRoll);
        rightWing.addChild(this.rightPitch);
        
        leftTip.visible = false;
        leftPitch.visible = false;
        leftRoll.visible = false;
        rightTip.visible = false;
        rightPitch.visible = false;
        rightRoll.visible = false;
        
        rocketsModel = new RocketsModel<>(this);
    
        leftWingList = ImmutableList.of(leftWing);
        rightWingList = ImmutableList.of(rightWing);
        reportedBodyParts = bothWingsList = ImmutableList.of(leftWing, rightWing);
        
        referenceBodyModel = new ModelRenderer(this, 0, 0);
    }
    
    public void renderGlint(
      MatrixStack mStack, IRenderTypeBuffer buffer, int packedLight,
      int packedOverlay, ResourceLocation glintTexture,
      float red, float green, float blue, float alpha
    ) {
        reportedBodyParts = bothWingsList;
        IVertexBuilder glintBuilder = ItemRenderer.getFoilBufferDirect(
          buffer, RenderType.entityNoOutline(glintTexture), false, true);
        doRender(mStack, glintBuilder, packedLight, packedOverlay, red, green, blue, alpha);
    }
    
    public void renderWing(
      WingSide side, MatrixStack mStack, IVertexBuilder buffer, int packedLight,
      int packedOverlay, float red, float green, float blue, float alpha
    ) {
        switch (side) {
            case LEFT: reportedBodyParts = leftWingList; break;
            case RIGHT: reportedBodyParts = rightWingList; break;
            default: return;
        }
        doRender(mStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
    
    protected void doRender(
      MatrixStack mStack, IVertexBuilder buffer, int packedLight, int packedOverlay,
      float red, float green, float blue, float alpha
    ) {
        mStack.pushPose(); {
            prepareRender(mStack);
            // Extra translation added by the ElytraLayer, instead applied here
            mStack.translate(0.0D, 0.0D, 0.125D);
            super.renderToBuffer(mStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        } mStack.popPose();
    }
    
    protected void prepareRender(MatrixStack mStack) {
        if (referenceBodyModel.zRot != 0.0F)
            mStack.mulPose(Vector3f.ZP.rotation(referenceBodyModel.zRot));
        if (referenceBodyModel.yRot != 0.0F)
            mStack.mulPose(Vector3f.YP.rotation(referenceBodyModel.yRot));
        if (referenceBodyModel.xRot != 0.0F)
            mStack.mulPose(Vector3f.XP.rotation(referenceBodyModel.xRot));
    }
    
    @NotNull @Override
    protected Iterable<ModelRenderer> headParts() {
        return ImmutableList.of();
    }
    
    @NotNull @Override
    protected Iterable<ModelRenderer> bodyParts() {
        return reportedBodyParts;
    }
    
    @Override
    public void setupAnim(
      @NotNull T entity, float limbSwing, float limbSwingAmount,
      float ageInTicks, float netHeadYaw, float headPitch
    ) {
        if (entity instanceof AbstractClientPlayerEntity) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) entity;
            IFlightData fd = getFlightDataOrDefault(player);
            IElytraPose newPose = fd.getFlightMode().getElytraPose(player);
            AerobaticRenderData smoother = AerobaticRenderData.getAerobaticRenderData(player);
            if (newPose == null) {
                newPose = player.isFallFlying()
                          ? IElytraPose.FLYING_POSE
                          : player.isCrouching()
                            ? IElytraPose.CROUCHING_POSE
                            : IElytraPose.STANDING_POSE;
            }
            final IElytraPose prevPose = smoother.pose;
            if (ageInTicks - smoother.animationStart < 0F)
                smoother.animationStart = 0F;
            float t = (ageInTicks - smoother.animationStart) / smoother.animationLength;
            if (smoother.updatePose(newPose)) {
                final AerobaticElytraModelPose prev = prevPose.getNonNullPose(
                  entity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, ageInTicks);
                if (t < 1)
                    interpolate(Interpolator.quadOut(t), smoother.capturedPose, prev);
                else update(prev);
                captureSnapshot(smoother.capturedPose);
                newPose.modifyPrevious(smoother.capturedPose);
                smoother.animationStart = ageInTicks;
                float temp = newPose.getFadeInTime();
                if (Float.isNaN(temp)) {
                    temp = prevPose.getFadeOutTime();
                    if (Float.isNaN(temp))
                        temp = DEFAULT_ANIMATION_LENGTH;
                }
                smoother.animationLength = temp;
                t = 0F;
            }
            final AerobaticElytraModelPose targetPose = smoother.pose.getNonNullPose(
              entity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, ageInTicks);
            if (t < 1)
                interpolate(Interpolator.quadOut(t), smoother.capturedPose, targetPose);
            else update(targetPose);
            updateVisibility();
            
            setRotateAngle(referenceBodyModel, 0F, 0F, 0F);
        } else {
            AerobaticElytraModelPose pose;
            if (entity instanceof ArmorStandEntity) {
                final Rotations leftLegRotation = ((ArmorStandEntity) entity).getLeftLegPose();
                final Rotations rightLegRotation = ((ArmorStandEntity) entity).getRightLegPose();
                IElytraPose p = leftLegRotation.getX() >= 30F
                                ? IElytraPose.FLYING_POSE
                                : rightLegRotation.getX() >= 30F
                                  ? IElytraPose.CROUCHING_POSE
                                  : IElytraPose.STANDING_POSE;
                pose = p.getNonNullPose(
                  entity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, ageInTicks);
                Rotations bodyRotation = ((ArmorStandEntity) entity).getBodyPose();
                setRotateAngle(
                  referenceBodyModel, bodyRotation.getX() * TO_RAD,
                  bodyRotation.getY() * TO_RAD, bodyRotation.getZ() * TO_RAD);
                update(pose);
                updateVisibility();
            } else {
                pose = IElytraPose.STANDING_POSE.getNonNullPose(
                  entity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, ageInTicks);
                update(pose);
            }
        }
    }
    
    /**
     * This is a helper function from Tabula to set the rotation of model parts
     */
    public static void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }
    
    /**
     * AcrobaticElytraModel.RocketsModel - EndorH<br>
     * Designed with the help of Tabula 8.0.0<br>
     * Child model for {@link AerobaticElytraModel}
     * which adds a pair on rockets on each wing.<br>
     * It's not intended to be used on its own.
     * {@code setRotationAngles} won't do anything.<br>
     * Its angles are updated by the parent {@code AcrobaticElytraModel}
     */
    @OnlyIn(Dist.CLIENT)
    public static class RocketsModel<T extends LivingEntity> extends ElytraModel<T> {
        public ModelRenderer leftWing;
        public ModelRenderer leftRocket;
        public ModelRenderer rightWing;
        public ModelRenderer rightRocket;
        public AerobaticElytraModel<T> parent;
    
        private static final float DEG_5 = (float) (Math.PI / 36D);
        private static final float DEG_15 = (float) (Math.PI / 12D);
    
        /**
         * Build model
         */
        protected RocketsModel(AerobaticElytraModel<T> parent) {
            this.parent = parent;
            texWidth = 128;
            texHeight = 64;
        
            leftWing = new ModelRenderer(this, 0, 0);
            leftWing.setPos(0F, 0F, 0F);
        
            leftRocket = new ModelRenderer(this, 0, 0);
            leftRocket.setPos(-2.4F, 8.7F, 1.7F);
            leftRocket.texOffs(0, 0)
              .addBox(-1.5F, -7.7F, -0.5F, 3F, 10F, 3F, -0.5F, -1.2F, -0.5F);
            leftRocket.texOffs(0, 13)
              .addBox(-2.5F, -6F, -1.5F, 5F, 2F, 5F, -1F, -0.2F, -1F);
            leftRocket.texOffs(0, 13)
              .addBox(-0.5F, -7.3F, 0.5F, 1F, 1F, 1F, -0.1F, -0.1F, -0.1F);
            leftRocket.texOffs(0, 15)
              .addBox(-0.5F, 1F, 0.5F, 1F, 1F, 1F, -0.1F, -0.1F, -0.1F);
            setRotateAngle(leftRocket, DEG_5, 0F, DEG_15);
        
            rightWing = new ModelRenderer(this, 0, 0);
            rightWing.mirror = true;
            rightWing.setPos(0F, 0F, 0F);
        
            rightRocket = new ModelRenderer(this, 0, 0);
            rightRocket.setPos(2.4F, 8.7F, 1.7F);
            rightRocket.texOffs(20, 0)
              .addBox(-1.5F, -7.7F, -0.5F, 3F, 10F, 3F, -0.5F, -1.2F, -0.5F);
            rightRocket.texOffs(20, 13)
              .addBox(-2.5F, -6F, -1.5F, 5F, 2F, 5F, -1F, -0.2F, -1F);
            rightRocket.texOffs(20, 13)
              .addBox(-0.5F, -7.3F, 0.5F, 1F, 1F, 1F, -0.1F, -0.1F, -0.1F);
            rightRocket.texOffs(20, 15)
              .addBox(-0.5F, 1F, 0.5F, 1F, 1F, 1F, -0.1F, -0.1F, -0.1F);
            setRotateAngle(rightRocket, DEG_5, 0F, -DEG_15);
        
            leftWing.addChild(this.leftRocket);
            rightWing.addChild(this.rightRocket);
        }
    
        @NotNull @Override protected Iterable<ModelRenderer> headParts() {
            return ImmutableList.of();
        }
    
        @NotNull @Override protected Iterable<ModelRenderer> bodyParts() {
            return ImmutableList.of(this.leftWing, this.rightWing);
        }
    
        public void copyAttributes(AerobaticElytraModel<T> elytraModel) {
            elytraModel.copyPropertiesTo(this);
            leftWing.copyFrom(elytraModel.leftWing);
            rightWing.copyFrom(elytraModel.rightWing);
        }
    
        @Override
        public void renderToBuffer(
          @NotNull MatrixStack mStack, @NotNull IVertexBuilder buffer, int packedLight,
          int packedOverlay, float red, float green, float blue, float alpha
        ) {
            mStack.pushPose(); {
                parent.prepareRender(mStack);
                super.renderToBuffer(mStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            } mStack.popPose();
        }
    
        /**
         * This is a helper function from Tabula to set the rotation of model parts
         */
        public static void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z) {
            modelRenderer.xRot = x;
            modelRenderer.yRot = y;
            modelRenderer.zRot = z;
        }
    }
    
    // Transformations to AerobaticElytraModelTargetPose
    @SuppressWarnings("SuspiciousNameCombination")
    public void captureSnapshot(final AerobaticElytraModelPose p) {
        p.leftWing.x = leftWing.xRot;
        p.leftWing.y = leftWing.yRot;
        p.leftWing.z = leftWing.zRot;
        p.leftWing.origin.x = leftWing.x;
        p.leftWing.origin.y = leftWing.y;
        p.leftWing.origin.z = leftWing.z;
        p.leftTip = leftTip.xRot;
        p.leftRoll = leftRoll.yRot;
        p.leftPitch = leftPitch.yRot;
        p.leftRocket.x = rocketsModel.leftRocket.xRot;
        p.leftRocket.y = rocketsModel.leftRocket.yRot;
        p.leftRocket.z = rocketsModel.leftRocket.zRot;
        p.rightWing.x = rightWing.xRot;
        p.rightWing.y = rightWing.yRot;
        p.rightWing.z = rightWing.zRot;
        p.rightWing.origin.x = rightWing.x;
        p.rightWing.origin.y = rightWing.y;
        p.rightWing.origin.z = rightWing.z;
        p.rightTip = rightTip.xRot;
        p.rightRoll = rightRoll.yRot;
        p.rightPitch = rightPitch.yRot;
        p.rightRocket.x = rocketsModel.rightRocket.xRot;
        p.rightRocket.y = rocketsModel.rightRocket.yRot;
        p.rightRocket.z = rocketsModel.rightRocket.zRot;
    }
    
    public void interpolate(
      float t, AerobaticElytraModelPose pre, AerobaticElytraModelPose pos
    ) {
        leftWing.xRot = MathHelper.lerp(t, pre.leftWing.x, pos.leftWing.x);
        leftWing.yRot = MathHelper.lerp(t, pre.leftWing.y, pos.leftWing.y);
        leftWing.zRot = MathHelper.lerp(t, pre.leftWing.z, pos.leftWing.z);
        leftWing.x = MathHelper.lerp(t, pre.leftWing.origin.x, pos.leftWing.origin.x);
        leftWing.y = MathHelper.lerp(t, pre.leftWing.origin.y, pos.leftWing.origin.y);
        leftWing.z = MathHelper.lerp(t, pre.leftWing.origin.z, pos.leftWing.origin.z);
        leftTip.xRot = MathHelper.lerp(t, pre.leftTip, pos.leftTip);
        leftRoll.yRot = MathHelper.lerp(t, pre.leftRoll, pos.leftRoll);
        leftPitch.yRot = MathHelper.lerp(t, pre.leftPitch, pos.leftPitch);
        
        rightWing.xRot = MathHelper.lerp(t, pre.rightWing.x, pos.rightWing.x);
        rightWing.yRot = MathHelper.lerp(t, pre.rightWing.y, pos.rightWing.y);
        rightWing.zRot = MathHelper.lerp(t, pre.rightWing.z, pos.rightWing.z);
        rightWing.x = MathHelper.lerp(t, pre.rightWing.origin.x, pos.rightWing.origin.x);
        rightWing.y = MathHelper.lerp(t, pre.rightWing.origin.y, pos.rightWing.origin.y);
        rightWing.z = MathHelper.lerp(t, pre.rightWing.origin.z, pos.rightWing.origin.z);
        rightTip.xRot = MathHelper.lerp(t, pre.rightTip, pos.rightTip);
        rightRoll.yRot = MathHelper.lerp(t, pre.rightRoll, pos.rightRoll);
        rightPitch.yRot = MathHelper.lerp(t, pre.rightPitch, pos.rightPitch);
    
        rocketsModel.copyAttributes(this);
        rocketsModel.leftRocket.xRot = MathHelper.lerp(t, pre.leftRocket.x, pos.leftRocket.x);
        rocketsModel.leftRocket.yRot = MathHelper.lerp(t, pre.leftRocket.y, pos.leftRocket.y);
        rocketsModel.leftRocket.zRot = MathHelper.lerp(t, pre.leftRocket.z, pos.leftRocket.z);
        rocketsModel.rightRocket.xRot = MathHelper.lerp(t, pre.rightRocket.x, pos.rightRocket.x);
        rocketsModel.rightRocket.yRot = MathHelper.lerp(t, pre.rightRocket.y, pos.rightRocket.y);
        rocketsModel.rightRocket.zRot = MathHelper.lerp(t, pre.rightRocket.z, pos.rightRocket.z);
    }
    
    @SuppressWarnings("SuspiciousNameCombination")
    public void update(
      AerobaticElytraModelPose pose
    ) {
        leftWing.xRot = pose.leftWing.x;
        leftWing.yRot = pose.leftWing.y;
        leftWing.zRot = pose.leftWing.z;
        leftWing.x = pose.leftWing.origin.x;
        leftWing.y = pose.leftWing.origin.y;
        leftWing.z = pose.leftWing.origin.z;
        leftTip.xRot = pose.leftTip;
        leftRoll.yRot = pose.leftRoll;
        leftPitch.yRot = pose.leftPitch;
        
        rightWing.xRot = pose.rightWing.x;
        rightWing.yRot = pose.rightWing.y;
        rightWing.zRot = pose.rightWing.z;
        rightWing.x = pose.rightWing.origin.x;
        rightWing.y = pose.rightWing.origin.y;
        rightWing.z = pose.rightWing.origin.z;
        rightTip.xRot = pose.rightTip;
        rightRoll.yRot = pose.rightRoll;
        rightPitch.yRot = pose.rightPitch;
    
        rocketsModel.copyAttributes(this);
        rocketsModel.leftRocket.xRot = pose.leftRocket.x;
        rocketsModel.leftRocket.yRot = pose.leftRocket.y;
        rocketsModel.leftRocket.zRot = pose.leftRocket.z;
        rocketsModel.rightRocket.xRot = pose.rightRocket.x;
        rocketsModel.rightRocket.yRot = pose.rightRocket.y;
        rocketsModel.rightRocket.zRot = pose.rightRocket.z;
    }
    
    public void updateVisibility() {
        leftPitch.visible = MathHelper.abs(leftPitch.yRot) <= DEG_175;
        rightPitch.visible = MathHelper.abs(rightPitch.yRot) <= DEG_175;
        leftRoll.visible =
          (MathHelper.abs(leftTip.xRot) <= DEG_90 || MathHelper.abs(leftRoll.yRot) <= DEG_90)
          && MathHelper.abs(leftRoll.yRot) <= DEG_175;
        rightRoll.visible =
          (MathHelper.abs(rightTip.xRot) <= DEG_90 || MathHelper.abs(rightRoll.yRot) <= DEG_90)
          && MathHelper.abs(rightRoll.yRot) <= DEG_175;
        leftTip.visible = MathHelper.abs(leftTip.xRot) <= DEG_175;
        rightTip.visible = MathHelper.abs(rightTip.xRot) <= DEG_175;
    }
}
