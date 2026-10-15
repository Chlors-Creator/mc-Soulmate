package com.mcsoulmate.client;

import com.mcsoulmate.McSoulmate;
import com.mcsoulmate.entity.SoulmateEntity;
import net.minecraft.client.model.ArmorModelSet;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

public final class SoulmateRenderer extends HumanoidMobRenderer<SoulmateEntity, AvatarRenderState, PlayerModel> {
    private static final float SHADOW_RADIUS = 0.5F;
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(McSoulmate.MOD_ID, "textures/entity/soulmate.png");

    public SoulmateRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), SHADOW_RADIUS);
        ArmorModelSet<HumanoidModel<AvatarRenderState>> armorModels =
                ArmorModelSet.bake(ModelLayers.PLAYER_SLIM_ARMOR, context.getModelSet(), HumanoidModel::new);
        this.addLayer(new HumanoidArmorLayer<>(this, armorModels, context.getEquipmentRenderer()));
        this.addLayer(new ItemInHandLayer<>(this));
        this.addLayer(new FishingLineLayer(this));
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(SoulmateEntity entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.setRenderData(FishingLineLayer.HOOK_POSITION, findFishingHookPosition(entity, partialTick));
        setFishingArmPose(entity, state);
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return TEXTURE;
    }

    private static Vec3 findFishingHookPosition(SoulmateEntity entity, float partialTick) {
        int hookId = entity.getFishingHookId();
        if (!entity.hasFishingHook()) {
            return null;
        }
        if (!(entity.level().getEntity(hookId) instanceof FishingHook hook)) {
            return null;
        }
        return new Vec3(hook.getX(partialTick), hook.getY(partialTick), hook.getZ(partialTick));
    }

    private static void setFishingArmPose(SoulmateEntity entity, AvatarRenderState state) {
        if (!entity.hasFishingHook()) {
            return;
        }
        if (entity.getMainArm() == HumanoidArm.RIGHT) {
            state.rightArmPose = HumanoidModel.ArmPose.ITEM;
            return;
        }
        state.leftArmPose = HumanoidModel.ArmPose.ITEM;
    }
}
