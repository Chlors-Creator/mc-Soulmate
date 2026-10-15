# MC Soulmate

Minecraft Java Edition 26.2 NeoForge mod prototype.

## Build

Use JDK 25. The official 26.2 ModDevGradle template is configured to resolve the Java toolchain through Foojay.

On Windows, run:

    .\gradlew.bat jar

The built mod is written to build\libs.

## Spawn and equipment

The entity has no biome spawn entries or spawn egg. Summon it with:

    /summon mcsoulmate:soulmate ~ ~ ~

The prototype targets hostile monsters and does not select players as targets. Its main-hand item selects the combat mode: a bow uses ranged attacks; other items or an empty hand use melee.

Equip a bow:

    /item replace entity @e[type=mcsoulmate:soulmate,limit=1,sort=nearest] weapon.mainhand with minecraft:bow

Equip a sword:

    /item replace entity @e[type=mcsoulmate:soulmate,limit=1,sort=nearest] weapon.mainhand with minecraft:iron_sword

Equip armor through armor.head, armor.chest, armor.legs, and armor.feet entity slots.

The entity uses Minecraft's Alex/slim PlayerModel and its vanilla player animation model. The fixed skin texture is assets/mcsoulmate/textures/entity/soulmate.png. Ground movement starts at a 0.13 movement-speed attribute, the normal player sprint baseline; navigation still needs in-game calibration for exact distance-per-second parity.