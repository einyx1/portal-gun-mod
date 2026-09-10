# Portal Gun — Forge Mod

> Java port do addon **Portal Gun (Bedrock)** para Minecraft Forge.

**Versão atual:** `0.12.68-preview.33`  
**Autor:** s4zm (dono original: JhonFX)  
**Licença:** MIT

---

## Requisitos

| Dependência | Versão mínima | Obrigatória |
|---|---|---|
| Minecraft | 1.20.1 – 1.20.x | ✅ |
| Forge | 47+ | ✅ |
| GeckoLib | 4.8+ | ✅ |
| GeckoMesh | 1.1+ | ✅ |
| Cutscene API | 1.6.5 – 1.6.x | ✅ |
| Ad Astra | 1.15.19+ | ⬜ (compat opcional) |

---

## Estrutura do projeto

```
portalgun/
├── src/
│   └── main/
│       ├── java/com/jhonfx/portalgun/
│       │   ├── PortalGunMod.java          ← entrada do mod
│       │   ├── ModConfig.java
│       │   ├── block/
│       │   ├── block/entity/
│       │   ├── citadel/
│       │   ├── client/
│       │   ├── command/
│       │   ├── compat/
│       │   ├── curve/
│       │   ├── entity/
│       │   ├── event/
│       │   ├── federation/
│       │   ├── init/
│       │   ├── item/
│       │   ├── network/
│       │   ├── omega/
│       │   └── prologue/
│       └── resources/
│           ├── META-INF/mods.toml
│           ├── pack.mcmeta
│           ├── portalgun_logo.png
│           ├── assets/portalgun/
│           │   ├── animations/
│           │   ├── blockstates/
│           │   ├── geo/
│           │   ├── lang/
│           │   ├── mesh/
│           │   ├── models/
│           │   ├── sounds/
│           │   └── textures/
│           └── data/portalgun/
│               ├── cutscenes/
│               ├── dimension/
│               ├── dimension_type/
│               ├── loot_tables/
│               └── recipes/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── README.md
```

---

## Como compilar

```bash
# Clone o repositório
git clone https://github.com/<seu-usuario>/portalgun-forge.git
cd portalgun-forge

# Gere os sources do Minecraft/Forge (primeira vez)
./gradlew genSources

# Compile e gere o JAR
./gradlew build
```

O JAR compilado fica em `build/libs/`.

---

## Conteúdo do mod

### Itens principais
- Portal Gun (variantes: Standard, Evil Morty, Prime, Prototype)
- Rick Weapon / Freeze Ray / Laser Gun / Plasma Pistol / Mindblower Gun
- Meeseeks Box, Rick Ship, Combat Tech, Neural Scanner, Portal Cannon
- Purge Suit com habilidades especiais

### Blocos
- Omega Device, Omega Core, Omega Emitter, Omega Ring
- Citadel (alloy, circuit, glass, gold, panel, shop, workshop, terminal, mission terminal)
- Federation (energy, floor, glass, hull, terminal)
- Portal Solution, Curve Core, Dimensional Drive

### Entidades
- Meeseeks, Evil Morty, Rick Prime, Diane Robot, Rick Ship
- Citadel NPC (Rick, Morty, Guard, Council)
- Federation Alien, Federation Drone, Federation Heavy Trooper
- Portal Entity, Curve Node, Captive Rick

### Sistemas
- Citadel of Ricks com economy, missões e geração de estrutura
- Omega Machine com crafting especial
- Curve system para portais
- Federation threat/prison system
- Prologue / cutscene system (via Cutscene API)
- Integração com Ad Astra (espaço)

---

## Contribuindo

Pull requests são bem-vindos. Para mudanças grandes, abra uma issue primeiro para discutir o que você quer mudar.
