package endorh.aerobatic_elytra.client.render.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.util.math.Rotations;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import static endorh.util.math.Vec3f.TO_RAD;

/**
 * AcrobaticElytraChestModel - EndorH<br>
 * Designed with the help of Tabula 8.0.0<br>
 * Adds a pair of rockets to the back of the player<br>
 * Intended to be combined with {@link AerobaticElytraModel}
 */
@OnlyIn(Dist.CLIENT)
public class AerobaticElytraChestModel<T extends LivingEntity> extends BipedModel<T> {
    public final ModelRenderer leftRocket;
    public final ModelRenderer rightRocket;
    
    /**
     * Build model
     */
    public AerobaticElytraChestModel() {
        super(RenderType::getEntityCutoutNoCull, 1F, 0F, 128, 128);
        textureWidth = 128;
        textureHeight = 64;
    
        // Replace bipedBody with empty model
        this.bipedBody = new ModelRenderer(this, 0, 0);
        this.bipedBody.setRotationPoint(0F, 0F, 0F);
        
        leftRocket = new ModelRenderer(this, 0, 20);
        leftRocket.setRotationPoint(-4.0F, 0.0F, -2.0F);
        leftRocket.addBox(4.5F, 0.8F, 3.0F, 3.0F, 10.0F, 3.0F, -0.5F, -1.2F, -0.5F);
        leftRocket.setTextureOffset(0, 33).addBox(3.5F, 2.6F, 2.0F, 5.0F, 2.0F, 5.0F, -1.0F, -0.2F, -1.0F);
        leftRocket.setTextureOffset(0, 33).addBox(5.5F, 1.1F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        leftRocket.setTextureOffset(0, 35).addBox(5.5F, 9.5F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        rightRocket = new ModelRenderer(this, 20, 20);
        rightRocket.setRotationPoint(-4.0F, 0.0F, -2.0F);
        rightRocket.addBox(0.5F, 0.8F, 3.0F, 3.0F, 10.0F, 3.0F, -0.5F, -1.2F, -0.5F);
        rightRocket.setTextureOffset(20, 33).addBox(-0.5F, 2.6F, 2.0F, 5.0F, 2.0F, 5.0F, -1.0F, -0.2F, -1.0F);
        rightRocket.setTextureOffset(20, 33).addBox(1.5F, 1.1F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        rightRocket.setTextureOffset(20, 35).addBox(1.5F, 9.5F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        
        bipedBody.addChild(leftRocket);
        bipedBody.addChild(rightRocket);
        
        bipedBody.showModel = true;
        
        bipedHead.showModel = false;
        bipedLeftArm.showModel = false;
        bipedRightArm.showModel = false;
        bipedLeftLeg.showModel = false;
        bipedRightLeg.showModel = false;
        bipedHeadwear.showModel = false;
    }

    /*@Override
    public void render(MatrixStack mStack, IVertexBuilder buf, int packedLight, int packedOverlay,
                       float red, float green, float blue, float alpha) {
        super.render(mStack, buf, packedLight, packedOverlay, red, green, blue, alpha);
    }*/
    
    /**
     * Only the bipedBody part can be visible
     */
    @Override public void setVisible(boolean visible) {
        this.bipedBody.showModel = visible;
    }
    
    /**
     * This is a helper function from Tabula to set the rotation of model parts
     */
    public void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
    
    @Override
    public void setRotationAngles(
      @NotNull T entity, float limbSwing, float limbSwingAmount,
      float ageInTicks, float netHeadYaw, float headPitch
    ) {
        super.setRotationAngles(
          entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity instanceof ArmorStandEntity) {
            final Rotations rot = ((ArmorStandEntity) entity).getBodyRotation();
            setRotateAngle(bipedBody, rot.getX() * TO_RAD, rot.getY() * TO_RAD, rot.getZ() * TO_RAD);
        }
    }
}
