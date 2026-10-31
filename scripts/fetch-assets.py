#!/usr/bin/env python3
"""Fetch pinned translation graphs and Android libraries; verify all bundled dependencies."""
import argparse,hashlib,json,os,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
REVISION='c02b76706e40968d787ea4f36659bb4e63466f61'
REMOTE={
    'encoder.onnx':f'https://huggingface.co/onnx-community/opus-mt-en-trk-ONNX/resolve/{REVISION}/onnx/encoder_model_int8.onnx',
    'decoder.onnx':f'https://huggingface.co/onnx-community/opus-mt-en-trk-ONNX/resolve/{REVISION}/onnx/decoder_model_merged_int8.onnx',
    'onnxruntime-android-1.22.0.aar':'https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.22.0/onnxruntime-android-1.22.0.aar',
    'tesseract4android-4.9.0.aar':'https://jitpack.io/cz/adaptech/tesseract4android/tesseract4android/4.9.0/tesseract4android-4.9.0.aar',
}
def digest(path):
    h=hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda:stream.read(1024*1024),b''):h.update(chunk)
    return h.hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true',help='Verify without downloading');args=parser.parse_args()
    manifest=json.loads((ROOT/'docs/DEPENDENCIES.json').read_text())
    for item in manifest['files']:
        path=ROOT/item['path']
        if path.is_file() and path.stat().st_size==item['bytes'] and digest(path)==item['sha256']:
            print('OK',item['path']);continue
        if args.check or path.name not in REMOTE:raise SystemExit('Eksik veya bozuk bağımlılık: '+item['path'])
        url=REMOTE[path.name]
        path.parent.mkdir(parents=True,exist_ok=True);temporary=path.with_suffix('.download')
        try:
            print('İndiriliyor:',path.name)
            with urllib.request.urlopen(url,timeout=60) as response,temporary.open('wb') as output:
                for chunk in iter(lambda:response.read(1024*1024),b''):output.write(chunk)
            if temporary.stat().st_size!=item['bytes'] or digest(temporary)!=item['sha256']:raise SystemExit('İndirilen dosyanın özeti eşleşmedi: '+path.name)
            os.replace(temporary,path)
        finally:temporary.unlink(missing_ok=True)
    print('Tüm model ve yerel bağımlılık özetleri doğrulandı.')
if __name__=='__main__':main()
