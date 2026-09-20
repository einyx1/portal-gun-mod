package com.jhonfx.portalgun.entity;

/**
 * Port das variantes ram_pg:blue_portal / ram_pg:orange_portal (o addon original
 * também tem green/yellow para o modo CUSTOM — aqui simplificado ao par
 * clássico azul/laranja, como no jogo Portal).
 */
public enum PortalColor {
    BLUE(0x168BFF),
    GREEN(0x28FF63),
    YELLOW(0xFFD21A);

    private final int rgb;

    PortalColor(int rgb) {
        this.rgb = rgb;
    }

    public int rgb() {
        return rgb;
    }
}
