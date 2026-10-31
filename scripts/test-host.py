#!/usr/bin/env python3
"""Run the real Java translation engine and LAN protocol smoke checks on a JDK 17 host."""
import os,pathlib,shutil,subprocess,tempfile,urllib.request
root=pathlib.Path(__file__).resolve().parents[1]
java_bin=pathlib.Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else None
javac=str(java_bin/'javac') if java_bin else shutil.which('javac')
java=str(java_bin/'java') if java_bin else shutil.which('java')
if not javac or not java:raise SystemExit('JDK 17 kur ve JAVA_HOME ayarla.')
with tempfile.TemporaryDirectory(prefix='lodil-host-') as temp:
    temp=pathlib.Path(temp);runtime=temp/'onnxruntime.jar';json_jar=temp/'json.jar';classes=temp/'classes';classes.mkdir()
    urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime/1.22.0/onnxruntime-1.22.0.jar',runtime)
    urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/org/json/json/20250107/json-20250107.jar',json_jar)
    src=root/'app/src/main/java/app/lodil'
    files=[src/n for n in ['MarianTokenizer.java','MarianTranslator.java','SubtitleGate.java','CaptureGeometry.java','LanReceiver.java','RuntimeState.java']]
    files += [root/'tests/host/android/os/SystemClock.java',root/'tests/java/EngineSmoke.java',root/'tests/java/ReceiverSmoke.java',root/'tests/java/GateRegression.java',root/'tests/java/ReceiverBridge.java']
    cp=os.pathsep.join(map(str,[runtime,json_jar]))
    subprocess.run([javac,'-cp',cp,'-d',str(classes),*map(str,files)],check=True)
    cp=os.pathsep.join([str(classes),cp])
    subprocess.run([java,'-cp',cp,'EngineSmoke',str(root/'app/src/main/assets/translation')],check=True)
    subprocess.run([java,'-Dsun.net.http.allowRestrictedHeaders=true','-cp',cp,'ReceiverSmoke'],check=True)
    subprocess.run([java,'-cp',cp,'GateRegression'],check=True)
    if os.environ.get('LODIL_DOTNET'):
        import json
        server=subprocess.Popen([java,'-cp',cp,'ReceiverBridge'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,text=True)
        try:
            connection=json.loads(server.stdout.readline())
            subprocess.run([os.environ['LODIL_DOTNET'],'run','--project',str(root/'desktop/Lodil.Smoke'),'--','--interop',connection['address'],connection['code']],check=True,cwd=root/'desktop')
        finally:
            server.communicate(input='x',timeout=10)
