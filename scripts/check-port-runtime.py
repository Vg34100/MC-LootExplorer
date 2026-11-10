#!/usr/bin/env python3
"""Run the opt-in native port assertions, require Done, then stop only the owned server."""
import json, os, re, runpy, sys, tempfile, uuid
from pathlib import Path
root=Path(__file__).resolve().parents[1]
api=runpy.run_path(str(root/'build-smart.py'))
project=api['Project'](root)
java, message, fatal=api['select_gradle_java'](project, [])
if fatal: raise SystemExit(message)
if len(sys.argv) < 2:
    raise SystemExit('Usage: python scripts/check-port-runtime.py <matrix-target> [...]')
(root/'build/port-validation').mkdir(parents=True,exist_ok=True)
for target in sys.argv[1:]:
    if target not in {node.name for node in project.targets}:
        raise SystemExit('Unknown matrix target: '+target)
    run=Path(tempfile.gettempdir())/'lootexplorer-port-validation'/target/uuid.uuid4().hex
    run.mkdir(parents=True)
    (run/'eula.txt').write_text('eula=true\n')
    (run/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nmax-tick-time=120000\n')
    # Wait for the actual assertions, then reuse the wrapper's owned stop/exit checks.
    api['run_server_smoke'].__globals__['SERVER_READY']=re.compile(r'LOOTEXPLORER_PORT_CHECKS_PASS:')
    api['run_server_smoke'].__globals__['show_runtime']=lambda line: None
    os.environ['BUILD_SMART_SERVER_READY_TIMEOUT']='600'
    os.environ['BUILD_SMART_SERVER_STOP_TIMEOUT']='180'
    ok, reason, code, lines=api['run_server_smoke'](project, f':{target}:runServer',
        ['-Pport_checks','-Pdev_integrations=false',f'-Pvalidation_run_dir={run}','--max-workers=2','--configure-on-demand'],java)
    done=any(re.search(r'Done \([^\n]*\)!',line) for line in lines)
    ok=ok and done
    log=root/'build/port-validation'/f'{target}-server.log'
    log.write_text('\n'.join(lines),encoding='utf-8')
    report=dict(target=target,pass_=ok,done=done,reason=reason,exit=code,runtime=str(run),log=str(log))
    (root/'build/port-validation'/f'{target}-server.json').write_text(json.dumps(report,indent=2))
    print(f'{target}: {"PASS" if ok else "FAIL"}: {reason}; Done={done}; log={log}',flush=True)
    if not ok: raise SystemExit(1)
