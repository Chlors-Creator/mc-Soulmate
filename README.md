# MC Soulmate

Minecraft Java Edition 26.2 NeoForge mod prototype targeting NeoForge 26.2.0.84 and newer.

## Build

Use JDK 25. The official 26.2 ModDevGradle template is configured to resolve the Java toolchain through Foojay.

On Windows, run:

    .\build-package.ps1

The script uses the workspace's `.toolchain\jdk-25.0.1` and keeps the Gradle distribution and dependency cache under `.gradle-user-home`. It disables Gradle's configuration cache because the NeoForge artifact generation task cannot be cached with this toolchain. It builds against NeoForge 26.2.0.84; the mod metadata requires NeoForge 26.2.0.84 or newer. NeoForge releases and Mojang metadata use the official Forge CDN Maven endpoints as mirrors. The first build needs network access to resolve dependencies. The mod jar is written to `build\libs\Soulmate-v0.2.0.jar` and can be copied into a NeoForge 26.2 `mods` folder.

## Spawn and equipment

Each player receives one starter chest beside the world spawn. It contains a Soulmate spawn egg, one diamond, and one name tag. The chest is placed in an empty adjacent space with solid ground; if none is available, the mod logs a warning and tries again on that player's next login. Once placed, it is not generated again for that player.

The Soulmate has no biome spawn entries. Use the spawn egg or summon it with:

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

## Work hat

While the Soulmate is fishing, harvesting, or planting crops, it automatically wears a straw hat rendered by the mod (`mcsoulmate:textures/entity/straw_hat.png`). A helmet in its `armor.head` slot keeps its armor value and enchantments but is hidden from rendering while the hat is on. Each hat toggle has a 1-second cooldown, so quick task changes do not cause flickering.

## API configuration

Open the Minecraft **Options** screen from the title menu or pause menu and select **API Configuration**. The screen stores the provider name, API URL, API key, and model in the client file `config/mcsoulmate-api.properties`. The API key is masked in the screen. This entry currently saves local connection settings; the mod does not yet send API requests.

## Fishing

Equip a fishing rod in the main hand. While safe and near fishable water, the Soulmate walks to the shore, faces the water, casts, waits for a bite, and reels in using vanilla fishing behavior. The fishing pose shows the held rod, bobber, and line. Fishing loot is saved separately from farm produce and seeds. After it has collected 10 fish items, it pauses fishing and deposits its fishing inventory into a nearby chest within 20 blocks, stacking matching items first; fishing resumes when fewer than 10 fish remain in its inventory.
