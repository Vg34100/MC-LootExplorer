#!/usr/bin/env python3
"""Verify LootExplorer's installable matrix JARs, not gameplay behavior."""
import json
from pathlib import Path
import re
import struct
from zipfile import BadZipFile, ZipFile

try:
    import tomllib
except ImportError:  # The established wrapper also supports Python 3.10.
    tomllib = None

ROOT = Path(__file__).resolve().parents[1]
MOD_ID = 'lootexplorer'
PACKAGE = 'net/vg/lootexplorer/'


def properties(path):
    return {key.strip(): value.strip() for line in path.read_text().splitlines()
            if '=' in line and not line.lstrip().startswith('#')
            for key, value in [line.split('=', 1)]}


def quoted(text, key):
    match = re.search(rf'^\s*{re.escape(key)}\s*=\s*"([^"\n]*)"', text, re.MULTILINE)
    assert match, f'Missing metadata field: {key}'
    return match.group(1)


def release_jar(matrix):
    """Select exactly one expected release; reject classifiers and stale versions."""
    jars = [p for p in (ROOT / 'build/libs' / matrix.stem).glob('*.jar')
            if not p.name.endswith(('-dev.jar', '-sources.jar', '-javadoc.jar',
                                    '-dev-shadow.jar', '-raw.jar'))]
    assert len(jars) == 1, f'Expected one release JAR: {jars}'
    root_pins = properties(ROOT / 'gradle.properties')
    minecraft, loader = matrix.stem.rsplit('-', 1)
    expected = f"{root_pins['archives_name']}-{loader}-{minecraft}-{root_pins['mod_version']}.jar"
    assert jars[0].name == expected, f'Wrong/stale release artifact: {jars[0]} (expected {expected})'
    return jars[0]


def inspect(matrix):
    pins = properties(matrix)
    assert matrix.stem == f"{pins['minecraft_version']}-{pins['loader']}", 'Target/property mismatch'
    artifact = release_jar(matrix)
    legacy = pins['loom_generation'] == 'legacy'
    root_pins = properties(ROOT / 'gradle.properties')
    with ZipFile(artifact) as jar:
        entries = jar.namelist()
        files = set(entries)
        assert len(entries) == len(files), 'Duplicate archive entries'
        read_json = lambda name: json.loads(jar.read(name))
        required_classes = (
            'LootExplorer', 'config/LootExplorerConfig', 'client/LootExplorerConfigScreen',
            'inventory/LootPreviewScreen', 'inventory/LootPreviewMenu', 'util/ModKeyMaps',
            'util/LootHandler', 'item/ModItemGroups', 'compat/jei/LootExplorerJeiPlugin',
        )
        for name in required_classes:
            assert PACKAGE + name + '.class' in files, f'Missing mod class: {name}'
        classes = [name for name in files if name.endswith('.class')]
        assert classes and all(name.startswith(PACKAGE) for name in classes), 'Bundled dependency classes'
        for name in classes:
            bytecode = jar.read(name)
            assert bytecode[:4] == b'\xca\xfe\xba\xbe', f'Invalid class: {name}'
            assert struct.unpack('>H', bytecode[6:8])[0] == int(pins['java_version']) + 44, f'Wrong class level: {name}'
        for name in files:
            if name.endswith('.json'):
                read_json(name)
        mixins = read_json('lootexplorer.mixins.json')
        assert mixins['required'] is True
        assert mixins['compatibilityLevel'] == 'JAVA_' + pins['java_version']
        # The canonical baseline registers no injections; dormant helpers stay dormant.
        assert mixins['client'] == [] and mixins['mixins'] == [], 'Unexpected active mixins'
        assert mixins['package'] == 'net.vg.lootexplorer.mixin'
        for side in ('mixins', 'client', 'server'):
            for mixin in mixins.get(side, []):
                name = (mixins['package'] + '.' + mixin).replace('.', '/') + '.class'
                assert name in files, f'Missing mixin class: {name}'
        if pins['loader'] == 'fabric':
            assert 'META-INF/neoforge.mods.toml' not in files, 'Wrong loader metadata'
            meta = read_json('fabric.mod.json')
            assert meta['id'] == MOD_ID and meta['version'] == root_pins['mod_version']
            assert meta['depends']['minecraft'] == pins['minecraft_version']
            assert set(meta['depends']) == {'java', 'minecraft', 'fabricloader', 'architectury', 'fabric-api'}
            for dep, pin in (('java', 'java_version'), ('fabricloader', 'fabric_loader_version'),
                             ('architectury', 'architectury_api_version')):
                assert meta['depends'][dep] == '>=' + pins[pin], f'Wrong dependency: {dep}'
            assert meta['suggests'] == {'modmenu': '*', 'jei': '*'}
            assert meta['mixins'] == ['lootexplorer.mixins.json']
            assert meta['environment'] == 'client'
            assert meta['entrypoints'] == {
                'main': ['net.vg.lootexplorer.fabric.LootExplorerFabric'],
                'client': ['net.vg.lootexplorer.fabric.client.LootExplorerFabricClient'],
                'modmenu': ['net.vg.lootexplorer.fabric.client.LootExplorerModMenu'],
            }
            for names in meta['entrypoints'].values():
                for name in names:
                    assert name.replace('.', '/') + '.class' in files, f'Missing entrypoint: {name}'
            assert meta['icon'] in files
            assert 'accessWidener' not in meta
            if legacy:
                assert b'net/minecraft/class_' in jar.read(PACKAGE + 'inventory/LootPreviewScreen.class'), 'Unremapped Fabric release'

        else:
            assert 'fabric.mod.json' not in files, 'Wrong loader metadata'
            meta = jar.read('META-INF/neoforge.mods.toml').decode()
            assert quoted(meta, 'modId') == MOD_ID
            assert quoted(meta, 'version') == root_pins['mod_version']
            assert PACKAGE + 'neoforge/LootExplorerNeoForge.class' in files
            blocks = re.findall(r'\[\[dependencies\.lootexplorer\]\](.*?)(?=\n\[|\Z)', meta, re.DOTALL)
            deps = {quoted(block, 'modId'): block for block in blocks}
            assert set(deps) == {'minecraft', 'neoforge', 'architectury', 'jei'}
            assert quoted(deps['minecraft'], 'versionRange') == '[' + pins['minecraft_version'] + ']'
            for dep, pin in (('neoforge', 'neoforge_version'), ('architectury', 'architectury_api_version')):
                assert quoted(deps[dep], 'versionRange') == '[' + pins[pin] + ',)'
            assert all(quoted(deps[dep], 'type') == 'required' for dep in ('minecraft', 'neoforge', 'architectury'))
            assert quoted(deps['jei'], 'type') == 'optional'
            assert quoted(deps['jei'], 'versionRange') == '[' + pins['jei_version'] + ',)'
            assert all(quoted(block, 'side') == 'CLIENT' for block in deps.values())
            assert quoted(meta, 'displayTest') == 'IGNORE_ALL_VERSION'
            assert b'CLIENT' in jar.read(PACKAGE + 'neoforge/LootExplorerNeoForge.class'), 'Missing client-only bootstrap'
            if tomllib is not None:
                tomllib.loads(meta)
            assert quoted(meta, 'javaVersion') == '[' + pins['java_version'] + ',)'
            icon_field = 'iconFile' if pins['minecraft_version'] == '26.2' else 'logoFile'
            assert quoted(meta, icon_field) in files
            assert quoted(meta, 'config') == 'lootexplorer.mixins.json'
        assert 'assets/lootexplorer/lang/en_us.json' in files
        assert jar.read('META-INF/services/mezz.jei.api.IModPlugin').decode().strip() == 'net.vg.lootexplorer.compat.jei.LootExplorerJeiPlugin'
        assert any(name.startswith('LICENSE') for name in files)
        assert not any(name.endswith(('.jar', '.java')) for name in files), 'Embedded JAR or source archive'
        for name in ('fabric.mod.json', 'META-INF/neoforge.mods.toml', 'lootexplorer.mixins.json'):
            if name in files:
                assert b'${' not in jar.read(name), f'Unexpanded metadata: {name}'
    print(f'{matrix.stem}: release metadata, classes, mixins and resources OK')
    return artifact


if __name__ == '__main__':
    matrices = sorted((ROOT / 'gradle/matrix').glob('*.properties'))
    assert matrices, 'No matrix properties found'
    for matrix in matrices:
        try:
            inspect(matrix)
        except (AssertionError, KeyError, ValueError, OSError, BadZipFile) as error:
            raise SystemExit(f'{matrix.stem}: {error}') from error
    print(f'ARTIFACT VERIFICATION PASS ({len(matrices)}/{len(matrices)})')
