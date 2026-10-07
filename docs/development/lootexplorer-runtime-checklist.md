# LootExplorer runtime regression checklist

Run the feature checks on 26.2 Fabric/NeoForge and 1.21.1 Fabric/NeoForge. Add a target only when its implementation has a distinct compatibility boundary.

Packaged startup smoke proves the exact release JAR, loader bootstrap, initial resource reload and first screen. It does not establish the in-world/UI checks below.

- Start a local creative world. Loot discovery uses its integrated server resources; no dedicated-server installation is required. Check that the Container Loot Tables and Brushable Loot Tables creative tabs populate for the configured paths.
- Select a generated chest preview for a known vanilla table such as `minecraft:chests/simple_dungeon`. Hold it and press Insert (Preview Loot Table). Confirm representative items appear and the title, background, item grid and tooltips render correctly.
- Hold the same preview and press O (Copy Lore). Confirm the copied text is the table identifier. Verify both keys appear under Loot Explorer in Controls and can be rebound.
- With a preview containing more than 63 items, exercise mouse-wheel scrolling and scrollbar dragging/release. Confirm the item grid and hover tooltips follow the selected rows.
- Close with Escape, reopen the preview, change GUI scale and resize the window. Check clipping, alignment, mouse hitboxes and back navigation.
- Open settings through Fabric Mod Menu or NeoForge's mod configuration button. Type paths, add/remove rows, switch pages, then use Done, Cancel and Escape. Restart the world after saving and confirm discovery uses the saved paths.
- Use F3+T with the client active, then reopen settings and the loot preview. Check for crashes and missing textures/text. The baseline discovers loot when a world starts; configuration/resource changes are checked again on the next world start.
- With JEI installed, confirm generated preview stacks are hidden from its ingredient list while LootExplorer's creative tabs remain usable. Also check startup and preview behavior without JEI; JEI and Mod Menu remain optional.
- Close/reopen the world and repeat a known loot lookup. Review the log for parsing/registry errors. Datapack or mod tables should follow the configured container/brushable paths.

The restored optional development mods/datapacks, their target coverage, and
specific 26.1.1 loot checks are documented in
[LootExplorer development integrations](lootexplorer-dev-integrations.md).

LootExplorer is client-focused but its lookup needs an integrated server. Remote-server table discovery is not established by this checklist. Networking helper classes and mixin helper classes exist in the baseline but are not registered by its entrypoints/configuration.
