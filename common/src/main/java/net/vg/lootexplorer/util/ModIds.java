package net.vg.lootexplorer.util;

import net.minecraft.resources.Identifier;
import net.vg.lootexplorer.Constants;

/** Stable namespaces, with Minecraft's identifier constructor boundary in one place. */
public final class ModIds {
    private ModIds() {}
    public static Identifier parse(String text) {
        //? if >=1.21 {
        return Identifier.parse(text);
        //? } else {
        /*return new Identifier(text);
        *///? }
    }
    public static Identifier id(String path) { return id(Constants.MOD_ID, path); }
    public static Identifier id(String namespace, String path) {
        //? if >=1.21 {
        return Identifier.fromNamespaceAndPath(namespace, path);
        //? } else {
        /*return new Identifier(namespace, path);
        *///? }
    }
}
