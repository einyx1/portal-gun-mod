package com.jhonfx.portalgun.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Small UV sphere mesh; no entity-per-facet, particles or external texture required. */
final class EnergyMesh {
    static void sphere(PoseStack pose, VertexConsumer vertices, float radius,
                       float red, float green, float blue, float alpha) {
        for (int latitude = 0; latitude < 12; latitude++) {
            double a = Math.PI * latitude / 12, b = Math.PI * (latitude + 1) / 12;
            for (int longitude = 0; longitude < 20; longitude++) {
                double c = Math.PI * 2 * longitude / 20, d = Math.PI * 2 * (longitude + 1) / 20;
                vertex(pose, vertices, radius, a, c, red, green, blue, alpha);
                vertex(pose, vertices, radius, b, c, red, green, blue, alpha);
                vertex(pose, vertices, radius, b, d, red, green, blue, alpha);
                vertex(pose, vertices, radius, a, d, red, green, blue, alpha);
            }
        }
    }
    private static void vertex(PoseStack pose, VertexConsumer vertices, float radius,
                               double lat, double lon, float r, float g, float b, float alpha) {
        float shade = (float) (0.6 + 0.4 * Math.cos(lat * 0.5));
        vertices.vertex(pose.last().pose(), (float) (radius * Math.sin(lat) * Math.cos(lon)),
                (float) (radius * Math.cos(lat)), (float) (radius * Math.sin(lat) * Math.sin(lon)))
                .color(r * shade, g * shade, b * shade, alpha).endVertex();
    }
    private EnergyMesh() {}
}
