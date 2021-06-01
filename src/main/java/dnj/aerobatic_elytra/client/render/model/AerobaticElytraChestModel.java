package dnj.aerobatic_elytra.client.render.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * AcrobaticElytraChestModel - Endor8
 * <br>
 * Created using Tabula 8.0.0
 * <br>
 * Adds a pair of rockets to the back of the player
 */
@OnlyIn(Dist.CLIENT)
public class AerobaticElytraChestModel<T extends LivingEntity> extends BipedModel<T> {
    public final ModelRenderer leftRocket;
    public final ModelRenderer rightRocket;
    
    /**
     * Build model
     */
    public
    AerobaticElytraChestModel() {
        super(RenderType::getEntityCutoutNoCull, 1F, 0F, 128, 128);
        textureWidth = 128;
        textureHeight = 128;
        
        leftRocket = new ModelRenderer(this, 64, 0);
        leftRocket.setRotationPoint(-4.0F, 0.0F, -2.0F);
        leftRocket.addBox(4.5F, 0.8F, 3.0F, 3.0F, 10.0F, 3.0F, -0.5F, -1.2F, -0.5F);
        leftRocket.setTextureOffset(80, 0).addBox(3.5F, 2.6F, 2.0F, 5.0F, 2.0F, 5.0F, -1.0F, -0.2F, -1.0F);
        leftRocket.setTextureOffset(80, 8).addBox(5.5F, 1.1F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        leftRocket.setTextureOffset(88, 8).addBox(5.5F, 9.5F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        rightRocket = new ModelRenderer(this, 64, 0);
        rightRocket.setRotationPoint(-4.0F, 0.0F, -2.0F);
        rightRocket.addBox(0.5F, 0.8F, 3.0F, 3.0F, 10.0F, 3.0F, -0.5F, -1.2F, -0.5F);
        rightRocket.setTextureOffset(80, 0).addBox(-0.5F, 2.6F, 2.0F, 5.0F, 2.0F, 5.0F, -1.0F, -0.2F, -1.0F);
        rightRocket.setTextureOffset(80, 8).addBox(1.5F, 1.1F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        rightRocket.setTextureOffset(88, 8).addBox(1.5F, 9.5F, 4.0F, 1.0F, 1.0F, 1.0F, -0.1F, -0.1F, -0.1F);
        
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
}
