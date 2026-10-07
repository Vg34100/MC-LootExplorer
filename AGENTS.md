# AGENTS.md

This file defines the default agent workflow for LootExplorer.

LootExplorer is transitioning from a single-target Architectury project centered on Minecraft 26.1.2 into one maintainable Stonecutter multiversion repository.

Detailed procedures live under `docs/development/`.

## Priorities

1. Preserve existing LootExplorer behavior.
2. Keep the current 26.1.2 implementation as the behavioral reference unless inspection shows a concrete defect.
3. Port outward from the proven current implementation rather than redesigning the mod.
4. Keep one source of truth for versions, loaders, dependencies, artifacts, and release metadata.
5. Represent Minecraft-version drift with the smallest readable compatibility mechanism.
6. Treat Fabric and NeoForge as separate loader axes where their APIs differ.
7. Keep the mod client-focused. Do not invent server support or server gameplay requirements.
8. Minimize context use and validation cost.
9. Preserve unrelated user work.
10. Stop when the requested acceptance evidence exists.

## Desired Target Matrix

The completed migration should support exactly:

```text
1.21      Fabric / NeoForge
1.21.1    Fabric / NeoForge
26.1      Fabric / NeoForge
26.1.1    Fabric / NeoForge
26.1.2    Fabric / NeoForge
26.2      Fabric / NeoForge
```

Once the matrix exists, the registered target set must be discovered from:

```text
gradle/matrix/*.properties
```

Do not maintain duplicate hard-coded target lists when the matrix can be discovered dynamically.

## Current Project Shape

LootExplorer currently has a single modern source baseline around Minecraft 26.1.2.

Treat that implementation as the initial canonical candidate.

The migration problem is primarily:

```text
current 26.1.2 implementation
    ->
current adjacent versions
    ->
legacy 1.21.x backport
```

Do not invent historical branch archaeology if the repository does not actually contain meaningful older ports.

If relevant historical branches/tags exist, inspect them only when they can answer a specific compatibility question.

## Expected Compatibility Hotspots

LootExplorer is a client-focused inspection/UI mod.

Compatibility-sensitive areas are expected to include things such as:

- screen construction and navigation;
- screen rendering APIs;
- widgets and GUI graphics;
- text/component rendering;
- mouse/keyboard input;
- key mappings;
- screen inspection hooks;
- loot-table lookup/query APIs;
- registry/resource identifiers;
- client bootstrap;
- Fabric client initialization;
- NeoForge client initialization/events;
- mixins, if present;
- Mod Menu/config entry, if present;
- metadata/environment declarations.

These are inspection priorities only.

Do not proactively rewrite all of them.

## Final Repository Model

After migration, responsibilities should be:

- `settings.gradle` — Stonecutter target registration and early platform selection.
- `stonecutter.gradle` — active-target and aggregate matrix tasks.
- `build.matrix.gradle` — generic per-target Gradle configuration.
- `gradle/matrix/*.properties` — target-specific Minecraft/loader/Java/dependency facts.
- `common/`, `fabric/`, `neoforge/` — canonical maintained source.
- `gradle/compat/` and/or a small compatibility source area — only where real version boundaries require it.
- `gradle/resource-compat.gradle` — only if serialized/resource metadata differs by version.
- `build-smart.py` — established wrapper for compile/package/runtime/release smoke.
- `scripts/verify-matrix-artifacts.py` — release-artifact verification.
- `scripts/smoke-release-client.py` — packaged-release-JAR client smoke.

Publishing is a separate phase after conversion acceptance.

Fresh-machine doctor/bootstrap work is also separate and should not block the initial conversion.

## Reference Repositories

Use references in this order:

1. **StructureVoidable** — primary reference for the current generic migration/release architecture.
2. **Sagittary** — secondary/deeper reference when StructureVoidable does not contain a needed mechanism.

If sibling checkouts are available, use them READ-ONLY.

Reuse generic architecture and proven helpers, not project-specific behavior.

Do NOT copy from reference mods:

- mod IDs;
- package names;
- publishing IDs;
- feature assertions;
- optional dependencies;
- gameplay logic;
- resource paths;
- server assumptions;
- project-specific compatibility code.

LootExplorer must remain independently buildable without sibling repositories.

## Context Discipline

Search first, read second, edit last.

- Start with `git status --short`.
- Read only files needed to understand the current build and active failure.
- Do not dump the whole Java tree into context.
- Do not inspect every dependency JAR preemptively.
- Do not read the full wrapper/helper scripts once they are established unless they fail or select the wrong plan.
- Prefer targeted diffs and small source excerpts.
- Batch related compatibility fixes before rebuilding.
- Use sentinel targets before the full matrix.

For a fresh session, read only the task-relevant documents:

- migration / adding versions → `docs/development/multiversion-playbook.md`
- compatibility representation → `docs/development/compatibility-policy.md`
- validation decisions → `docs/development/validation-and-release.md`
- publishing → `docs/development/publishing.md`

Fresh-machine setup documentation is not part of normal migration work on an already-configured machine.

## Minecraft/API Investigation

For Minecraft API changes, mappings, class/method availability, mixin targets, or cross-version questions, use this escalation order:

1. `minecraft-dev` MCP.
2. LootExplorer source and relevant repository history.
3. dependency metadata/source.
4. Gradle cache/JAR inspection.
5. `javap` only when narrower methods are insufficient.

Do not begin compatibility work with broad bytecode archaeology.

## Compatibility Hierarchy

Represent differences using the smallest mechanism that keeps behavior visible:

1. unchanged shared source;
2. local Stonecutter `//?` condition;
3. narrow deterministic replacement for a mechanical rename;
4. parsed resource/data transform;
5. separate compatibility implementation when behavior or lifecycle materially differs;
6. small compatibility subsystem only when a whole class family genuinely diverges.

Avoid:

- full source trees per Minecraft version;
- giant legacy overlays;
- broad regex rewriting;
- Gradle acting as a Java source generator;
- copying old source wholesale merely because it compiles.

## Loader Rule

Minecraft-version differences and loader differences are separate axes.

Keep Fabric-only code under Fabric where practical.

Keep NeoForge-only code under NeoForge where practical.

Check loader-specific behavior independently for:

- bootstrap;
- client events;
- key registration;
- config/Mod Menu integration;
- metadata;
- mixins;
- runtime environment declarations.

Do not hide meaningful loader differences behind a forced shared abstraction.

## Client-Only Rule

LootExplorer is expected to be client-focused.

Do not automatically copy server-oriented validation or publication metadata from other mods.

Derive the actual environment from LootExplorer's current source and metadata.

If the mod is client-only:

- metadata should say so;
- publication payloads should say so;
- dedicated-server startup is not a gameplay acceptance gate;
- server-only launch infrastructure should not be added merely for symmetry.

If actual source evidence shows server/common behavior is required, document that explicitly.

## Mixins

If LootExplorer uses mixins, treat them as runtime-sensitive.

For every affected version/loader shape, verify:

- target class;
- target method/descriptor;
- injection point;
- actual dispatch path;
- whether the mixin should exist on that target.

Compilation does not prove a mixin works.

## UI and Rendering

Compilation is not sufficient evidence for GUI behavior.

For changed screen/rendering/input code, validate the representative client manually or via the smallest practical runtime check.

Important behaviors may include:

- opening the inspector UI;
- rendering the inspected screen correctly;
- text/widgets alignment;
- mouse interaction;
- keyboard input;
- closing/back navigation;
- keybind opening;
- scrolling, if present;
- resource reload while a client session is active.

Do not build a GUI automation framework just to avoid a short manual regression pass.

## Loot Querying

Treat loot-table/resource lookup as a separate compatibility surface from GUI rendering.

When APIs differ:

- confirm target registry/resource APIs with Minecraft MCP first;
- keep the query logic behaviorally equivalent;
- do not redesign the feature during a port;
- verify an actual representative lookup in-game after the target launches.

## Build Wrapper

Use the proven `build-smart.py` architecture from the reference repositories.

Treat it as established infrastructure once copied/adapted.

Adapt only project-specific facts such as:

- mod/artifact naming;
- matrix discovery;
- class-origin proof;
- release staging;
- client-only behavior;
- helper references.

Do not redesign the wrapper.

Expected conversion-stage commands include:

```bash
python build-smart.py compile --print-plan

python build-smart.py compile
python build-smart.py compile:fabric
python build-smart.py compile:neoforge
python build-smart.py compile:modern
python build-smart.py compile:legacy

python build-smart.py matrix:compile
python build-smart.py matrix:package

python build-smart.py smoke-release-client:<target>
python build-smart.py release-smoke
```

Do not add publishing commands during the conversion stage.

## Validation Strategy

Use the smallest validation set that can disprove the current change.

Examples:

- shared Java change → affected generation on both loaders;
- Fabric-only change → affected Fabric sentinel;
- NeoForge-only change → affected NeoForge sentinel;
- local Stonecutter condition → test both sides;
- GUI API condition → representative runtime on both API shapes;
- loot-query API condition → representative runtime on both query shapes;
- resource transform → processed/package verification;
- mixin change → runtime on each distinct injection shape;
- build-matrix change → sentinel first, full matrix only when stable.

Do not rerun the full matrix after every small edit.

## Sentinel Rule

Initial representative shapes should cover:

```text
26.2 Fabric
26.2 NeoForge
1.21.1 Fabric
1.21.1 NeoForge
```

If LootExplorer introduces an additional unique compatibility boundary on another target, add that target.

Examples:

- a 26.1-only screen API boundary;
- a 26.1.1-only loader issue;
- a unique resource/mixin boundary.

Do not treat the four default sentinels as magic if the source proves another target differs.

## Full Matrix Gate

Only after sentinel compatibility is stable:

```bash
python build-smart.py matrix:compile
python build-smart.py matrix:package
python scripts/verify-matrix-artifacts.py
```

Run the full matrix once for acceptance.

Do not repeat it unless later edits invalidate the evidence.

## Packaged Release Smoke

Development `runClient` is not the final release proof.

Use the proven packaged-release smoke architecture:

- exact packaged JAR;
- isolated runtime;
- staged artifact hash/origin proof;
- deterministic startup marker;
- bounded timeout;
- owned-process termination;
- no existing launcher profile dependency.

Representative release smoke should cover:

```text
26.2 Fabric
26.2 NeoForge
1.21.1 Fabric
1.21.1 NeoForge
```

Add another target only if LootExplorer has a unique compatibility boundary there.

Once the required representative smokes pass, stop launching production clients.

## Manual Regression

Derive the manual checklist from the actual mod.

Likely checks include:

- keybind opens LootExplorer;
- inspected screen opens correctly;
- UI renders without clipping/misalignment;
- mouse interaction works;
- keyboard/back navigation works;
- loot-table/resource lookup returns expected data;
- changing/closing screens does not crash;
- relevant config/Mod Menu entry works;
- F3+T/resource reload does not break the UI.

Do not invent features that do not exist.

## Stopping Rule

Once requested acceptance evidence is green, stop.

Do not repeat:

- full matrix builds;
- package gates;
- release smokes;
- runtime clients;
- dependency/JAR archaeology;
- reference-repository comparisons;

solely for reassurance.

Continue only when:

- a required criterion remains unresolved;
- a later edit invalidates previous evidence;
- a new deterministic failure appears;
- the user explicitly asks for more validation.

## Git Safety

Before editing:

```bash
git status --short
```

Never revert unrelated user changes.

Do not stage:

- `.env`;
- build output;
- run directories;
- caches;
- downloaded tools;
- validation logs;
- unrelated docs/scripts.

Do not commit, push, tag, publish, merge branches, or delete branches unless the user explicitly authorizes it.

## Closeout

Report only:

- canonical baseline decision;
- compatibility boundaries added;
- files/architecture changed;
- matrix results;
- artifact verification;
- representative runtime/release-smoke results;
- manual checks still required;
- anything intentionally deferred.

Keep raw logs and migration diary material out of the closeout.
