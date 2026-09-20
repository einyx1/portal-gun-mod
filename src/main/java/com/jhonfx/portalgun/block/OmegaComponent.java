package com.jhonfx.portalgun.block;

public enum OmegaComponent {
    CORE("core"),
    EMITTER("emitter"),
    RING("ring");

    private final String id;

    OmegaComponent(String id) { this.id = id; }
    public String id() { return id; }
}
