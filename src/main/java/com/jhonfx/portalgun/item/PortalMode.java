package com.jhonfx.portalgun.item;

public enum PortalMode {
    FIFO("FIFO"),
    LIFO("LIFO"),
    MULTI_PAIR("Multi-Pair"),
    ROOT("Raiz"),
    ONE_SHOT("One Shot aleatorio"),
    CUSTOM("Coordenadas"),
    STRUCTURE("Procurar estrutura");

    private final String displayName;

    PortalMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public PortalMode next() {
        PortalMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
