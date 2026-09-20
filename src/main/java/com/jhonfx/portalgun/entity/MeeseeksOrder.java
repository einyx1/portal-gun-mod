package com.jhonfx.portalgun.entity;

/**
 * All available Meeseeks orders.
 *
 * goal() = 0 means "indefinite / until dismissed".
 */
public enum MeeseeksOrder {
    FOLLOW        ("Siga-me",                  0),
    PROTECT       ("Proteja-me",               0),
    KILL          ("Mate o alvo",              1),
    COLLECT       ("Pegue itens (32)",        32),
    COLLECT_ALL   ("Pegue TUDO ao redor",      0),   // indefinite collect
    GUIDE         ("Leve-me a lugar seguro",   1),
    MINE          ("Minere blocos (24)",       24),
    MINE_ORE      ("Minere apenas minérios",   16),  // ore-targeted mining
    MINE_LOGS     ("Colete madeira",            24),
    BUILD         ("Construa um abrigo",       96),
    BUILD_WALL    ("Construa uma parede",      28),
    BUILD_BRIDGE  ("Construa uma ponte",       63),
    BUILD_TOWER   ("Construa uma torre",      226),
    PORTAL_HELP   ("Ajude no portal",          1),
    TRAINING      ("Treinamento (5 rounds)",   5),
    FARM          ("Colha plantações",         32),  // right-click farmland crops
    PATROL        ("Patrulhe a área",          0),   // circles a 12-block radius
    CARRY         ("Carregue meu inventário",  1);   // drops inv items near player on death

    private final String label;
    private final int goal;

    MeeseeksOrder(String label, int goal) {
        this.label = label;
        this.goal  = goal;
    }

    public String label() { return label; }
    public int    goal()  { return goal;  }

    /** True for orders that involve block modification — gated by config. */
    public boolean requiresGriefing() {
        return this == MINE || this == MINE_ORE || this == MINE_LOGS || this == BUILD || this == BUILD_WALL
                || this == BUILD_BRIDGE || this == BUILD_TOWER || this == FARM;
    }
}
