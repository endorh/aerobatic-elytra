package endorh.aerobaticelytra.client.render.model;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
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

import static endorh.aerobaticelytra.client.render.model.AerobaticElytraModelPose.ModelRotation.TO_RAD;

/**
 * AcrobaticElytraChestModel - EndorH<br>
 * Designed with the help of Tabula 8.0.0<br>
 * Adds a pair of rockets to the back of the player<br>
 * Intended to be combined with {@link AerobaticElytraModel}
 */
@EventBusSubscriber(value=Dist.CLIENT, bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
@OnlyIn(Dist.CLIENT)
public class AerobaticElytraChestModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation AEROBATIC_ELYTRA_CHEST_LAYER =
      new ModelLayerLocation(new ResourceLocation("player"), "aerobatic_elytra_chest");
    private static final String BODY = "body";
    public static final String LEFT_ROCKET = "left_rocket";
    public static final String RIGHT_ROCKET = "right_rocket";
    
    public final ModelPart leftRocket;
    public final ModelPart rightRocket;
    
    @SubscribeEvent
    public static void registerLayerDefinition(RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AEROBATIC_ELYTRA_CHEST_LAYER, AerobaticElytraChestModel::createLayer);
    }
    
    /**
     * Model definition
     */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        
        // Add empty body
        PartDefinition body = root.addOrReplaceChild(BODY, CubeListBuilder.create(), PartPose.ZERO);
        
        // Add rockets
        body.addOrReplaceChild(LEFT_ROCKET, CubeListBuilder.create()
            .texOffs(0, 20).addBox(4.5F, 0.8F, 3.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(-0.5F, -1.2F, -0.5F))
            .texOffs(0, 33).addBox(3.5F, 2.6F, 2.0F, 5.0F, 2.0F, 5.0F, new CubeDeformation(-1.0F, -0.2F, -1.0F))
            .texOffs(0, 33).addBox(5.5F, 1.1F, 4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F, -0.1F, -0.1F))
            .texOffs(0, 35).addBox(5.5F, 9.5F, 4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F, -0.1F, -0.1F)),
          PartPose.offset(-4.0F, 0.0F, -2.0F));
        body.addOrReplaceChild(RIGHT_ROCKET, CubeListBuilder.create()
            .texOffs(20, 20).addBox(0.5F, 0.8F, 3.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(-0.5F, -1.2F, -0.5F))
            .texOffs(20, 33).addBox(-0.5F, 2.6F, 2.0F, 5.0F, 2.0F, 5.0F, new CubeDeformation(-1.0F, -0.2F, -1.0F))
            .texOffs(20, 33).addBox(1.5F, 1.1F, 4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F, -0.1F, -0.1F))
            .texOffs(20, 35).addBox(1.5F, 9.5F, 4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F, -0.1F, -0.1F)),
          PartPose.offset(-4.0F, 0.0F, -2.0F));
        
        // Add empty parent parts
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        
        return LayerDefinition.create(mesh, 128, 64);
    }
    
    public AerobaticElytraChestModel(ModelPart modelPart) {
        super(modelPart);
        leftRocket = body.getChild(LEFT_ROCKET);
        rightRocket = body.getChild(RIGHT_ROCKET);
        initVisibility();
    }
    
    protected void initVisibility() {
        body.visible = true;
        
        head.visible = false;
        leftArm.visible = false;
        rightArm.visible = false;
        leftLeg.visible = false;
        rightLeg.visible = false;
        hat.visible = false;
    }
    
    
    /**
     * Only the bipedBody part can be visible
     */
    @Override public void setAllVisible(boolean visible) {
        this.body.visible = visible;
    }
    
    @Override public void setupAnim(
      @NotNull T entity, float limbSwing, float limbSwingAmount,
      float ageInTicks, float netHeadYaw, float headPitch
    ) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity instanceof ArmorStand) {
            final Rotations rot = ((ArmorStand) entity).getBodyPose();
            body.setRotation(rot.getX() * TO_RAD, rot.getY() * TO_RAD, rot.getZ() * TO_RAD);
        }
    }
}
