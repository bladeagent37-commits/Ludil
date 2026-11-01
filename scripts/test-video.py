#!/usr/bin/env python3
"""Generate synthetic videos, OCR decoded frames, and exercise the production C# core.

This is a local component test, not Windows capture, Android execution, or a real game test.
Requires ffmpeg, Tesseract 5 CLI, Pillow and the .NET SDK used by the repository.
"""
import argparse,csv,hashlib,io,json,os,shutil,subprocess,time
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[1]
WIDTH,HEIGHT,FPS=1280,720,10
SCENES={
    'subtitles':[
        (3,['We need to get out of here.']),
        (3,['Do not move.']),
        (1,[]),
        (3,['We need to get out of here.']),
        (3,['Open the gate.']),
    ],
    'menus':[
        (3,['New Game','Continue','Settings','Exit']),
        (3,['Graphics','Audio','Controls','Accessibility','Back']),
        (3,['Resolution: 1920x1080','Texture Quality: High','Mouse Sensitivity: 50','V-Sync: Off']),
        (3,['Choose your destiny.','Settings']),
    ],
    'transitions':[
        (3,['On']),
        (1,[]),
        (3,['Off']),
        (3,['Key Bindings','Invert Y Axis: Off','Apply Changes','Cancel']),
        (1,[]),
    ],
}
def find_font():
    options=[Path('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'),Path('C:/Windows/Fonts/arial.ttf'),Path('/Library/Fonts/Arial.ttf')]
    for path in options:
        if path.is_file():return str(path)
    raise SystemExit('Provide --font with a readable TrueType font.')
def scene_at(scenes,seconds):
    cursor=0
    for duration,lines in scenes:
        if seconds<cursor+duration:return lines
        cursor+=duration
    return []
def frame_image(name,seconds,font):
    image=Image.new('RGB',(WIDTH,HEIGHT),(17,23,32));draw=ImageDraw.Draw(image)
    draw.rectangle((900+int(seconds*13)%160,100,1100+int(seconds*13)%160,HEIGHT-90),fill=(27,38,50))
    draw.text((20,12),'Ludil local test - synthetic input',font=ImageFont.truetype(font,18),fill=(135,151,167))
    lines=scene_at(SCENES[name],seconds)
    if name=='subtitles':
        if lines:draw.text((WIDTH//2,548),lines[0],font=ImageFont.truetype(font,40),fill='white',anchor='mm',stroke_width=1)
    else:
        for i,line in enumerate(lines):
            y=120+i*90
            if i==int(seconds)%max(1,len(lines)):
                draw.rounded_rectangle((96,y-8,760,y+58),radius=10,fill=(255,139,120));ink=(20,22,27)
            else:ink=(245,245,245)
            draw.text((116,y),line,font=ImageFont.truetype(font,38),fill=ink)
    return image
def encode_video(path,frames):
    command=['ffmpeg','-y','-loglevel','error','-f','rawvideo','-pix_fmt','rgb24','-s',f'{WIDTH}x{HEIGHT}','-r',str(FPS),'-i','-','-an','-c:v','libx264','-preset','veryfast','-crf','22','-pix_fmt','yuv420p','-movflags','+faststart',str(path)]
    process=subprocess.Popen(command,stdin=subprocess.PIPE)
    try:
        for frame in frames:process.stdin.write(frame.tobytes())
    finally:process.stdin.close()
    if process.wait()!=0:raise RuntimeError('Video encoding failed')
def ocr(image,tessdata,menu):
    buffer=io.BytesIO();image.save(buffer,format='PNG')
    result=subprocess.run(['tesseract','stdin','stdout','--tessdata-dir',str(tessdata),'-l','eng','--psm','11' if menu else '3','-c','tessedit_create_tsv=1'],input=buffer.getvalue(),stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True)
    grouped={};confidences=[]
    for row in csv.DictReader(io.StringIO(result.stdout.decode()),delimiter='\t'):
        value=(row.get('text') or '').strip();confidence=float(row.get('conf','-1'))
        if value and confidence>=0:
            key=(row['block_num'],row['par_num'],row['line_num']);grouped.setdefault(key,[]).append(value);confidences.append(confidence)
    text='\n'.join(' '.join(words) for words in grouped.values())
    return text,(sum(confidences)/len(confidences)/100 if confidences else 0)
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--dotnet',default='dotnet');parser.add_argument('--output',type=Path,default=ROOT/'qa/video');parser.add_argument('--font');args=parser.parse_args()
    for tool in ['ffmpeg','tesseract']:
        if not shutil.which(tool):raise SystemExit('Required local executable: '+tool)
    output=args.output.resolve();output.mkdir(parents=True,exist_ok=True);font=args.font or find_font();records=[];samples=[];preprocess=[];start=time.monotonic()
    tessdata=output/'tessdata';tessdata.mkdir(exist_ok=True);shutil.copyfile(ROOT/'app/src/main/assets/eng.traineddata',tessdata/'eng.traineddata')
    for name,scenes in SCENES.items():
        frames=int(sum(s[0] for s in scenes)*FPS);path=output/f'{name}-input.mp4';encode_video(path,(frame_image(name,n/FPS,font) for n in range(frames)))
        folder=output/name;folder.mkdir(exist_ok=True)
        subprocess.run(['ffmpeg','-y','-loglevel','error','-i',str(path),'-vf','fps=2',str(folder/'frame-%04d.png')],check=True)
        for index in range(int(sum(scene[0] for scene in scenes)*2)):
            file=folder/f'frame-{index+1:04d}.png'
            seconds=index*.5;image=Image.open(file).convert('RGB');menu=name!='subtitles'
            crop=image.crop((51,43,819,677)) if menu else image.crop((64,417,1216,604))
            crop_file=folder/f'crop-{index:04d}.png';crop.save(crop_file)
            if menu:
                raw_file=folder/f'crop-{index:04d}.rgb';raw_file.write_bytes(crop.tobytes());mask=folder/f'crop-{index:04d}.pgm';preprocess.append({'input':str(raw_file),'output':str(mask),'width':crop.width,'height':crop.height});crop_file=mask
            samples.append({'clip':name,'mode':'menu' if menu else 'subtitle','time_ms':10000+round(seconds*1000),'expected_lines':scene_at(scenes,seconds),'crop':str(crop_file)})
    plan=output/'preprocess.json';plan.write_text(json.dumps(preprocess));subprocess.run([args.dotnet,'run','--project',str(ROOT/'desktop/Ludil.Smoke'),'--','--preprocess-menus',str(plan)],cwd=ROOT,check=True)
    for sample in samples:
        text,confidence=ocr(Image.open(sample.pop('crop')).convert('RGB'),tessdata,sample['mode']=='menu');records.append(dict(sample,text=text,confidence=confidence))
    source=output/'ocr-input.json';source.write_text(json.dumps(records,ensure_ascii=False,indent=2))
    translated=output/'translations.json'
    subprocess.run([args.dotnet,'run','--project',str(ROOT/'desktop/Ludil.Smoke'),'--','--video-batch',str(ROOT/'app/src/main/assets/translation'),str(source),str(translated)],cwd=ROOT,check=True)
    rows=json.loads(translated.read_text());checks=[]
    def require(value,name):
        if not value:raise AssertionError(name)
        checks.append(name)
    text_by_clip={name:'\n'.join(row['translation'] for row in rows if row['clip']==name) for name in SCENES}
    for needle in ['Yeni Oyun','Devam Et','Ayarlar','Çözünürlük: 1920x1080','Doku Kalitesi: Yüksek','Fare Hassasiyeti: 50']:
        require(needle in text_by_clip['menus'],'Real video OCR → menu translation: '+needle)
    require('kader' in text_by_clip['menus'].casefold(),'Unknown menu sentence uses real ONNX fallback')
    for needle in ['Buradan çıkmalıyız','Kıpırdama','Kapıyı aç']:
        require(needle in text_by_clip['subtitles'],'Real video OCR → ONNX subtitle: '+needle)
    require(sum(row['emitted'] and 'Buradan çıkmalıyız' in row['translation'] for row in rows if row['clip']=='subtitles')>=2,'Subtitle can reappear after a blank scene')
    require('Açık' in text_by_clip['transitions'] and 'Kapalı' in text_by_clip['transitions'],'Two-letter menu values survive OCR and stabilization')
    for source_row,row in zip(records,rows):
        if not source_row['expected_lines']:require(not row['translation'],'No caption remains in a blank decoded frame')
    for name,scenes in SCENES.items():
        clip_rows=[row for row in rows if row['clip']==name]
        def annotated():
            for n in range(int(sum(s[0] for s in scenes)*FPS)):
                image=frame_image(name,n/FPS,font);draw=ImageDraw.Draw(image);sample=min(len(clip_rows)-1,int(n/FPS*2));text=clip_rows[sample]['translation']
                if text:
                    draw.rounded_rectangle((846,64,1260,660),radius=12,fill=(32,34,40));draw.text((862,80),'TURKISH / CORE OUTPUT',font=ImageFont.truetype(font,17),fill=(255,139,120))
                    import textwrap
                    y=122
                    for line in text.splitlines():
                        for piece in textwrap.wrap(line,width=29):draw.text((862,y),piece,font=ImageFont.truetype(font,23),fill='white');y+=34
                yield image
        encode_video(output/f'{name}-translated.mp4',annotated())
    report={'version':'0.4.0','passed':True,'clips':len(SCENES),'decoded_ocr_samples':len(records),'assertions':len(checks),'checks':checks,'seconds':round(time.monotonic()-start,2),'environment':{'ocr':subprocess.check_output(['tesseract','--version'],stderr=subprocess.STDOUT,text=True).splitlines()[0],'platform':'Local host; synthetic videos + Tesseract CLI + production C# Core'},'limitations':['Not native Windows screen capture','Not Android device execution','Not a real game, TCL HDMI or PlayStation test'],'videos':[]}
    for file in sorted(output.glob('*.mp4')):
        with file.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
        report['videos'].append({'file':file.name,'bytes':file.stat().st_size,'sha256':digest})
    (output/'video-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(report,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
