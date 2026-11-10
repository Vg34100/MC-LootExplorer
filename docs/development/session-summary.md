# LootExplorer: sixteen-target port

2026-10-18. Started clean on `main` at `4d53ceb`. The original working 26.1.2
implementation remains the behavioral reference. Version stays 1.0.3; no commit,
push, tag, public-project mutation or publication was performed. Previously
published 1.0.3 files are unchanged. A later release requires a new version.

The matrix now discovers sixteen nodes: 1.20.1 Fabric/Forge and 1.21, 1.21.1,
26.1, 26.1.1, 26.1.2, 26.2, 26.3 Fabric/NeoForge. Java generations are 17/21/25.

| Addition | Compile/package/artifact | Dedicated assertions | Packaged client |
| --- | --- | --- | --- |
| 1.20.1 Fabric | PASS | PASS | PASS, JEI absent / Mod Menu present |
| 1.20.1 Forge | PASS | PASS | PASS, JEI present |
| 26.3 Fabric | PASS | PASS | PASS, JEI absent / Mod Menu present |
| 26.3 NeoForge | PASS | PASS | PASS, JEI present |

- Aggregate compile: **16/16 PASS**. Aggregate package: **16/16 PASS**.
  Structured artifact verification: **16/16 PASS**, including Java levels,
  exact target metadata, side isolation and absence of fixtures/test classes.
  The aggregate gate ran once. Final Forge-only pack/key initialization fixes
  were subsequently compiled, packaged, verified and smoked on Forge only;
  the unaffected fifteen nodes were not rebuilt for reassurance.
- Dedicated servers: **six distinct targets PASS** — all four additions plus
  NeoForge 26.1/26.1.1. Every run passed loot/config/packet-codec assertions,
  reached Done, received owned stop and exited cleanly. The final Forge client
  initialization change also passed a narrow dedicated-server recheck.
- Packaged clients: **4/4 PASS**, with staged SHA-256, exact loaded-class JAR
  origin, resource reload/initial-screen milestone and owned-process shutdown.
  The final Forge run has no missing-pack or late-key-registration warning.
- The template's exact early-NeoForge repair passed tests against both actual
  pinned Architectury binaries, including idempotence and rejected inputs.
- Publishing helper regressions: **19/19 PASS**; read-only plan: **16 nodes**.
  Production dependencies remain Architectury/Fabric API as applicable, optional
  JEI/Mod Menu. Development integrations remain independent and unbundled.
  No platform dry-run or publish preflight was run for this port.

## Durable changes

Native Forge support and four target pins; shared identifier, loot-manager and
NBT/component boundaries; 26.3 modifier/selector/keyboard compatibility;
physical-side bootstrap and directional networking; Forge resource pack and
client initialization; tightly gated 26.1/26.1.1 Architectury repair; target-aware
verification/smoke/publication helpers and three-Java-generation CI/editor tasks.

Custom editable JSON, packet identifiers/layouts, creative identifiers and saved
item data are preserved. No SimpleConfig migration, content redesign or new shared
runtime library was introduced. See [port details](lootexplorer-1.20.1-and-26.3-port.md).

## Remaining manual checks and host limitation

Actual keybind opening/copying, preview layout/tooltips/scrolling, settings-screen
editing/Done/Cancel/paging, placed chest/brushable behavior, F3+T, JEI ingredient
filtering and actual two-process request/response exchange remain manual. Native
assertions prove codec round-trips and real server loot behavior, not multiplayer
transport or visual QA. See [the checklist](lootexplorer-runtime-checklist.md).

Loom's 26.3 Fabric client hit the same native Windows access violation under the
host Microsoft JDK 25.0.3 documented by FishingFrenzy. Its unchanged JAR passed with
PortableMC/Mojang-managed Java; that production launcher is selected in the matrix.
The root cause of the host development-launcher fault remains unconfirmed. Default
`runClient` on that JDK is not established by the successful production smoke.

Raw server/artifact/client evidence is ignored under `build/port-validation/` and
isolated Windows temporary runtimes. Nothing generated, secret or machine-local
is intended for version control. Publication and fresh-machine setup remain
separate work.
