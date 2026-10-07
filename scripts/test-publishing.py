#!/usr/bin/env python3
"""Small stdlib-only regression tests for publication safety; no real secrets/API writes."""
import os
from pathlib import Path
import runpy
import subprocess
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
WRAPPER = runpy.run_path(str(ROOT / "build-smart.py"))
PUBLISH = runpy.run_path(str(ROOT / "scripts/publish-release.py"))


class PublishingSafety(unittest.TestCase):
    def test_dotenv_is_child_only_and_environment_wins(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / ".env").write_text("# comment\n\nMODRINTH_TOKEN='synthetic-test-value'\nCURSEFORGE_TOKEN=synthetic-cf-value\nJAVA_HOME=ignored\n")
            result = subprocess.CompletedProcess([], 0, stdout=b"", stderr=b"")
            with patch.dict(os.environ, {}, clear=True), patch('subprocess.run', return_value=result):
                child = WRAPPER["publishing_environment"](root)
                self.assertEqual(child, {"MODRINTH_TOKEN": "synthetic-test-value", "CURSEFORGE_TOKEN": "synthetic-cf-value"})
                self.assertNotIn("MODRINTH_TOKEN", os.environ)
            with patch.dict(os.environ, {"MODRINTH_TOKEN": "explicit-test-value", "CURSEFORGE_TOKEN": "explicit-cf-value"}, clear=True), patch('subprocess.run', return_value=result):
                with patch.object(Path, 'open', side_effect=AssertionError("must not open .env")):
                    self.assertEqual(WRAPPER["publishing_environment"](root)["MODRINTH_TOKEN"], "explicit-test-value")

    def test_tracked_env_stops_before_open(self):
        result = subprocess.CompletedProcess([], 0, stdout=b".env\n", stderr=b"")
        with patch('subprocess.run', return_value=result), patch.object(Path, 'open', side_effect=AssertionError("must not open .env")):
            with self.assertRaisesRegex(WRAPPER["PlanError"], "STOP: .env is tracked"):
                WRAPPER["publishing_environment"](ROOT)

    def test_confirmation_required_before_other_work(self):
        project = WRAPPER["Project"](ROOT)
        self.assertEqual(PUBLISH["run"](project, "publish:modrinth", [], False, {}), 2)
        self.assertEqual(PUBLISH["run"](project, "publish:curseforge", [], False, {}), 2)
        self.assertEqual(PUBLISH["run"](project, "publish:all", [], False, {}), 2)

    def test_plan_is_read_only_and_matrix_derived(self):
        with patch.object(Path, 'write_text', side_effect=AssertionError("read-only")), patch('subprocess.run', side_effect=AssertionError("no subprocess")):
            data = PUBLISH["publication_plan"]()
        self.assertEqual({r["target"] for r in data["entries"]}, {p.stem for p in (ROOT / 'gradle/matrix').glob('*.properties')})
        self.assertEqual(len({r['version_number'] for r in data['entries']}), len(data['entries']))
        self.assertEqual(len(data['entries']), 12)
        self.assertEqual(data['project_id'], 'pVa5DTcR')
        self.assertEqual(data['curseforge_project_id'], '1072637')
        self.assertEqual(data['modrinth_environments'], dict(client_side='required', server_side='unsupported'))
        for row in data['entries']:
            loader = {'fabric': 'Fabric', 'neoforge': 'NeoForge'}[row['loader']]
            self.assertEqual(row['version_name'], f"[{loader}] Loot Explorer {data['mod_version']} ({row['minecraft']})")
            self.assertEqual(row['version_number'], f"{data['mod_version']}-{row['minecraft']}-{row['loader']}")
            self.assertEqual(row['artifact'], f"build/libs/{row['target']}/LootExplorer-{row['loader']}-{row['minecraft']}-{data['mod_version']}.jar")
            mods = {d['mod_id'] for d in row['dependencies']}
            self.assertEqual(mods, {'architectury', 'fabric-api', 'modmenu', 'jei'} if row['loader'] == 'fabric' else {'architectury', 'jei'})
            kinds = {d['mod_id']: d['dependency_type'] for d in row['dependencies']}
            self.assertEqual(kinds['architectury'], 'required')
            self.assertEqual(kinds['jei'], 'optional')
            self.assertEqual({d['mod_id']: (d['project_id'], d['curseforge_slug']) for d in row['dependencies']},
                             {k: v for k, v in {'architectury': ('lhGA9TYQ', 'architectury-api'),
                                               'fabric-api': ('P7dR8mSH', 'fabric-api'),
                                               'jei': ('u6dRKJwZ', 'jei'),
                                               'modmenu': ('mOgUt4GM', 'modmenu')}.items() if k in mods})
            self.assertFalse(mods & {'betterarcheology', 'resourcefulconfig', 'extrachests', 'ironchest', 'mr_dungeons_andtaverns', 'fluffytg'})
            if row['loader'] == 'fabric':
                self.assertEqual(kinds['fabric-api'], 'required')
                self.assertEqual(kinds['modmenu'], 'optional')
            self.assertEqual(row['curseforge_versions'], [row['minecraft'], loader, f"Java {row['java']}", 'Client'])
            self.assertEqual(row['environments'], ['Client'])

    def test_curseforge_exact_metadata_and_duplicate_guard(self):
        data = PUBLISH['publication_plan']()
        names = {name for row in data['entries'] for name in row['curseforge_versions']}
        versions = [dict(name=name, gameVersionTypeID=1) for name in names]
        PUBLISH['check_curseforge_versions'](data, versions)
        with self.assertRaisesRegex(ValueError, 'Unsupported CurseForge'):
            PUBLISH['check_curseforge_versions'](data, [v for v in versions if v['name'] != '26.2'])
        existing = dict(id=1072637, files=[dict(id=1, display=data['entries'][0]['version_name'])])
        with self.assertRaisesRegex(ValueError, 'Existing CurseForge'):
            PUBLISH['check_curseforge_duplicates'](data, existing)
        PUBLISH['check_curseforge_duplicates'](data, dict(id=1072637, files=[]))

    def test_resume_requires_exact_artifact_receipt(self):
        import json
        data = PUBLISH['publication_plan']()
        row = data['entries'][0]
        for entry in data['entries']:
            entry.update(sha256='synthetic-hash', size=100)
        for platform in ('modrinth', 'curseforge'):
            receipt = dict(platform=platform, target=row['target'], mod_version=data['mod_version'],
                           project_id=data['project_id'] if platform == 'modrinth' else data['curseforge_project_id'],
                           artifact=Path(row['artifact']).name, sha256=row['sha256'], size=row['size'], id='synthetic-id')
            with patch.object(Path, 'is_file', return_value=True), patch.object(Path, 'read_text', return_value=json.dumps(receipt)):
                self.assertEqual(len(PUBLISH['pending_entries'](data, platform)), len(data['entries'])-1)
            for change in ({'sha256': 'wrong'}, {'project_id': 'wrong'}, {'artifact': 'wrong.jar'}):
                with patch.object(Path, 'is_file', return_value=True), patch.object(Path, 'read_text', return_value=json.dumps(dict(receipt, **change))):
                    with self.assertRaisesRegex(ValueError, 'Conflicting upload receipt'):
                        PUBLISH['pending_entries'](data, platform)

    def test_exact_and_historical_duplicate_guards(self):
        data = PUBLISH["publication_plan"]()
        row = next(r for r in data['entries'] if r['target'] == '26.1.2-fabric')
        for number in (row['version_number'], data['mod_version'], 'lootexplorer-' + data['mod_version'] + '-v26.1.2-fabric'):
            existing = [dict(id='synthetic-version', version_number=number, game_versions=[row['minecraft']], loaders=[row['loader']])]
            with self.assertRaisesRegex(ValueError, 'Existing Modrinth'):
                PUBLISH['check_duplicates'](data, existing)
        PUBLISH['check_duplicates'](data, [])

    def test_semantic_duplicates_check_target_and_loader(self):
        data = PUBLISH['publication_plan']()
        row = next(r for r in data['entries'] if r['target'] == '26.2-fabric')
        existing = dict(id='synthetic-version', version_number='historical-name',
                        name='Loot Explorer ' + data['mod_version'], game_versions=['26.2'], loaders=['fabric'])
        with self.assertRaisesRegex(ValueError, 'Existing Modrinth'):
            PUBLISH['check_duplicates'](data, [existing])
        for change in ({'name': 'Loot Explorer ' + data['mod_version'] + '0'}, {'loaders': ['quilt']}, {'game_versions': ['1.20.1']}):
            PUBLISH['check_duplicates'](data, [dict(existing, **change)])
        file = dict(id=1, name='lootexplorer-' + data['mod_version'] + '-v26.2-fabric.jar', versions=['26.2', 'Fabric'])
        with self.assertRaisesRegex(ValueError, 'Existing CurseForge'):
            PUBLISH['check_curseforge_duplicates'](data, dict(id=1072637, files=[file]))
        PUBLISH['check_curseforge_duplicates'](data, dict(id=1072637, files=[dict(file, versions=['26.2', 'Quilt'])]))

    def test_public_project_identity_and_environments_must_match(self):
        data = PUBLISH['publication_plan']()
        project = dict(id=data['project_id'], slug=data['project_slug'], **data['modrinth_environments'])
        for change in ({'id': 'wrong'}, {'slug': 'wrong'}, {'server_side': 'required'}):
            with patch('urllib.request.urlopen') as urlopen:
                import json
                urlopen.return_value.__enter__.return_value.read.return_value = json.dumps(dict(project, **change)).encode()
                with self.assertRaises(ValueError):
                    PUBLISH['check_modrinth_project'](data)

    def test_historical_environment_allowed_only_for_dry_run(self):
        import json
        data = PUBLISH['publication_plan']()
        project = dict(id=data['project_id'], slug=data['project_slug'], **data['modrinth_legacy_environments'])
        with patch('urllib.request.urlopen') as urlopen, patch('builtins.print'):
            urlopen.return_value.__enter__.return_value.read.return_value = json.dumps(project).encode()
            PUBLISH['check_modrinth_project'](data, dry_run=True)
            with self.assertRaisesRegex(ValueError, 'Client-only'):
                PUBLISH['check_modrinth_project'](data)

    def test_unmapped_metadata_and_wrong_mod_namespace_fail(self):
        config = PUBLISH['properties'](ROOT / 'gradle/publishing.properties')
        with self.assertRaisesRegex(ValueError, 'Unmapped'):
            PUBLISH['dependencies']('{"depends":{"unexpected-mod":"*"}}', 'fabric', {}, config)
        with self.assertRaisesRegex(ValueError, 'Missing NeoForge dependency'):
            PUBLISH['dependencies']('[[dependencies.unrelated]]\nmodId="architectury"\ntype="required"\n', 'neoforge', {}, config)

    def test_cli_overrides_cannot_bypass_confirmation_or_debug_mode(self):
        project = WRAPPER['Project'](ROOT)
        for argument in ('--parallel', '--continue', '--scan', '--debug', '--info', '-Ppublish_confirm=true', '-Dorg.gradle.project.publish_confirm=true', '-PMODRINTH_TOKEN=synthetic'):
            with self.assertRaisesRegex(ValueError, 'Publishing forbids'):
                PUBLISH['run'](project, 'publish:modrinth-dry-run', [argument], False, {})

    def test_dry_run_strips_credentials_and_never_opens_dotenv(self):
        data = PUBLISH['publication_plan']()
        project = WRAPPER['Project'](ROOT)
        calls = []
        def gradle(project, tasks, java, env):
            calls.append((tasks, env))
            return 0, []
        wrapper = dict(select_gradle_java=lambda *args: (None, None, False), run_gradle=gradle,
                       process_output=lambda *args: None,
                       publishing_environment=lambda *args: self.fail('dry-run requested credentials'))
        replacements = dict(publication_plan=lambda *args: data, verified_plan=lambda *args: data,
                            check_modrinth_project=lambda *args, **kwargs: None, check_duplicates=lambda *args: None,
                            verify_debug_output=lambda *args: None)
        with patch.dict(PUBLISH['run'].__globals__, replacements), patch.dict(os.environ, {'MODRINTH_TOKEN': 'synthetic-mr', 'CURSEFORGE_TOKEN': 'synthetic-cf'}), patch.object(Path, 'open', side_effect=AssertionError('must not open .env')), patch.object(Path, 'write_text'), patch.object(Path, 'mkdir'), patch('builtins.print'):
            self.assertEqual(PUBLISH['run'](project, 'publish:modrinth-dry-run', [], False, wrapper), 0)
        tasks, env = calls[0]
        self.assertNotIn('MODRINTH_TOKEN', env)
        self.assertNotIn('CURSEFORGE_TOKEN', env)
        self.assertIn('-Ppublish_confirm=false', tasks)
        self.assertIn('--no-parallel', tasks)

    def test_debug_payload_verification_rejects_wrong_loader_or_relations(self):
        import json
        data = PUBLISH['publication_plan']()
        modrinth, curseforge = [], []
        for row in data['entries']:
            row.update(sha256='synthetic-hash', size=100)
            upload = dict(target=row['target'], path=str((ROOT / row['artifact']).resolve()), sha256=row['sha256'], size=row['size'])
            payload = dict(projectId=data['project_id'], versionNumber=row['version_number'], name=row['version_name'],
                           gameVersions=[row['minecraft']], loaders=[row['loader']], changelog=data['changelog'], versionType=row['version_type'],
                           dependencies=[dict(projectId=d['project_id'], dependencyType=d['dependency_type']) for d in row['dependencies']])
            modrinth.extend(['PUBLISH FILE ' + json.dumps(upload), 'Full data to be sent for upload: ' + json.dumps(payload), 'Not going to upload this version.'])
            cf_payload = dict(displayName=row['version_name'], releaseType=row['version_type'], changelog=data['changelog'], changelogType='markdown',
                              gameVersionNames=row['curseforge_versions'], relations=dict(projects=[dict(slug=d['curseforge_slug'], type=d['dependency_type']+'Dependency') for d in row['dependencies']]))
            curseforge.extend(['CURSEFORGE FILE ' + json.dumps(upload), f'Upload file URI for file: https://minecraft.curseforge.com/api/projects/{data["curseforge_project_id"]}/upload-file\n' + json.dumps(cf_payload)])
        with patch('builtins.print'):
            PUBLISH['verify_debug_output'](modrinth, data)
            PUBLISH['verify_curseforge_debug_output'](curseforge, data)
            with self.assertRaisesRegex(ValueError, 'loaders'):
                PUBLISH['verify_debug_output']([line.replace('"loaders": ["fabric"]', '"loaders": ["quilt"]') for line in modrinth], data)
            with self.assertRaisesRegex(ValueError, 'relation mismatch'):
                PUBLISH['verify_curseforge_debug_output']([line.replace('optionalDependency', 'requiredDependency') for line in curseforge], data)

    def test_missing_and_empty_notes_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'gradle').mkdir()
            (root / 'gradle.properties').write_text('mod_version=1.0.3\narchives_name=lootexplorer\n')
            config = (ROOT / 'gradle/publishing.properties').read_text()
            (root / 'gradle/publishing.properties').write_text(config)
            with self.assertRaisesRegex(ValueError, 'release notes missing'):
                PUBLISH['publication_plan'](root)
            (root / 'docs/wiki').mkdir(parents=True)
            (root / 'docs/wiki/release-notes.md').write_text('## 1.0.3\n\n## Next\n')
            with self.assertRaisesRegex(ValueError, 'nonempty release-notes'):
                PUBLISH['publication_plan'](root)

    def test_stale_and_ambiguous_artifacts_fail(self):
        verifier = runpy.run_path(str(ROOT / 'scripts/verify-matrix-artifacts.py'))
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'gradle.properties').write_text('mod_version=1.0.3\narchives_name=LootExplorer\n')
            matrix = root / 'gradle/matrix/1.21.1-fabric.properties'
            matrix.parent.mkdir(parents=True)
            matrix.touch()
            jars = root / 'build/libs' / matrix.stem
            jars.mkdir(parents=True)
            old = jars / 'LootExplorer-fabric-1.21.1-1.0.2.jar'
            expected = jars / 'LootExplorer-fabric-1.21.1-1.0.3.jar'
            old.touch()
            with patch.dict(verifier['release_jar'].__globals__, ROOT=root):
                with self.assertRaisesRegex(AssertionError, 'stale'):
                    verifier['release_jar'](matrix)
                expected.touch()
                with self.assertRaisesRegex(AssertionError, 'one release'):
                    verifier['release_jar'](matrix)
                old.unlink()
                (jars / 'LootExplorer-fabric-1.21.1-1.0.3-sources.jar').touch()
                self.assertEqual(verifier['release_jar'](matrix), expected)

    def test_preflight_runs_only_the_authoritative_sequence(self):
        data = PUBLISH['publication_plan']()
        project = WRAPPER['Project'](ROOT)
        steps = []
        replacements = dict(verified_plan=lambda *args: data)
        with patch.dict(PUBLISH['run'].__globals__, replacements), patch('builtins.print'):
            self.assertEqual(PUBLISH['run'](project, 'publish:preflight', [], False,
                                           dict(main=lambda args: steps.append(args[0]) or 0)), 0)
        self.assertEqual(steps, ['matrix:package', 'release-smoke', 'publish:all-dry-run'])

    def test_github_workflow_collects_verified_assets_without_platform_uploads(self):
        text = (ROOT / '.github/workflows/release.yml').read_text()
        self.assertIn("tags: ['v*']", text)
        self.assertIn('contents: write', text)
        self.assertIn('github.token', text)
        self.assertIn('Release tag does not match mod_version', text)
        self.assertIn('scripts/publish-release.py collect', text)
        self.assertIn('scripts/publish-release.py restore', text)
        self.assertIn('-Pdev_integrations=false', text)
        self.assertNotIn('MODRINTH_TOKEN', text)
        self.assertNotIn('CURSEFORGE_TOKEN', text)
        self.assertNotIn('publish:all', text)

    def test_github_bundle_restore_rejects_extra_or_changed_artifacts(self):
        import copy
        import hashlib
        data = copy.deepcopy(PUBLISH['publication_plan']())
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for row in data['entries']:
                payload = row['target'].encode()
                artifact = root / row['artifact']
                artifact.parent.mkdir(parents=True, exist_ok=True)
                artifact.write_bytes(payload)
                row.update(sha256=hashlib.sha256(payload).hexdigest(), size=len(payload))
            planned = dict(data, entries=[{k: v for k, v in r.items() if k not in ('sha256', 'size')}
                                          for r in data['entries']])
            replacements = dict(ROOT=root, verified_plan=lambda: data, publication_plan=lambda: planned)
            with patch.dict(PUBLISH['collect'].__globals__, replacements), patch.dict(os.environ, {}, clear=True), patch('builtins.print'):
                PUBLISH['collect']()
                bundle = root / 'build/publishing/github-assets'
                self.assertEqual(len(list(bundle.glob('*.jar'))), 12)
                PUBLISH['restore'](bundle)
                extra = bundle / 'unexpected-dev.jar'
                extra.touch()
                with self.assertRaisesRegex(ValueError, 'exactly the planned'):
                    PUBLISH['restore'](bundle)
                extra.unlink()
                jar = bundle / Path(data['entries'][0]['artifact']).name
                jar.write_bytes(b'changed')
                with self.assertRaisesRegex(ValueError, 'hash/size mismatch'):
                    PUBLISH['restore'](bundle)


if __name__ == '__main__':
    unittest.main()
