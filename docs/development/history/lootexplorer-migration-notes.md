# LootExplorer conversion facts

The original 26.1.2 implementation at `bfed8a4` is the behavioral baseline, mod version 1.0.2. Both original Architectury loader packages compiled successfully before conversion. The pre-existing deleted root build file was tested in an isolated copy, without restoring it in the worktree.

## Maintained architecture

`settings.gradle` discovers and validates `gradle/matrix/*.properties`; `stonecutter.gradle` selects 26.1.2 Fabric for editing and derives aggregate tasks from those files. `build.matrix.gradle` combines the canonical `common/` and selected loader sources using Stonecutter's native preparation, with an explicit non-empty-source guard. Generated `versions/` views are disposable. No sibling checkout is a build/runtime dependency.

The matrix owns Minecraft, loader, Java, Loom generation, Architectury API, Fabric API/Loader, JEI and Mod Menu pins. Root properties retain only shared identity/build settings. Legacy uses Java 21 and `remapJar`; current uses Java 25 and `jar`. Installable artifacts are `build/libs/<target>/LootExplorer-<loader>-<minecraft>-1.0.2.jar`.

## Proven compatibility boundaries

- **26.2:** navigation is `Minecraft.gui.setScreen`; the brushable block-entity constant lives in `BlockEntityTypes`. Local source conditions preserve the prior calls on earlier targets.
- **1.21.x identifiers:** `ResourceLocation` and `Identifier` are one exact, bidirectional native Stonecutter string replacement. This is a class rename, with no custom Java rewrite pipeline.
- **1.21.x client APIs:** key categories are strings, action-bar messages use `displayClientMessage(component, true)`, and the settings screen uses immediate `GuiGraphics` rendering. Local conditions retain the same controls/layout.
- **1.21.x preview screen:** immediate rendering requires `renderBg`, explicit tooltip rendering and positional mouse methods. Only `gradle/compat/legacy/common/net/vg/lootexplorer/inventory/LootPreviewScreen.java` overrides the canonical class. Its dimensions, grid, labels, scroll calculations and hitboxes match the baseline.
- **1.21.x loot components:** legacy uses `CustomData` containing the block-entity registry ID, `HIDE_ADDITIONAL_TOOLTIP`, the older NBT boolean getter, item hover name and enchantment holder lookup. These are local branches in the shared loot handler. Resource discovery and loot parsing remain shared.
- **JEI:** legacy gets vanilla item ingredients through its older API; current retains `getAllItemStacks`. Both filter the existing generated-preview marker. JEI and Mod Menu are compile/development runtime integrations, remain optional in metadata, and are not embedded.
- **Metadata:** exact Minecraft/dependency/Java facts expand from the matrix. NeoForge 26.2 uses `iconFile`, earlier targets use `logoFile`. Language/icons/services are unchanged; no serialized gameplay-resource transform is needed.

Minecraft API facts were checked with `minecraft-dev` before patching. Loader environment declarations follow the [NeoForge client entrypoint documentation](https://docs.neoforged.net/docs/gettingstarted/modfiles/) and [NeoForge metadata example](https://github.com/neoforged/ModDevGradle/blob/main/testproject/src/main/resources/META-INF/neoforge.mods.toml).

## Environment and runtime scope

The baseline registers client key mappings and screens. Loot discovery listens to the local integrated server's startup and reads its resources. Fabric now declares `environment: client`; NeoForge's entrypoint is restricted to `Dist.CLIENT` with client dependency sides and `IGNORE_ALL_VERSION`. Dedicated-server installation/gameplay and remote-server loot discovery are not acceptance requirements.

The baseline mixin config registers no injections. Dormant accessor/network helpers remain present without enabling a new feature or injection. Compilation of those helpers does not imply active networking or mixin behavior.

`build-smart.py` retains the established wrapper; representative smokes are selected by `release_smoke=true` matrix facts. Fabric uses Loom production clients, NeoForge uses checksum-pinned PortableMC 5.0.5 Windows x64. Each stages and hashes the exact release JAR, verifies loaded class origin, observes initial reload/first-screen startup, applies a timeout and terminates only its owned client.

The [runtime checklist](../lootexplorer-runtime-checklist.md) separates packaged startup evidence from manual in-world/UI checks. Publishing, version bumps and fresh-machine doctor/bootstrap are separate later work; seeded helpers for those phases were not implemented or exercised by this conversion.

## Conversion acceptance — 2026-10-06

- `matrix:compile`: PASS, all 12 discovered targets.
- `matrix:package`: PASS, all 12 installable artifacts.
- `python3.12 scripts/verify-matrix-artifacts.py`: `ARTIFACT VERIFICATION PASS (12/12)`, including parsed JSON/TOML metadata, class levels, release selectors and required classes/resources.
- `release-smoke`: `RELEASE SMOKE PASS (4/4)`.

| Packaged target | Backend | Result |
| --- | --- | --- |
| 26.2 Fabric | Loom production client | PASS |
| 26.2 NeoForge | PortableMC 5.0.5 Windows x64 | PASS |
| 1.21.1 Fabric | Loom production client | PASS |
| 1.21.1 NeoForge | PortableMC 5.0.5 Windows x64 | PASS |

Every smoke verified the staged release hash and loaded LootExplorer class origin, observed initial resource reload plus completed startup/first-screen initialization, and stopped only its owned client. The adjacent targets introduced no additional compatibility boundary requiring a launch. No production clients were launched after these four passes.

Manual in-world lookup, preview rendering/input, configuration, optional JEI present/absent, and resource-reload interaction checks remain for a human pass using the runtime checklist. Startup smoke does not claim that feature QA. No publishing, bootstrap, version bump, commit or push was performed.
