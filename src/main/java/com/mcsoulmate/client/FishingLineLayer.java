package com.mcsoulmate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mcsoulmate.McSoulmate;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class FishingLineLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float LINE_SAG = 0.25F;
    private static final float LINE_WIDTH = 1.0F;
    private static final float ROD_HAND_OFFSET_Y = 0.125F;
    private static final float ROD_TIP_OFFSET_Z = -0.75F;
    private static final float SAG_CURVE_FACTOR = 4.0F;
    private static final int LINE_SEGMENTS = 16;
    private static final int LINE_COLOR = 0xFFFFFFFF;
    public static final ContextKey<Vec3> HOOK_POSITION = new ContextKey<>(
            Identifier.fromNamespaceAndPath(McSoulmate.MOD_ID, "fishing_hook_position")
    );

    public FishingLineLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
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
        Vec3 hookPosition = state.getRenderData(HOOK_POSITION);
        if (hookPosition == null) {
            return;
        }
        Matrix4f inverseEntityPose = new Matrix4f(poseStack.last().pose()).invert();
        Vector3f lineStart = this.findRodTip(poseStack, inverseEntityPose);
        Vector3f lineEnd = this.toEntityLocal(hookPosition, state, inverseEntityPose);
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                RenderTypes.lines(),
                (pose, buffer) -> drawFishingLine(pose, buffer, lineStart, lineEnd)
        );
    }

    private Vector3f findRodTip(PoseStack poseStack, Matrix4f inverseEntityPose) {
        poseStack.pushPose();
        this.getParentModel().rightArm.translateAndRotate(poseStack);
        poseStack.translate(0.0F, ROD_HAND_OFFSET_Y, ROD_TIP_OFFSET_Z);
        Vector3f rodTipInCameraSpace = poseStack.last().pose().transformPosition(new Vector3f());
        poseStack.popPose();
        return inverseEntityPose.transformPosition(rodTipInCameraSpace);
    }

    private static Vector3f toEntityLocal(Vec3 hookPosition, AvatarRenderState state, Matrix4f inverseEntityPose) {
        Vector3f offset = new Vector3f(
                (float) (hookPosition.x - state.x),
                (float) (hookPosition.y - state.y),
                (float) (hookPosition.z - state.z)
        );
        return inverseEntityPose.transformDirection(offset);
    }

    private static void drawFishingLine(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            Vector3f start,
            Vector3f end
    ) {
        VertexConsumer line = buffer.setColor(LINE_COLOR).setLineWidth(LINE_WIDTH);
        Vector3f previous = start;
        for (int segment = 1; segment <= LINE_SEGMENTS; segment++) {
            float progress = (float) segment / LINE_SEGMENTS;
            Vector3f next = pointOnLine(start, end, progress);
            addLineVertex(line, pose, previous);
            addLineVertex(line, pose, next);
            previous = next;
        }
    }

    private static Vector3f pointOnLine(Vector3f start, Vector3f end, float progress) {
        float sag = LINE_SAG * SAG_CURVE_FACTOR * progress * (1.0F - progress);
        return new Vector3f(
                start.x + (end.x - start.x) * progress,
                start.y + (end.y - start.y) * progress + sag,
                start.z + (end.z - start.z) * progress
        );
    }

    private static void addLineVertex(VertexConsumer line, PoseStack.Pose pose, Vector3f position) {
        line.addVertex(pose, position).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
