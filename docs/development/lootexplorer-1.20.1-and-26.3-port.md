# LootExplorer: 1.20.1 and 26.3

The original working 26.1.2 behavior remains the reference. Four matrix files add
1.20.1 Fabric/Forge (Java 17) and 26.3 Fabric/NeoForge (Java 25), preserving the
original twelve nodes. Targets are discovered from `gradle/matrix/*.properties`.
No release version, public file, tag or external project setting changes here.

The read-only shared references were `.reference/template-stonecutter-multiversion`
(standardization/adoption guides, Forge build/bootstrap, networking side split and
ASM 9.11) and FishingFrenzy's completed 1.20.1/26.3 port. No sibling checkout is a
build dependency. JEI pins are LootExplorer-specific: 15.20.0.135 on 1.20.1 and
31.16.0.84 on 26.3. The latter matches current NeoForge's configuration API.

## Compatibility boundaries

- `ModIds` owns the 1.20.1 identifier constructor versus newer static factories;
  the existing exact ResourceLocation/Identifier Stonecutter rename remains.
- `ItemPreviewData` owns legacy item NBT versus data components. It preserves
  preview marker, names, lore, chest loot/seed, brushable block entity ID/table
  and enchantment glint. It introduces no registered item or enchantment.
- `LootTables` uses the 1.20.1 loot manager and the 1.21+ reloadable registry.
  Discovery and config filters recognize the generation's `loot_tables/` or
  `loot_table/` directory. Config filename, keys, defaults and editable path
  lists remain unchanged; the custom JSON backend is retained.
- 26.3 changes resource selection to a functional selector, keyboard input to
  `KEYBOARD`, and loot functions to structured `modifier` objects/lists with
  `type`. The shared parser still accepts older `functions`/`function` forms.
  It retains the baseline's entry traversal and preview interpretation rather
  than replacing them with a new loot engine. Archaeology sampling supplies
  its brush/entity context where required.
- Forge 1.20.1 supplies a real resource pack (`pack_format=15`, owned by its
  matrix pin). Client registration occurs while Forge constructs its physical-client
  subscriber, before Architectury collects mappings; settings remain in client setup.
- The existing legacy preview screen gains only 1.20.1 background/scroll
  signatures. JSON settings retain their dynamic list editor. Dormant accessor
  source follows renamed loot classes/fields; dormant injections stay disabled.

## Common and client initialization

The audit confirmed that the old NeoForge entrypoint was client-filtered,
common initialization loaded keybinds, and the packet handler was not registered.
Common initialization now loads configuration, registers the existing creative
identifiers, server loot discovery and C2S handling. Physical-client entrypoints
own keybinds, S2C receivers and settings screens. Forge registers its Architectury
mod event bus before common registration. Dedicated servers register only the
outbound modern S2C codec; legacy raw-buffer channels retain the same packet IDs.
Existing modern packet field order/codecs are unchanged.

Client preview requests use the existing protocol when the table is unavailable
locally. A peer without the channel is skipped. This does not add automatic
remote creative-tab table synchronization. Fabric metadata permits both physical
sides; Forge/NeoForge common dependencies use BOTH and optional JEI remains CLIENT.
Future publication configuration reflects an optional server component. The
published client-only 1.0.3 release remains untouched and cannot be republished
with these changed bytes.

## Pinned early-NeoForge repair

A real dedicated 26.1 run reproduced Architectury 20.0.4's missing
`event.level.block.BreakBlockEvent` while the loader supplies
`event.level.BlockEvent$BreakEvent`. The template's exact repair is activated
only on NeoForge 26.1/26.1.1. Its plugin/anchor/resource stay absent elsewhere;
ASM and Mixin are supplied by the loader. Before mutation it verifies the exact
bridge descriptor and seven invocation owners, repairs frame/local references,
and tolerates an identical repair already applied by another mod. Both pins'
`verifyEarlyNeoForgeCompat` tasks check real library bytes, idempotence, unrelated
methods and rejection without partial edits. No broad descriptor rewriting or
new runtime dependency was introduced.

## Optional integrations and checks

JEI and Fabric Mod Menu remain optional compile APIs/development runtimes.
Installable runtime mod JARs do not pull their publication POM's annotation/API
artifacts into mod discovery. `-Pjei_runtime=false` exercises absence. Modern
JEI includes its own config dependency; it is not a direct LootExplorer
requirement. Existing test-fixture pins are untouched; the four additions do not
invent unverified fixture pins. `-Pdev_integrations=false` disables those fixtures.

`python scripts/check-port-runtime.py <target> [...]` runs a separate opt-in
`portChecks` development mod in isolated temporary worlds. It checks vanilla
chest/archaeology discovery and sampling, generated preview data, enchanted-book
modifiers, old/current JSON round-trips and request/response codec item-data
round-trips. Passing requires its assertion marker, Minecraft Done, owned stop
and a clean exit. It does not claim a real client/server packet exchange.

The artifact verifier checks all discovered targets, Forge metadata/manifest,
Java 17/21/25, exact releases, common-class side isolation and absence of test or
fixture payloads. The production smoke helper uses the proven PortableMC backend for
Forge/NeoForge and legacy Fabric. 26.3 Fabric also selects PortableMC via its
matrix property: the host Microsoft JDK 25.0.3 reproduced FishingFrenzy's native
Windows access violation during Loom resource loading. The production check
uses Mojang-managed Java and unchanged mod bytes; the native fault's root cause
is not established. Other current Fabric targets retain Loom. CI discovers sixteen assets and provisions all
three Java generations; it was not run or published in this session.

See [session-summary.md](session-summary.md) for completed gates and
[the runtime checklist](lootexplorer-runtime-checklist.md) for remaining manual QA.
