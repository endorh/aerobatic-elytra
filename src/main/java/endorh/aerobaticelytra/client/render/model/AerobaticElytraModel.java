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
        textureWidth = 128;
        textureHeight = 64;
        
        leftWing = new ModelRenderer(this, 44, 0);
        leftWing.setRotationPoint(0F, 0F, 0F);
        leftWing.addBox(-15F, -10F, -1F, 20F, 40F, 4F, -4F, -9F, 0F);
        
        leftTip = new ModelRenderer(this, 0, 0);
        leftTip.setRotationPoint(-11F, 21F, 3F);
        leftTip.setTextureOffset(48, 44).addBox(-4F, -3F, -2F, 20F, 16F, 2F, -4F, -3F, 0F);
        setRotateAngle(leftTip, -DEG_180, 0F, 0F);
        leftRoll = new ModelRenderer(this, 0, 0);
        leftRoll.setRotationPoint(0F, 0F, 0F);
        leftRoll.setTextureOffset(92, 44).addBox(-5F, -3F, -2F, 6F, 16F, 2F, -1F, -3F, 0F);
        setRotateAngle(leftRoll, 0F, -DEG_180, 0F);
        leftPitch = new ModelRenderer(this, 0, 0);
        leftPitch.setRotationPoint(-11F, 21F, 3F);
        leftPitch.setTextureOffset(92, 12).addBox(-5F, -23.25F, -2F, 6F, 30F, 2F, -1F, -6.75F, 0F);
        setRotateAngle(leftPitch, 0F, -DEG_180, 0F);
        
        
        rightWing = new ModelRenderer(this, 44, 0);
        rightWing.mirror = true;
        rightWing.setRotationPoint(0F, 0F, 0F);
        rightWing.addBox(-5F, -10F, -1F, 20F, 40F, 4F, -4F, -9F, 0F);
        
        rightTip = new ModelRenderer(this, 0, 0);
        rightTip.mirror = true;
        rightTip.setRotationPoint(11F, 21F, 3F);
        rightTip.setTextureOffset(48, 44).addBox(-16F, -3F, -2F, 20F, 16F, 2F, -4F, -3F, 0F);
        setRotateAngle(rightTip, -DEG_180, 0F, 0F);
        rightRoll = new ModelRenderer(this, 0, 0);
        rightRoll.mirror = true;
        rightRoll.setRotationPoint(0F, 0F, 0F);
        rightRoll.setTextureOffset(92, 44).addBox(-1F, -3F, -2F, 6F, 16F, 2F, -1F, -3F, 0F);
        setRotateAngle(rightRoll, 0F, DEG_180, 0F);
        rightPitch = new ModelRenderer(this, 0, 0);
        rightPitch.mirror = true;
        rightPitch.setRotationPoint(11F, 21F, 3F);
        rightPitch.setTextureOffset(92, 12).addBox(-1F, -23.25F, -2F, 6F, 30F, 2F, -1F, -6.75F, 0F);
        setRotateAngle(rightPitch, 0F, DEG_180, 0F);
    
        leftWing.addChild(this.leftTip);
        leftTip.addChild(this.leftRoll);
        leftWing.addChild(this.leftPitch);
        rightWing.addChild(this.rightTip);
        rightTip.addChild(this.rightRoll);
        rightWing.addChild(this.rightPitch);
        
        leftTip.showModel = false;
        leftPitch.showModel = false;
        leftRoll.showModel = false;
        rightTip.showModel = false;
        rightPitch.showModel = false;
        rightRoll.showModel = false;
        
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
        IVertexBuilder glintBuilder = ItemRenderer.getEntityGlintVertexBuilder(
          buffer, RenderType.getEntityNoOutline(glintTexture), false, true);
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
        mStack.push(); {
            prepareRender(mStack);
            // Extra translation added by the ElytraLayer, instead applied here
            mStack.translate(0.0D, 0.0D, 0.125D);
            super.render(mStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        } mStack.pop();
    }
    
    protected void prepareRender(MatrixStack mStack) {
        if (referenceBodyModel.rotateAngleZ != 0.0F)
            mStack.rotate(Vector3f.ZP.rotation(referenceBodyModel.rotateAngleZ));
        if (referenceBodyModel.rotateAngleY != 0.0F)
            mStack.rotate(Vector3f.YP.rotation(referenceBodyModel.rotateAngleY));
        if (referenceBodyModel.rotateAngleX != 0.0F)
            mStack.rotate(Vector3f.XP.rotation(referenceBodyModel.rotateAngleX));
    }
    
    @NotNull @Override
    protected Iterable<ModelRenderer> getHeadParts() {
        return ImmutableList.of();
    }
    
    @NotNull @Override
    protected Iterable<ModelRenderer> getBodyParts() {
        return reportedBodyParts;
    }
    
    @Override
    public void setRotationAngles(
      @NotNull T entity, float limbSwing, float limbSwingAmount,
      float ageInTicks, float netHeadYaw, float headPitch
    ) {
        if (entity instanceof AbstractClientPlayerEntity) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) entity;
            IFlightData fd = getFlightDataOrDefault(player);
            IElytraPose newPose = fd.getFlightMode().getElytraPose(player);
            AerobaticRenderData smoother = AerobaticRenderData.getAerobaticRenderData(player);
            if (newPose == null) {
                newPose = player.isElytraFlying()
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
                final Rotations leftLegRotation = ((ArmorStandEntity) entity).getLeftLegRotation();
                final Rotations rightLegRotation = ((ArmorStandEntity) entity).getRightLegRotation();
                IElytraPose p = leftLegRotation.getX() >= 30F
                                ? IElytraPose.FLYING_POSE
                                : rightLegRotation.getX() >= 30F
                                  ? IElytraPose.CROUCHING_POSE
                                  : IElytraPose.STANDING_POSE;
                pose = p.getNonNullPose(
                  entity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, ageInTicks);
                Rotations bodyRotation = ((ArmorStandEntity) entity).getBodyRotation();
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
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
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
            textureWidth = 128;
            textureHeight = 64;
        
            leftWing = new ModelRenderer(this, 0, 0);
            leftWing.setRotationPoint(0F, 0F, 0F);
        
            leftRocket = new ModelRenderer(this, 0, 0);
            leftRocket.setRotationPoint(-2.4F, 8.7F, 1.7F);
            leftRocket.setTextureOffset(0, 0)
              .addBox(-1.5F, -7.7F, -0.5F, 3F, 10F, 3F, -0.5F, -1.2F, -0.5F);
            leftRocket.setTextureOffset(0, 13)
              .addBox(-2.5F, -6F, -1.5F, 5F, 2F, 5F, -1F, -0.2F, -1F);
            leftRocket.setTextureOffset(0, 13)
              .addBox(-0.5F, -7.3F, 0.5F, 1F, 1F, 1F, -0.1F, -0.1F, -0.1F);
            leftRocket.setTextureOffset(0, 15)
              .addBox(-0.5F, 1F, 0.5F, 1F, 1F, 1F, -0.1F, -0.1F, -0.1F);
            setRotateAngle(leftRocket, DEG_5, 0F, DEG_15);
        
            rightWing = new ModelRenderer(this, 0, 0);
            rightWing.mirror = true;
            rightWing.setRotationPoint(0F, 0F, 0F);
        
            rightRocket = new ModelRenderer(this, 0, 0);
            rightRocket.setRotationPoint(2.4F, 8.7F, 1.7F);
            rightRocket.setTextureOffset(20, 0)
              .addBox(-1.5F, -7.7F, -0.5F, 3F, 10F, 3F, -0.5F, -1.2F, -0.5F);
            rightRocket.setTextureOffset(20, 13)
              .addBox(-2.5F, -6F, -1.5F, 5F, 2F, 5F, -1F, -0.2F, -1F);
            rightRocket.setTextureOffset(20, 13)
              .addBox(-0.5F, -7.3F, 0.5F, 1F, 1F, 1F, -0.1F, -0.1F, -0.1F);
            rightRocket.setTextureOffset(20, 15)
              .addBox(-0.5F, 1F, 0.5F, 1F, 1F, 1F, -0.1F, -0.1F, -0.1F);
            setRotateAngle(rightRocket, DEG_5, 0F, -DEG_15);
        
            leftWing.addChild(this.leftRocket);
            rightWing.addChild(this.rightRocket);
        }
    
        @NotNull @Override protected Iterable<ModelRenderer> getHeadParts() {
            return ImmutableList.of();
        }
    
        @NotNull @Override protected Iterable<ModelRenderer> getBodyParts() {
            return ImmutableList.of(this.leftWing, this.rightWing);
        }
    
        public void copyAttributes(AerobaticElytraModel<T> elytraModel) {
            elytraModel.copyModelAttributesTo(this);
            leftWing.copyModelAngles(elytraModel.leftWing);
            rightWing.copyModelAngles(elytraModel.rightWing);
        }
    
        @Override
        public void render(
          @NotNull MatrixStack mStack, @NotNull IVertexBuilder buffer, int packedLight,
          int packedOverlay, float red, float green, float blue, float alpha
        ) {
            mStack.push(); {
                parent.prepareRender(mStack);
                super.render(mStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            } mStack.pop();
        }
    
        /**
         * This is a helper function from Tabula to set the rotation of model parts
         */
        public static void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z) {
            modelRenderer.rotateAngleX = x;
            modelRenderer.rotateAngleY = y;
            modelRenderer.rotateAngleZ = z;
        }
    }
    
    // Transformations to AerobaticElytraModelTargetPose
    @SuppressWarnings("SuspiciousNameCombination")
    public void captureSnapshot(final AerobaticElytraModelPose p) {
        p.leftWing.x = leftWing.rotateAngleX;
        p.leftWing.y = leftWing.rotateAngleY;
        p.leftWing.z = leftWing.rotateAngleZ;
        p.leftWing.origin.x = leftWing.rotationPointX;
        p.leftWing.origin.y = leftWing.rotationPointY;
        p.leftWing.origin.z = leftWing.rotationPointZ;
        p.leftTip = leftTip.rotateAngleX;
        p.leftRoll = leftRoll.rotateAngleY;
        p.leftPitch = leftPitch.rotateAngleY;
        p.leftRocket.x = rocketsModel.leftRocket.rotateAngleX;
        p.leftRocket.y = rocketsModel.leftRocket.rotateAngleY;
        p.leftRocket.z = rocketsModel.leftRocket.rotateAngleZ;
        p.rightWing.x = rightWing.rotateAngleX;
        p.rightWing.y = rightWing.rotateAngleY;
        p.rightWing.z = rightWing.rotateAngleZ;
        p.rightWing.origin.x = rightWing.rotationPointX;
        p.rightWing.origin.y = rightWing.rotationPointY;
        p.rightWing.origin.z = rightWing.rotationPointZ;
        p.rightTip = rightTip.rotateAngleX;
        p.rightRoll = rightRoll.rotateAngleY;
        p.rightPitch = rightPitch.rotateAngleY;
        p.rightRocket.x = rocketsModel.rightRocket.rotateAngleX;
        p.rightRocket.y = rocketsModel.rightRocket.rotateAngleY;
        p.rightRocket.z = rocketsModel.rightRocket.rotateAngleZ;
    }
    
    public void interpolate(
      float t, AerobaticElytraModelPose pre, AerobaticElytraModelPose pos
    ) {
        leftWing.rotateAngleX = MathHelper.lerp(t, pre.leftWing.x, pos.leftWing.x);
        leftWing.rotateAngleY = MathHelper.lerp(t, pre.leftWing.y, pos.leftWing.y);
        leftWing.rotateAngleZ = MathHelper.lerp(t, pre.leftWing.z, pos.leftWing.z);
        leftWing.rotationPointX = MathHelper.lerp(t, pre.leftWing.origin.x, pos.leftWing.origin.x);
        leftWing.rotationPointY = MathHelper.lerp(t, pre.leftWing.origin.y, pos.leftWing.origin.y);
        leftWing.rotationPointZ = MathHelper.lerp(t, pre.leftWing.origin.z, pos.leftWing.origin.z);
        leftTip.rotateAngleX = MathHelper.lerp(t, pre.leftTip, pos.leftTip);
        leftRoll.rotateAngleY = MathHelper.lerp(t, pre.leftRoll, pos.leftRoll);
        leftPitch.rotateAngleY = MathHelper.lerp(t, pre.leftPitch, pos.leftPitch);
        
        rightWing.rotateAngleX = MathHelper.lerp(t, pre.rightWing.x, pos.rightWing.x);
        rightWing.rotateAngleY = MathHelper.lerp(t, pre.rightWing.y, pos.rightWing.y);
        rightWing.rotateAngleZ = MathHelper.lerp(t, pre.rightWing.z, pos.rightWing.z);
        rightWing.rotationPointX = MathHelper.lerp(t, pre.rightWing.origin.x, pos.rightWing.origin.x);
        rightWing.rotationPointY = MathHelper.lerp(t, pre.rightWing.origin.y, pos.rightWing.origin.y);
        rightWing.rotationPointZ = MathHelper.lerp(t, pre.rightWing.origin.z, pos.rightWing.origin.z);
        rightTip.rotateAngleX = MathHelper.lerp(t, pre.rightTip, pos.rightTip);
        rightRoll.rotateAngleY = MathHelper.lerp(t, pre.rightRoll, pos.rightRoll);
        rightPitch.rotateAngleY = MathHelper.lerp(t, pre.rightPitch, pos.rightPitch);
    
        rocketsModel.copyAttributes(this);
        rocketsModel.leftRocket.rotateAngleX = MathHelper.lerp(t, pre.leftRocket.x, pos.leftRocket.x);
        rocketsModel.leftRocket.rotateAngleY = MathHelper.lerp(t, pre.leftRocket.y, pos.leftRocket.y);
        rocketsModel.leftRocket.rotateAngleZ = MathHelper.lerp(t, pre.leftRocket.z, pos.leftRocket.z);
        rocketsModel.rightRocket.rotateAngleX = MathHelper.lerp(t, pre.rightRocket.x, pos.rightRocket.x);
        rocketsModel.rightRocket.rotateAngleY = MathHelper.lerp(t, pre.rightRocket.y, pos.rightRocket.y);
        rocketsModel.rightRocket.rotateAngleZ = MathHelper.lerp(t, pre.rightRocket.z, pos.rightRocket.z);
    }
    
    @SuppressWarnings("SuspiciousNameCombination")
    public void update(
      AerobaticElytraModelPose pose
    ) {
        leftWing.rotateAngleX = pose.leftWing.x;
        leftWing.rotateAngleY = pose.leftWing.y;
        leftWing.rotateAngleZ = pose.leftWing.z;
        leftWing.rotationPointX = pose.leftWing.origin.x;
        leftWing.rotationPointY = pose.leftWing.origin.y;
        leftWing.rotationPointZ = pose.leftWing.origin.z;
        leftTip.rotateAngleX = pose.leftTip;
        leftRoll.rotateAngleY = pose.leftRoll;
        leftPitch.rotateAngleY = pose.leftPitch;
        
        rightWing.rotateAngleX = pose.rightWing.x;
        rightWing.rotateAngleY = pose.rightWing.y;
        rightWing.rotateAngleZ = pose.rightWing.z;
        rightWing.rotationPointX = pose.rightWing.origin.x;
        rightWing.rotationPointY = pose.rightWing.origin.y;
        rightWing.rotationPointZ = pose.rightWing.origin.z;
        rightTip.rotateAngleX = pose.rightTip;
        rightRoll.rotateAngleY = pose.rightRoll;
        rightPitch.rotateAngleY = pose.rightPitch;
    
        rocketsModel.copyAttributes(this);
        rocketsModel.leftRocket.rotateAngleX = pose.leftRocket.x;
        rocketsModel.leftRocket.rotateAngleY = pose.leftRocket.y;
        rocketsModel.leftRocket.rotateAngleZ = pose.leftRocket.z;
        rocketsModel.rightRocket.rotateAngleX = pose.rightRocket.x;
        rocketsModel.rightRocket.rotateAngleY = pose.rightRocket.y;
        rocketsModel.rightRocket.rotateAngleZ = pose.rightRocket.z;
    }
    
    public void updateVisibility() {
        leftPitch.showModel = MathHelper.abs(leftPitch.rotateAngleY) <= DEG_175;
        rightPitch.showModel = MathHelper.abs(rightPitch.rotateAngleY) <= DEG_175;
        leftRoll.showModel =
          (MathHelper.abs(leftTip.rotateAngleX) <= DEG_90 || MathHelper.abs(leftRoll.rotateAngleY) <= DEG_90)
          && MathHelper.abs(leftRoll.rotateAngleY) <= DEG_175;
        rightRoll.showModel =
          (MathHelper.abs(rightTip.rotateAngleX) <= DEG_90 || MathHelper.abs(rightRoll.rotateAngleY) <= DEG_90)
          && MathHelper.abs(rightRoll.rotateAngleY) <= DEG_175;
        leftTip.showModel = MathHelper.abs(leftTip.rotateAngleX) <= DEG_175;
        rightTip.showModel = MathHelper.abs(rightTip.rotateAngleX) <= DEG_175;
    }
}
