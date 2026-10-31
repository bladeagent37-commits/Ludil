using System.Diagnostics;
using Microsoft.ML.OnnxRuntime;
using Microsoft.ML.OnnxRuntime.Tensors;
namespace Lodil.Core;

public sealed class Translator:IDisposable {
    readonly InferenceSession encoder,decoder;
    readonly Tokenizer tokenizer;
    readonly Dictionary<string,string> cache=new(StringComparer.Ordinal);
    readonly Queue<string> order=new();readonly object sync=new();
    public Translator(string directory){
        tokenizer=new(Path.Combine(directory,"tokenizer.bin"));
        using var options=new SessionOptions{IntraOpNumThreads=2,InterOpNumThreads=1,EnableCpuMemArena=false,GraphOptimizationLevel=GraphOptimizationLevel.ORT_ENABLE_BASIC};
        encoder=new(Path.Combine(directory,"encoder.onnx"),options);
        try{decoder=new(Path.Combine(directory,"decoder.onnx"),options);}catch{encoder.Dispose();throw;}
    }
    static NamedOnnxValue Named<T>(string name,Tensor<T> tensor)=>NamedOnnxValue.CreateFromTensor(name,tensor);
    public string Translate(string source,CancellationToken cancellation=default){lock(sync){
        cancellation.ThrowIfCancellationRequested();source=Tokenizer.Normalize(source);if(cache.TryGetValue(source,out var hit))return hit;
        var ids=tokenizer.Encode(source);var mask=new DenseTensor<long>(Enumerable.Repeat(1L,ids.Length).ToArray(),[1,ids.Length]);
        using var encoded=encoder.Run([Named("input_ids",new DenseTensor<long>(ids,[1,ids.Length])),Named("attention_mask",mask)]);
        var hidden=encoded.First(x=>x.Name=="last_hidden_state").AsTensor<float>();
        IDisposableReadOnlyCollection<DisposableNamedOnnxValue>? first=null,previous=null;
        var generated=new List<int>();var next=tokenizer.Pad;var ended=false;var clock=Stopwatch.StartNew();
        try{for(var step=0;step<Math.Min(110,ids.Length*3+12);step++){
            cancellation.ThrowIfCancellationRequested();if(clock.Elapsed>TimeSpan.FromSeconds(20))throw new TimeoutException("Çeviri süresi aşıldı.");
            var feed=new List<NamedOnnxValue>{Named("encoder_attention_mask",mask),Named("encoder_hidden_states",hidden),Named("input_ids",new DenseTensor<long>(new long[]{next},[1,1])),Named("use_cache_branch",new DenseTensor<bool>(new[]{step>0},[1]))};
            foreach(var name in decoder.InputMetadata.Keys.Where(x=>x.StartsWith("past_key_values.",StringComparison.Ordinal))){
                var output=name.Replace("past_key_values.","present.",StringComparison.Ordinal);
                var tensor=step==0?new DenseTensor<float>([1,8,0,64]):(name.Contains(".encoder.",StringComparison.Ordinal)?first!:previous!).First(x=>x.Name==output).AsTensor<float>();
                feed.Add(Named(name,tensor));
            }
            var result=decoder.Run(feed);if(previous!=null&&!ReferenceEquals(previous,first))previous.Dispose();previous=result;first??=result;
            var logits=result.First(x=>x.Name=="logits").AsTensor<float>();var count=logits.Dimensions[2];var best=0;var value=float.NegativeInfinity;
            for(var i=0;i<count;i++){var score=logits[0,0,i];if(i!=tokenizer.Pad&&score>value){best=i;value=score;}}
            next=best;if(next==tokenizer.Eos){ended=true;break;}generated.Add(next);
            if(generated.Count>=9&&generated.TakeLast(3).SequenceEqual(generated.Skip(generated.Count-6).Take(3))&&generated.TakeLast(3).SequenceEqual(generated.Skip(generated.Count-9).Take(3)))throw new InvalidOperationException("Tekrarlayan çeviri atlandı.");
        }}finally{if(previous!=null&&!ReferenceEquals(previous,first))previous.Dispose();first?.Dispose();}
        if(!ended)throw new InvalidOperationException("Çeviri tamamlanamadı.");var text=tokenizer.Decode(generated);if(string.IsNullOrWhiteSpace(text))throw new InvalidOperationException("Okunabilir çeviri bulunamadı.");
        cache[source]=text;order.Enqueue(source);while(order.Count>160)cache.Remove(order.Dequeue());return text;
    }}
    public void Dispose(){lock(sync){encoder.Dispose();decoder.Dispose();cache.Clear();order.Clear();}}
}
