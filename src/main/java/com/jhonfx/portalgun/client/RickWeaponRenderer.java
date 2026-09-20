package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.item.RickWeaponItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class RickWeaponRenderer extends GeoItemRenderer<RickWeaponItem>{
    public RickWeaponRenderer(){super(new RickWeaponModel());}

    @Override
    public void preRender(PoseStack pose, RickWeaponItem weapon, BakedGeoModel model,
                          MultiBufferSource buffers, VertexConsumer vertices, boolean reRender,
                          float partialTick, int light, int overlay,
                          float red, float green, float blue, float alpha) {
        // Let GeckoLib establish the vanilla item anchor before changing it.
        // These models are authored in Blockbench pixel units (roughly 21-25
        // pixels long), so using them without this normalization makes them
        // look like large diagonal shields in the player's hand.
        super.preRender(pose, weapon, model, buffers, vertices, reRender, partialTick,
                light, overlay, red, green, blue, alpha);

        ItemDisplayContext context = this.renderPerspective;
        switch (context) {
            case FIRST_PERSON_RIGHT_HAND -> {
                pose.translate(0.36, 0.28, -0.42);
                pose.mulPose(Axis.YP.rotationDegrees(18));
                pose.mulPose(Axis.ZP.rotationDegrees(-5));
                pose.scale(0.54f, 0.54f, 0.54f);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                pose.translate(-0.36, 0.28, -0.42);
                pose.mulPose(Axis.YP.rotationDegrees(-18));
                pose.mulPose(Axis.ZP.rotationDegrees(5));
                pose.scale(0.54f, 0.54f, 0.54f);
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                pose.translate(0.02, 0.12, 0.0);
                pose.mulPose(Axis.XP.rotationDegrees(-78));
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.mulPose(Axis.ZP.rotationDegrees(4));
                pose.scale(0.38f, 0.38f, 0.38f);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                pose.translate(-0.02, 0.12, 0.0);
                pose.mulPose(Axis.XP.rotationDegrees(-78));
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.mulPose(Axis.ZP.rotationDegrees(-4));
                pose.scale(0.38f, 0.38f, 0.38f);
            }
            case GUI -> {
                pose.translate(0.0, -0.02, 0.0);
                pose.mulPose(Axis.XP.rotationDegrees(-12));
                pose.mulPose(Axis.YP.rotationDegrees(28));
                pose.scale(0.68f, 0.68f, 0.68f);
            }
            case GROUND -> {
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.scale(0.34f, 0.34f, 0.34f);
            }
            case FIXED -> {
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.scale(0.48f, 0.48f, 0.48f);
            }
            default -> pose.scale(0.48f, 0.48f, 0.48f);
        }

        pose.translate(-centerX(weapon) / 16.0, -centerY(weapon) / 16.0, -centerZ(weapon) / 16.0);
    }

    private static double centerX(RickWeaponItem weapon) {
        return switch (weapon.assetId()) {
            case "freeze_ray" -> 4.5;
            case "mindblower_gun" -> 3.5;
            case "plasma_pistol" -> 2.5;
            default -> 3.0;
        };
    }

    private static double centerY(RickWeaponItem weapon) {
        return switch (weapon.assetId()) {
            case "freeze_ray" -> 7.5;
            case "mindblower_gun" -> 8.0;
            case "plasma_pistol" -> 6.5;
            default -> 6.25;
        };
    }

    private static double centerZ(RickWeaponItem weapon) {
        return switch (weapon.assetId()) {
            case "plasma_pistol" -> -0.2;
            case "laser_gun" -> -0.1;
            default -> 0.0;
        };
    }
}
