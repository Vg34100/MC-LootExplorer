# LootExplorer development integrations

These are optional test resources for local integrated-server loot discovery.
They are development runtime dependencies, never LootExplorer requirements,
compile APIs, embedded JARs, or packaged-release smoke dependencies.

## What existed before Stonecutter

Commit `837bad8` added registry-based compatibility for Better Archeology,
ExtraChests, and Fabric Iron Chests. The old Gradle builds declared JEI and
Fabric Mod Menu, but did not declare those three mods or their datapacks.
Instead, the ignored working directories contained:

- `fabric/run/mods/`: Better Archeology 1.3.7, ExtraChests 1.0.2,
  Iron Chests 2.0.6, and Resourceful Config 4.0.1.
- `neoforge/run/mods/`: Better Archeology 1.3.7, ExtraChests 1.0.2,
  and Resourceful Config 4.0.1. Resourceful Config is Better Archeology's library.
- `fabric/run/saves/dev (1)/datapacks/` and
  `neoforge/run/saves/dev/datapacks/`: Dungeons and Taverns v5.2.0.
- `fabric/run/saves/dev-2/datapacks/`: Fluffy's Enchantments 6.08.

The old logs show LootExplorer processing `nova_structures:chests/...` from
Dungeons and Taverns. That is the strongest match for the remembered extra
loot source. Fluffy's `fluffytg:loot/...` tables also occur in older logs.
Its historical ZIP matches the author's published 6.08 checksum exactly;
that release targets 1.21.4, and the old 1.21.5 logs include datapack errors.
It must not be reused for modern targets.

All original files and worlds remain untouched. Stonecutter clients use
`runs/<minecraft>-<loader>/client/`, so they stopped seeing these ignored
mods/world-local datapacks. JEI and Mod Menu remained configured through Gradle.

## Compatible target coverage

The authoritative version IDs are the `*_dev_version` properties in
`gradle/matrix/*.properties`; the helper discovers support from those pins.

| Integration | Compatible development targets | Selected versions |
| --- | --- | --- |
| [Better Archeology](https://modrinth.com/mod/better-archeology/versions) | All 12 | 1.3.7 for 1.21/1.21.1 and 26.1/26.1.1/26.1.2; 1.3.8 for 26.2 |
| [Resourceful Config](https://modrinth.com/mod/resourceful-config/versions) | All 12; accompanies Better Archeology | 3.0.11 legacy, 4.0.1 for 26.1.x, 5.0.0 for 26.2 |
| [Dungeons and Taverns](https://modrinth.com/datapack/dungeons-and-taverns/versions) | All 12, using the author's loader-specific mod wrapper | 4.4.4 legacy, 5.2.0 for 26.1.x, 5.3.2 for 26.2 |
| ExtraChests | All 12 via exact locally packaged target JARs | 1.0.2 |
| [Iron Chests by 4nner](https://modrinth.com/mod/ironchest/versions) | Fabric: 1.21, 1.21.1, 26.1.2, 26.2 | 2.0.4 legacy, 2.0.6 for 26.1.2, 2.0.8 for 26.2 |
| [Fluffy's Enchants](https://modrinth.com/datapack/fluffys-enchants/versions) | 1.21 and 1.21.1, either loader, as a world datapack | 5.02; no compatible modern release |

Iron Chests here is the original Fabric project, not the unrelated NeoForge
project with the same name. It is deliberately omitted on unsupported targets.
[ExtraChests' published files](https://modrinth.com/mod/extra-chests/versions)
cover 26.1.2 Fabric/NeoForge and advertise 26.2 NeoForge. Local exact-target
artifacts take priority over those fallbacks.

## Development setup

`gradle/dev-integrations.gradle` attaches mods to Loom's `localRuntime` or
legacy `modLocalRuntime`. Public artifacts have fixed Modrinth version IDs;
Resourceful Config is pinned explicitly because Modrinth Maven has no
transitive dependency graph. Legacy Fabric Iron Chests also needs an explicit
LibGui development dependency: Loom does not load the LibGui/Jankson/LibNinePatch
JARs bundled inside the installed Iron Chests artifact. The exact bundled
LibGui version, `11.1.0+1.21`, is pinned in the two legacy Fabric nodes; the
[author's Maven metadata](https://staging.alexiil.uk/maven/io/github/cottonmc/LibGui/11.1.0%2B1.21/LibGui-11.1.0%2B1.21.pom)
supplies its libraries. These use development runtime scope only, and disabling
Iron Chests also disables them. The helper does not change LootExplorer's
loader metadata.

Exact ExtraChests installable JARs have been copied from the available sibling
build into ignored `dev-integrations/mods/<target>/`. That checkout is read
only and is not a build dependency. On another checkout, put a matching JAR in
that directory, or set `dev_mods_dir` to a directory with the same target layout.
Missing local files fall back to a compatible published pin when one exists;
otherwise that optional mod is skipped. Sources/dev JARs, mismatched Minecraft
metadata, and ambiguous local selections are not loaded.

Use `-Pdev_integrations=false` to disable all restored resources. Individual
switches are `betterarcheology_dev`, `dungeons_and_taverns_dev`,
`extrachests_dev`, `ironchests_dev`, and `fluffys_enchants_dev`.
Put persistent switches or paths in ignored `gradle.local.properties`.

```bash
python build-smart.py :26.1.1-fabric:verifyDevIntegrations
python build-smart.py :26.1.1-fabric:runClient
python build-smart.py :26.1.1-neoforge:runClient
```

Dungeons and Taverns is now available to each target's new/local worlds through
its official mod wrapper. Do not also copy the historical ZIP into the same
world. No old world is shared between Minecraft versions or upgraded.

Fluffy's compatible legacy ZIP is cached once in
`dev-integrations/datapacks/fluffys-enchants/<version-id>/`. Legacy `runClient`
prepares the cache. For an existing legacy target world, install it explicitly:

```bash
python build-smart.py :1.21.1-fabric:stageDevDatapacks -Pdev_world=dev
```

This copies only the compatible ZIP into that target world's `datapacks/`.
Enable it in the world's datapack list, then reload or reopen the world. For a
new world, run `prepareDevDatapacks` first and use the cached ZIP in Create
World's Data Packs screen. The old 6.08 ZIP remains in its original world.

## Manual loot checks

Launch **26.1.1 Fabric** first, then **26.1.1 NeoForge** to check both restored
loader runtimes. In a local creative world, check `nova_structures:chests/`
previews (for example `nova_structures:chests/badland_miner_outpost`), the
`extrachests` wood/trapped chest items, and Better Archeology's suspicious dirt
and suspicious red sand.

Better Archeology spells its loot-table directory `archeology/`, while
LootExplorer's default brushable filter is vanilla `archaeology/`. Add
`archeology/` to the relevant container/brushable filter in LootExplorer's
settings to include those tables, then reopen the world. This existing filter
difference is preserved rather than changing the mod's defaults.

For Fluffy's additional `fluffytg:loot/` tables, use **1.21.1 Fabric** with the
staging command above; its Iron Chests integration is also available there.
LootExplorer's default container filter already includes `loot/`.
Fluffy's world reload/feature behavior still needs this manual check.

## Narrow validation

`verifyDevIntegrations` passed for 26.1.1 Fabric/NeoForge and 1.21.1
Fabric/NeoForge. The 26.1.1 jars contain 93 Better Archeology, 580 Dungeons and
Taverns, and 20 ExtraChests loot tables. The cached Fluffy 5.02 ZIP matches its
published SHA-1 `a2bc273138fb57a6446c373b2ec4b53f890c5473` and contains 112
loot tables. Both 26.1.1 development clients loaded the restored mods, completed
initial resource reload, and initialized the title screen; their owned
processes were stopped. No worlds were opened or modified by these checks.
In-world loot results still need the manual checks above. No full matrix,
package gate, or packaged-release smoke was repeated.
The 26.1.1 Fabric verification also passed with `-Pdev_integrations=false`;
no restored optional fixture remained in its development configuration.

Dependency resolution alone initially missed a 1.21.1 Fabric startup crash
from Iron Chests' missing nested LibGui. The helper now resolves those libraries
explicitly. A separate 1.21.1 NeoForge world log exposed JEI rejecting an empty
preview-removal list; LootExplorer now skips the call when there are no previews.

After these fixes, both 1.21.1 targets compiled and both development clients
joined scratch copies of a 1.21.1 world with the restored integrations. JEI
initialized without the empty-list exception. LootExplorer discovered 119
Dungeons and Taverns chest tables on Fabric; NeoForge also exercised Better
Archeology discovery with `archeology/` enabled in its isolated test config.
The original world was not opened; its `level.dat` hash remained unchanged.
Screen interactions and actual loot-preview contents still need the manual
checklist.

The legacy Dungeons and Taverns 4.4.4 wrapper logs two advancement errors:
`minecraft:wander_add_map` and `minecraft:give_quest_trader_trade` reference
an absent `minecraft:root` parent. These are upstream datapack resources;
world loading and loot-table discovery succeeded. They have not been rewritten
inside LootExplorer.
