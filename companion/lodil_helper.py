#!/usr/bin/env python3
"""Lodil: optional local video/screen OCR, offline EN->TR, paired TV delivery."""
from __future__ import annotations
import argparse, collections, hmac, http.server, ipaddress, json, re, secrets, socket, struct, sys, threading, time
import urllib.parse, urllib.request
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
MODEL_DIR=ROOT/'app/src/main/assets/translation'

def normalize(text): return re.sub(r'\s+',' ',text or '').strip()

class Tokenizer:
    def __init__(self, path):
        with open(path,'rb') as f:
            def integer():return struct.unpack('>i',f.read(4))[0]
            def string():return f.read(integer()).decode('utf-8')
            if integer()!=0x4D414731:raise ValueError('Tokenizer biçimi geçersiz')
            self.pad,self.eos,self.language=integer(),integer(),integer()
            self.vocabulary=[string() for _ in range(integer())]
            self.pieces={}
            for _ in range(integer()):
                piece=string();score=struct.unpack('>f',f.read(4))[0];idx=integer();self.pieces[piece]=(score,idx)
            self.longest=max(map(len,self.pieces));self.unknown=min(s for s,_ in self.pieces.values())-10
    def encode(self,text):
        import unicodedata
        text='▁'+normalize(unicodedata.normalize('NFKC',text)).replace(' ','▁');length=len(text)
        best=[-float('inf')]*(length+1);best[0]=0;previous=[0]*(length+1);tokens=[1]*(length+1)
        for i in range(length):
            for j in range(i+1,min(length,i+self.longest)+1):
                p=self.pieces.get(text[i:j])
                if p and best[i]+p[0]>best[j]:best[j]=best[i]+p[0];previous[j]=i;tokens[j]=p[1]
            if best[i]+self.unknown>best[i+1]:best[i+1]=best[i]+self.unknown;previous[i+1]=i;tokens[i+1]=1
        ids=[];i=length
        while i>0:ids.append(tokens[i]);i=previous[i]
        if len(ids)>110:raise ValueError('Altyazı çok uzun')
        return [self.language]+ids[::-1]+[self.eos]
    def decode(self,tokens):return ''.join(self.vocabulary[i] for i in tokens if i not in (0,1,self.pad,self.language)).replace('▁',' ').strip()

class Translator:
    def __init__(self,directory=MODEL_DIR):
        import onnxruntime as ort
        directory=Path(directory)
        self.tokenizer=Tokenizer(directory/'tokenizer.bin')
        opts=ort.SessionOptions();opts.intra_op_num_threads=2;opts.inter_op_num_threads=1
        self.encoder=ort.InferenceSession(str(directory/'encoder.onnx'),opts,providers=['CPUExecutionProvider'])
        self.decoder=ort.InferenceSession(str(directory/'decoder.onnx'),opts,providers=['CPUExecutionProvider'])
        self.names=[x.name for x in self.decoder.get_outputs()];self.lock=threading.Lock();self.cache=collections.OrderedDict()
    def translate(self,text):
        import numpy as np
        text=normalize(text)
        with self.lock:
            if text in self.cache:self.cache.move_to_end(text);return self.cache[text]
            ids=self.tokenizer.encode(text);mask=np.ones((1,len(ids)),np.int64)
            hidden=self.encoder.run(None,{'input_ids':np.array([ids],np.int64),'attention_mask':mask})[0]
            first=previous=None;token=self.tokenizer.pad;generated=[];started=time.monotonic()
            for step in range(min(110,len(ids)*3+12)):
                feed={'encoder_hidden_states':hidden,'encoder_attention_mask':mask,'input_ids':np.array([[token]],np.int64),'use_cache_branch':np.array([step>0],bool)}
                for item in self.decoder.get_inputs():
                    name=item.name
                    if name.startswith('past_key_values.'):
                        key=name.replace('past_key_values.','present.')
                        feed[name]=np.empty((1,8,0,64),np.float32) if step==0 else (first if '.encoder.' in name else previous)[key]
                previous=dict(zip(self.names,self.decoder.run(None,feed)))
                if first is None:first=previous
                scores=previous['logits'][0,-1].copy();scores[self.tokenizer.pad]=-np.inf;token=int(scores.argmax())
                if token==self.tokenizer.eos:break
                generated.append(token)
                if time.monotonic()-started>20:raise RuntimeError('Çeviri süresi aşıldı')
                if len(generated)>=9 and generated[-3:]==generated[-6:-3]==generated[-9:-6]:raise RuntimeError('Tekrarlayan çeviri atlandı')
            else:raise RuntimeError('Çeviri tamamlanmadı')
            result=self.tokenizer.decode(generated)
            if not result:raise RuntimeError('Boş çeviri')
            self.cache[text]=result
            while len(self.cache)>160:self.cache.popitem(last=False)
            return result

class OCR:
    def __init__(self):
        from rapidocr_onnxruntime import RapidOCR
        self.engine=RapidOCR(intra_op_num_threads=2,inter_op_num_threads=1)
    def read(self,image):
        result,_=self.engine(image,use_cls=False)
        if not result:return ''
        valid=[r for r in result if float(r[2])>=.60 and re.search(r'[A-Za-z]{2}',r[1])]
        valid.sort(key=lambda r:(min(point[1] for point in r[0])//18,min(point[0] for point in r[0])))
        return normalize(' '.join(r[1] for r in valid))

class TV:
    def __init__(self,url,code):
        parsed=urllib.parse.urlparse(url)
        if parsed.scheme!='http' or parsed.username or parsed.query or parsed.fragment or parsed.path not in ('','/'):
            raise ValueError('TV adresini http://192.168.1.50:8765 biçiminde gir')
        for row in socket.getaddrinfo(parsed.hostname,parsed.port or 80,type=socket.SOCK_STREAM):
            ip=ipaddress.ip_address(row[4][0])
            if not(ip.is_private or ip.is_loopback or ip.is_link_local):raise ValueError('TV adresi ev ağına ait olmalı')
        self.url=url.rstrip('/');self.token=None;self.sequence=0
        self.opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
        response=self.request('/pair',{'code':code});self.token=response['token']
    def request(self,path,data):
        headers={'Content-Type':'application/json'}
        if self.token:headers['Authorization']='Bearer '+self.token
        request=urllib.request.Request(self.url+path,json.dumps(data,ensure_ascii=False).encode(),headers,method='POST')
        with self.opener.open(request,timeout=4) as response:return json.load(response)
    def subtitle(self,source,translation,ttl=4500):
        self.sequence+=1;return self.request('/subtitle',{'id':self.sequence,'sourceText':source,'translatedText':translation,'ttlMs':ttl})
    def clear(self):self.sequence+=1;return self.request('/clear',{'id':self.sequence})

class Gate:
    def __init__(self):self.candidate='';self.count=0;self.emitted='';self.last_seen=0
    def observe(self,text,now):
        text=normalize(text);key=text.casefold()
        if now-self.last_seen>2.2:self.candidate='';self.count=0;self.emitted=''
        if len(text)<3 or len(text)>600:self.candidate='';self.count=0;return None
        self.count=self.count+1 if key==self.candidate else 1;self.candidate=key;self.last_seen=now
        if self.count>=2 and key!=self.emitted:self.emitted=key;return text
        return None

def choose_region():
    import tkinter as tk
    root=tk.Tk();root.attributes('-fullscreen',True);root.attributes('-topmost',True);root.attributes('-alpha',.45)
    root.configure(bg='#102030');canvas=tk.Canvas(root,bg='#102030',highlightthickness=0);canvas.pack(fill='both',expand=True)
    canvas.create_text(30,35,anchor='nw',text='İngilizce altyazı bölgesini fareyle çiz. Vazgeç: Esc',fill='white',font=('Arial',20))
    state={}
    def down(e):state['x']=e.x_root;state['y']=e.y_root;state['id']=canvas.create_rectangle(e.x,e.y,e.x,e.y,outline='#64DDD0',width=4)
    def move(e):
        if 'id' in state:canvas.coords(state['id'],state['x']-root.winfo_rootx(),state['y']-root.winfo_rooty(),e.x,e.y)
    def up(e):
        if 'x' not in state:return
        x,y=state['x'],state['y'];state['region']=(min(x,e.x_root),min(y,e.y_root),abs(e.x_root-x),abs(e.y_root-y));root.destroy()
    canvas.bind('<ButtonPress-1>',down);canvas.bind('<B1-Motion>',move);canvas.bind('<ButtonRelease-1>',up);root.bind('<Escape>',lambda e:root.destroy());root.mainloop()
    region=state.get('region')
    if not region or region[2]<30 or region[3]<20:raise ValueError('Geçerli bir altyazı bölgesi seçilmedi')
    return region

def serve(translator,port):
    token=secrets.token_hex(8);limit=threading.BoundedSemaphore(2)
    class Handler(http.server.BaseHTTPRequestHandler):
        def log_message(self,*args):pass
        def setup(self):super().setup();self.connection.settimeout(4)
        def do_POST(self):
            address=ipaddress.ip_address(self.client_address[0])
            if not address.is_private or self.headers.get('Origin') or not hmac.compare_digest(self.headers.get('Authorization',''),'Bearer '+token):self.send_error(403);return
            if self.path!='/translate' or self.headers.get('Content-Type','').split(';')[0]!='application/json':self.send_error(415);return
            if not limit.acquire(blocking=False):self.send_error(429);return
            try:
                size=int(self.headers.get('Content-Length','0'))
                if not 0<size<=8192:raise ValueError()
                body=json.loads(self.rfile.read(size));text=body.get('q','')
                if body.get('source')!='en' or body.get('target')!='tr' or not isinstance(text,str) or not 1<=len(text)<=600:raise ValueError()
                output=json.dumps({'translatedText':translator.translate(text)},ensure_ascii=False).encode();self.send_response(200);self.send_header('Content-Type','application/json; charset=utf-8');self.send_header('Content-Length',str(len(output)));self.send_header('Cache-Control','no-store');self.end_headers();self.wfile.write(output)
            except Exception:self.send_error(422)
            finally:limit.release()
    print('Yerel EN → TR sunucusu, port:',port,'\nTV’ye gireceğin geçici anahtar:',token,'\nAynı ağdaki bilgisayarının IPv4 adresini kullan. Çıkış: Ctrl+C',flush=True)
    server=http.server.ThreadingHTTPServer(('0.0.0.0',port),Handler);server.daemon_threads=True
    try:server.serve_forever()
    finally:server.server_close()

def main():
    parser=argparse.ArgumentParser(description='Lodil • yerel İngilizce → Türkçe yardımcı')
    parser.add_argument('mode',choices=['text','image','video','screen','server'])
    parser.add_argument('--text');parser.add_argument('--file',type=Path);parser.add_argument('--models',type=Path,default=MODEL_DIR)
    parser.add_argument('--tv');parser.add_argument('--code');parser.add_argument('--region',help='x,y,genişlik,yükseklik');parser.add_argument('--interval',type=float,default=.6);parser.add_argument('--port',type=int,default=8766)
    args=parser.parse_args();translator=Translator(args.models)
    tv=TV(args.tv,args.code or input('TV’deki 6 haneli eşleştirme kodu: ').strip()) if args.tv else None
    def output(source):
        translated=translator.translate(source);print(json.dumps({'sourceText':source,'translatedText':translated},ensure_ascii=False),flush=True)
        if tv:tv.subtitle(source,translated)
        return translated
    if args.mode=='server':serve(translator,args.port);return
    if args.mode=='text':output(args.text or input('İngilizce altyazı: '));return
    ocr=OCR()
    if args.mode=='image':
        from PIL import Image
        import numpy as np
        if not args.file:parser.error('image için --file gerekli')
        text=ocr.read(np.array(Image.open(args.file).convert('RGB'))[:,:,::-1])
        if not text:raise ValueError('Okunabilir İngilizce metin bulunamadı')
        output(text);return
    gate=Gate();last_key='';last_translation='';last_send=0;failures=0
    def frame(image):
        nonlocal last_key,last_translation,last_send,failures
        text=ocr.read(image);now=time.monotonic();source=gate.observe(text,now)
        if source:
            started=time.monotonic();result=translator.translate(source)
            if time.monotonic()-started>4:print('Geciken altyazı atlandı.',flush=True);return
            last_key=normalize(source).casefold();last_translation=result
            print(json.dumps({'sourceText':source,'translatedText':result},ensure_ascii=False),flush=True)
            if tv:
                try:tv.subtitle(source,result);last_send=now;failures=0
                except OSError:failures+=1;print('TV bağlantısı koptu; tekrar deneniyor.',flush=True)
        elif tv and text and text.casefold()==last_key and now-last_send>1.5:
            try:tv.subtitle(text,last_translation);last_send=now;failures=0
            except OSError:failures+=1
        elif tv and not text and last_key:
            try:tv.clear()
            except OSError:failures+=1
            last_key='';last_translation=''
        if failures>=5:raise ConnectionError('TV bağlantısı kurulamadı. Yardımcıyı yeniden eşleştir.')
    try:
        if args.mode=='video':
            import cv2
            if not args.file:parser.error('video için --file gerekli')
            cap=cv2.VideoCapture(str(args.file));fps=cap.get(cv2.CAP_PROP_FPS) or 25;step=max(1,int(fps*max(.2,args.interval)));index=0
            try:
                while True:
                    ok,image=cap.read()
                    if not ok:break
                    if index%step==0:
                        if args.region:x,y,w,h=map(int,args.region.split(','));image=image[y:y+h,x:x+w]
                        else:image=image[int(image.shape[0]*.55):int(image.shape[0]*.85)]
                        frame(image)
                    index+=1
            finally:cap.release()
        else:
            import mss,numpy as np
            x,y,w,h=map(int,args.region.split(',')) if args.region else choose_region()
            if w<30 or h<20:raise ValueError('Okuma bölgesi çok küçük')
            with mss.mss() as screen:
                print('İngilizce altyazı bölgesi okunuyor. Çıkış: Ctrl+C',flush=True)
                while True:
                    begin=time.monotonic();frame(np.array(screen.grab({'left':x,'top':y,'width':w,'height':h}))[:,:,:3]);time.sleep(max(0,args.interval-(time.monotonic()-begin)))
    finally:
        if tv:
            try:tv.clear()
            except OSError:pass

if __name__=='__main__':
    try:main()
    except KeyboardInterrupt:print('\nDurduruldu.')
    except Exception as exc:print('İşlem tamamlanamadı:',str(exc),file=sys.stderr);sys.exit(1)
