package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.item.PortalGunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PortalGunRenderer extends GeoItemRenderer<PortalGunItem> {
    public PortalGunRenderer() {
        super(new PortalGunModel());
    }

    @Override
    public ResourceLocation getTextureLocation(PortalGunItem gun) {
        ItemStack stack = getCurrentItemStack();
        boolean loaded = stack != null && !stack.isEmpty() && PortalGunItem.getLoadedColor(stack) != null;
        String texture = loaded ? gun.variant().texture() : gun.variant().dischargedTexture();
        return new ResourceLocation(PortalGunMod.MOD_ID, "textures/item/" + texture + ".png");
    }

    @Override
    public void preRender(PoseStack poseStack, PortalGunItem gun, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        ItemStack stack = getCurrentItemStack();
        boolean loaded = stack != null && !stack.isEmpty() && PortalGunItem.getLoadedColor(stack) != null;
        for (String boneName : new String[]{"tube", "tube_outline", "tube_outline_bg", "portal_fluid"}) {
            model.getBone(boneName).ifPresent(bone -> {
                bone.setHidden(!loaded);
                bone.setChildrenHidden(!loaded);
            });
        }

        // GeckoLib establishes the vanilla item anchor here (including its
        // internal half-block translation). It must happen before our
        // context-specific rotations, scales and geometric centering;
        // otherwise that anchor itself is rotated/scaled and the gun appears
        // detached from the hand or camera.
        super.preRender(poseStack, gun, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);

        ItemDisplayContext context = this.renderPerspective;
        switch (context) {
            case FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.48, 0.48, -0.55);
                poseStack.mulPose(Axis.YP.rotationDegrees(25));
                poseStack.mulPose(Axis.ZP.rotationDegrees(12));
                poseStack.scale(1.15f, 1.15f, 1.15f);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(-0.48, 0.48, -0.55);
                poseStack.mulPose(Axis.YP.rotationDegrees(335));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-12));
                poseStack.scale(1.15f, 1.15f, 1.15f);
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.05, 0.18, 0.02);
                poseStack.mulPose(Axis.XP.rotationDegrees(-78));
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.scale(0.72f, 0.72f, 0.72f);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(-0.05, 0.18, 0.02);
                poseStack.mulPose(Axis.XP.rotationDegrees(-78));
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.scale(0.72f, 0.72f, 0.72f);
            }
            case GUI -> {
                poseStack.translate(0.0, -0.08, 0.0);
                poseStack.mulPose(Axis.XP.rotationDegrees(-18));
                poseStack.mulPose(Axis.YP.rotationDegrees(32));
                poseStack.scale(0.82f, 0.82f, 0.82f);
            }
            case GROUND -> {
                // The standard and Prime source files are authored upright.
                // Item entities therefore need to be laid on their side and
                // reduced independently from the hand/item-frame transforms.
                if (gun.variant() == com.jhonfx.portalgun.item.PortalGunVariant.STANDARD
                        || gun.variant() == com.jhonfx.portalgun.item.PortalGunVariant.PRIME) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    poseStack.scale(0.24f, 0.24f, 0.24f);
                } else {
                    poseStack.scale(0.62f, 0.62f, 0.62f);
                }
            }
            case FIXED -> {
                // Item frames use their exact centre as the anchor. Keep this
                // context free from hand/camera offsets.
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.scale(0.70f, 0.70f, 0.70f);
            }
            default -> { }
        }
        // Normalize every source model around its real geometric centre. This is
        // what keeps the same anchor in GUI, item frames and both player cameras.
        float normalizedScale = normalizedScale(gun);
        poseStack.scale(normalizedScale, normalizedScale, normalizedScale);
        poseStack.translate(-centerX(gun) / 16.0, -centerY(gun) / 16.0, -centerZ(gun) / 16.0);
    }

    private float normalizedScale(PortalGunItem gun) {
        return switch (gun.variant()) {
            case STANDARD -> 1.0f;
            case PROTOTYPE, BLUE_PROTOTYPE -> 0.507f;
            case EVIL_MORTY -> 0.583f;
            case PRIME -> 1.052f;
        };
    }

    private double centerX(PortalGunItem gun) {
        return switch (gun.variant()) {
            case STANDARD -> -0.426;
            case PROTOTYPE, BLUE_PROTOTYPE -> -0.30376;
            case EVIL_MORTY, PRIME -> 0.0;
        };
    }

    private double centerY(PortalGunItem gun) {
        return switch (gun.variant()) {
            case STANDARD -> 5.46873;
            case PROTOTYPE, BLUE_PROTOTYPE -> 10.69089;
            case EVIL_MORTY -> 10.6125;
            case PRIME -> 5.065855;
        };
    }

    private double centerZ(PortalGunItem gun) {
        return switch (gun.variant()) {
            case STANDARD -> 0.01683;
            case PROTOTYPE, BLUE_PROTOTYPE -> -0.74969;
            case EVIL_MORTY -> -5.4625;
            case PRIME -> -2.91375;
        };
    }

}
