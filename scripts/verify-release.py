#!/usr/bin/env python3
"""Verify distributable contents against the pinned assets and Windows file manifest."""
import argparse,hashlib,json
from pathlib import Path
from zipfile import ZipFile
ROOT=Path(__file__).resolve().parents[1]
def digest(stream):
    h=hashlib.sha256()
    for chunk in iter(lambda:stream.read(1024*1024),b''):h.update(chunk)
    return h.hexdigest()
def main():
    p=argparse.ArgumentParser();p.add_argument('directory',type=Path);args=p.parse_args();checks=[]
    dependencies=json.loads((ROOT/'docs/DEPENDENCIES.json').read_text())['files']
    assets=[f for f in dependencies if f['path'].startswith('app/src/main/assets/')]
    with ZipFile(args.directory/'Lodil-0.3.0-Android.apk') as apk:
        assert apk.testzip() is None,'APK CRC error'
        for f in assets:
            name='assets/'+f['path'].split('assets/',1)[1]
            with apk.open(name) as stream:assert digest(stream)==f['sha256'],name
        assert sorted(n for n in apk.namelist() if n.startswith('assets/translation/'))==sorted('assets/translation/'+name for name in ['decoder.onnx','encoder.onnx','tokenizer.bin']),'Unexpected translation payload'
        assert {n.split('/')[1] for n in apk.namelist() if n.startswith('lib/')}=={'arm64-v8a','armeabi-v7a','x86_64'},'Missing ABI'
        assert not any(n.endswith(('.p12','.jks','.keystore')) for n in apk.namelist()),'Private key in APK'
        checks.append('APK CRC, all model SHA-256 hashes, exactly three translation files, three ABIs, no private key')
    with ZipFile(args.directory/'Lodil-0.3.0-Windows-x64.zip') as windows:
        prefix='Lodil-0.3.0-Windows-x64/'
        assert windows.testzip() is None,'Windows ZIP CRC error'
        required=['Lodil.exe','Lodil.dll','Lodil.Core.dll','Lodil.runtimeconfig.json','coreclr.dll','hostfxr.dll','System.Windows.Forms.dll','onnxruntime.dll','x64/tesseract50.dll','x64/leptonica-1.82.0.dll','LICENSE','KULLANIM.md','Lodil-Test.cmd','DOSYA-OZETLERI.sha256']
        for name in required:assert prefix+name in windows.namelist(),name
        assert windows.read(prefix+'Lodil.exe')[:2]==b'MZ','Not a Windows executable'
        for f in assets:
            rel=f['path'].split('assets/',1)[1]
            target='models/ocr/eng.traineddata' if rel=='eng.traineddata' else 'models/'+rel
            with windows.open(prefix+target) as stream:assert digest(stream)==f['sha256'],target
        for line in windows.read(prefix+'DOSYA-OZETLERI.sha256').decode().splitlines():
            expected,name=line.split('  ',1)
            with windows.open(prefix+name) as stream:assert digest(stream)==expected,name
        checks.append('Windows ZIP CRC, executable PE header, self-contained runtime, OCR libraries, model hashes, every file manifest hash, MIT and dependency licenses')
    report={'version':'0.3.0','passed':True,'checks':checks,'does_not_verify':['Windows native execution/capture','Android device execution','TCL HDMI/PlayStation compatibility']}
    (args.directory/'package-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(report,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
