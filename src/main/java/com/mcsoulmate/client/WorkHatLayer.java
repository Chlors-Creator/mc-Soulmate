package com.mcsoulmate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mcsoulmate.McSoulmate;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.context.ContextKey;

public final class WorkHatLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(McSoulmate.MOD_ID, "work_hat"),
            "main"
    );
    public static final ContextKey<Boolean> WEARING_HAT = new ContextKey<>(
            Identifier.fromNamespaceAndPath(McSoulmate.MOD_ID, "wearing_work_hat")
    );
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(McSoulmate.MOD_ID, "textures/entity/straw_hat.png");
    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 64;
    private static final float BRIM_MIN = -6.0F;
    private static final float BRIM_SIZE = 12.0F;
    private static final float BRIM_BOTTOM = -9.0F;
    private static final float BRIM_HEIGHT = 1.0F;
    private static final float CROWN_MIN = -4.0F;
    private static final float CROWN_SIZE = 8.0F;
    private static final float CROWN_BOTTOM = -12.0F;
    private static final float CROWN_HEIGHT = 3.0F;

    private final Model<Unit> model;

    public WorkHatLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, ModelPart hatModelPart) {
        super(renderer);
        this.model = new Model.Simple(hatModelPart, texture -> RenderTypes.entitySolid(TEXTURE));
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "brim",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(BRIM_MIN, BRIM_BOTTOM, BRIM_MIN, BRIM_SIZE, BRIM_HEIGHT, BRIM_SIZE),
                PartPose.ZERO
        );
        root.addOrReplaceChild(
                "crown",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(CROWN_MIN, CROWN_BOTTOM, CROWN_MIN, CROWN_SIZE, CROWN_HEIGHT, CROWN_SIZE),
                PartPose.ZERO
        );
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            AvatarRenderState state,
            float yRot,
            float xRot
    ) {
        if (!Boolean.TRUE.equals(state.getRenderData(WEARING_HAT)) || state.isInvisible) {
            return;
        }

        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);
        submitNodeCollector.submitModel(
                this.model,
                Unit.INSTANCE,
                poseStack,
                RenderTypes.entitySolid(TEXTURE),
                lightCoords,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                state.outlineColor,
                null
        );
        poseStack.popPose();
    }
}
