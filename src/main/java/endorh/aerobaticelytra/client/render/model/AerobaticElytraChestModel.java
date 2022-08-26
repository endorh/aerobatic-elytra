package endorh.aerobaticelytra.client.render.model;

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
        super(RenderType::entityCutoutNoCull, 1F, 0F, 128, 128);
        texWidth = 128;
        texHeight = 64;
    
        // Replace bipedBody with empty model
        this.body = new ModelRenderer(this, 0, 0);
        this.body.setPos(0F, 0F, 0F);
        
        leftRocket = new ModelRenderer(this, 0, 20);
        leftRocket.setPos(-4.0F, 0.0F, -2.0F);
        leftRocket.addBox(4.5F, 0.8F, 3.0F, 3.0F, 10.0F, 3.0F, -0.5F, -1.2F, -0.5F);
        leftRocket.texOffs(0, 33).addBox(3.5F, 2.6F, 2.0F, 5.0F, 2.0F, 5.0F, -1.0F, -0.2F, -1.0F);
        leftRocket.texOffs(0, 33).addBox(5.5F, 1.1F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        leftRocket.texOffs(0, 35).addBox(5.5F, 9.5F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        rightRocket = new ModelRenderer(this, 20, 20);
        rightRocket.setPos(-4.0F, 0.0F, -2.0F);
        rightRocket.addBox(0.5F, 0.8F, 3.0F, 3.0F, 10.0F, 3.0F, -0.5F, -1.2F, -0.5F);
        rightRocket.texOffs(20, 33).addBox(-0.5F, 2.6F, 2.0F, 5.0F, 2.0F, 5.0F, -1.0F, -0.2F, -1.0F);
        rightRocket.texOffs(20, 33).addBox(1.5F, 1.1F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        rightRocket.texOffs(20, 35).addBox(1.5F, 9.5F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        
        body.addChild(leftRocket);
        body.addChild(rightRocket);
        
        body.visible = true;
        
        head.visible = false;
        leftArm.visible = false;
        rightArm.visible = false;
        leftLeg.visible = false;
        rightLeg.visible = false;
        hat.visible = false;
    }

    /*@Override
    public void render(MatrixStack mStack, IVertexBuilder buf, int packedLight, int packedOverlay,
                       float red, float green, float blue, float alpha) {
        super.render(mStack, buf, packedLight, packedOverlay, red, green, blue, alpha);
    }*/
    
    /**
     * Only the bipedBody part can be visible
     */
    @Override public void setAllVisible(boolean visible) {
        this.body.visible = visible;
    }
    
    /**
     * This is a helper function from Tabula to set the rotation of model parts
     */
    public void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }
    
    @Override
    public void setupAnim(
      @NotNull T entity, float limbSwing, float limbSwingAmount,
      float ageInTicks, float netHeadYaw, float headPitch
    ) {
        super.setupAnim(
          entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity instanceof ArmorStandEntity) {
            final Rotations rot = ((ArmorStandEntity) entity).getBodyPose();
            setRotateAngle(body, rot.getX() * TO_RAD, rot.getY() * TO_RAD, rot.getZ() * TO_RAD);
        }
    }
}
