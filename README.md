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

Use one diamond to right-click an untamed Soulmate and sign a permanent contract. The first contract awards the advancement **签订契约** (“一颗永流传”). Each Soulmate has one owner; it follows that player and protects them by attacking their attacker or the target they attack. It does not automatically hunt monsters or protect other players.

The main-hand item selects the combat mode: a bow uses ranged attacks; other items or an empty hand use melee.

Equip a bow:

    /item replace entity @e[type=mcsoulmate:soulmate,limit=1,sort=nearest] weapon.mainhand with minecraft:bow

Equip a sword:

    /item replace entity @e[type=mcsoulmate:soulmate,limit=1,sort=nearest] weapon.mainhand with minecraft:iron_sword

Equip armor through armor.head, armor.chest, armor.legs, and armor.feet entity slots.

The entity uses Minecraft's Alex/slim PlayerModel and its vanilla player animation model. The fixed skin texture is assets/mcsoulmate/textures/entity/soulmate.png. Ground movement starts at a 0.13 movement-speed attribute, the normal player sprint baseline; navigation still needs in-game calibration for exact distance-per-second parity.

## Farming and storage

When it has no combat target, a Soulmate searches nearby farmland for mature vanilla or modded `CropBlock` crops to harvest and replant, and empty farmland to plant using seeds in its farming inventory. Its farming inventory is saved with the entity.

After signing a contract, right-click the Soulmate with a crop seed (or a plantable crop such as carrots or potatoes) to add one item to its farming inventory. In survival mode the item is consumed.

If a regular or trapped chest with room is within 20 blocks, the Soulmate walks to it and deposits the farming inventory. It merges matching item stacks already in the chest first, then fills available slots in order. If no chest has room, it keeps the harvest in its saved inventory.

## Fishing

Equip a fishing rod in the main hand. While safe and near fishable water, the Soulmate walks to the shore, faces the water, casts, waits for a bite, and reels in using vanilla fishing behavior. The fishing pose shows the held rod, bobber, and line. Fishing loot is saved separately from farm produce and seeds. After it has collected 10 fish items, it pauses fishing and deposits its fishing inventory into a nearby chest within 20 blocks, stacking matching items first; fishing resumes when fewer than 10 fish remain in its inventory.
