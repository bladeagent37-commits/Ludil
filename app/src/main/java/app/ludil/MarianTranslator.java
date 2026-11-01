package app.ludil;

import ai.onnxruntime.*;
import java.io.*;
import java.nio.*;
import java.util.*;

/** Offline OPUS-MT inference. Turkish language token is forced on every source. */
public final class MarianTranslator implements AutoCloseable {
    private final OrtEnvironment environment;
    private final OrtSession encoder,decoder;
    private final MarianTokenizer tokenizer;
    private final Map<String,String> cache=new LinkedHashMap<String,String>(128,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<String,String> e){return size()>128;}};
    public MarianTranslator(File directory)throws Exception {
        tokenizer=new MarianTokenizer(new File(directory,"tokenizer.bin"));environment=OrtEnvironment.getEnvironment();
        try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){
            options.setIntraOpNumThreads(2);options.setInterOpNumThreads(1);options.setCPUArenaAllocator(false);
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
            encoder=environment.createSession(new File(directory,"encoder.onnx").getPath(),options);
            try{decoder=environment.createSession(new File(directory,"decoder.onnx").getPath(),options);}catch(Exception e){encoder.close();throw e;}
        }
    }
    private static Map<String,OnnxTensor> encoderInputs(OnnxTensor input,OnnxTensor mask){Map<String,OnnxTensor> data=new HashMap<>();data.put("input_ids",input);data.put("attention_mask",mask);return data;}
    public synchronized String translate(String source)throws Exception {
        source=SubtitleGate.normalize(source);
        String hit=cache.get(source);if(hit!=null)return hit;
        long[] ids=tokenizer.encode(source);long[] maskValues=new long[ids.length];Arrays.fill(maskValues,1);
        List<Integer> generated=new ArrayList<>();OrtSession.Result first=null,previous=null;
        List<OnnxTensor> emptyCaches=new ArrayList<>();
        boolean ended=false;
        try(OnnxTensor input=OnnxTensor.createTensor(environment,new long[][]{ids});
            OnnxTensor mask=OnnxTensor.createTensor(environment,new long[][]{maskValues});
            OrtSession.Result encoded=encoder.run(encoderInputs(input,mask))){
            OnnxTensor hidden=(OnnxTensor)encoded.get("last_hidden_state").get();
            long next=tokenizer.pad;long started=System.nanoTime();
            for(int step=0;step<Math.min(110,ids.length*3+12);step++){
                if(Thread.currentThread().isInterrupted() || (System.nanoTime()-started)>20_000_000_000L)throw new IOException("Çeviri süresi aşıldı.");
                Map<String,OnnxTensor> feed=new HashMap<>();feed.put("encoder_attention_mask",mask);feed.put("encoder_hidden_states",hidden);
                try(OnnxTensor current=OnnxTensor.createTensor(environment,new long[][]{{next}});
                    OnnxTensor branch=OnnxTensor.createTensor(environment,new boolean[]{step>0})){
                    feed.put("input_ids",current);feed.put("use_cache_branch",branch);
                    for(String name:decoder.getInputNames())if(name.startsWith("past_key_values.")){
                        if(step==0){OnnxTensor empty=OnnxTensor.createTensor(environment,FloatBuffer.allocate(0),new long[]{1,8,0,64});emptyCaches.add(empty);feed.put(name,empty);}
                        else {String output=name.replace("past_key_values.","present.");feed.put(name,(OnnxTensor)(name.contains(".encoder.")?first:previous).get(output).get());}
                    }
                    OrtSession.Result result=decoder.run(feed);
                    if(previous!=null&&previous!=first)previous.close();previous=result;if(first==null)first=result;
                    OnnxTensor logits=(OnnxTensor)result.get("logits").get();FloatBuffer scores=logits.getFloatBuffer();int vocabulary=(int)logits.getInfo().getShape()[2];int offset=scores.limit()-vocabulary;
                    int best=0;float score=Float.NEGATIVE_INFINITY;for(int k=0;k<vocabulary;k++){float value=scores.get(offset+k);if(k!=tokenizer.pad&&value>score){best=k;score=value;}}
                    next=best;if(best==tokenizer.eos){ended=true;break;}generated.add(best);
                    if(generated.size()>=9){int n=generated.size();if(generated.subList(n-3,n).equals(generated.subList(n-6,n-3))&&generated.subList(n-3,n).equals(generated.subList(n-9,n-6)))throw new IOException("Tekrarlayan çeviri atlandı.");}
                }
            }
        }finally{if(previous!=null&&previous!=first)previous.close();if(first!=null)first.close();for(OnnxTensor t:emptyCaches)t.close();}
        if(!ended)throw new IOException("Çeviri tamamlanamadı.");String text=tokenizer.decode(generated);if(text.isEmpty())throw new IOException("Boş çeviri");cache.put(source,text);return text;
    }
    @Override public synchronized void close()throws OrtException{encoder.close();decoder.close();cache.clear();}
}
