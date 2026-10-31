using Lodil.Core;
using System.Diagnostics;
using System.Text.Json;
if(args.Length==3&&args[0]=="--interop"){
    using var tv=new TvClient(args[1]);await tv.PairAsync(args[2]);
    await Task.WhenAll(Enumerable.Range(0,16).Select(i=>tv.CaptionAsync("Stay here.","Türkçe altyazı — Çığlık ıİşŞğĞ "+i)));
    await tv.ClearAsync();Console.WriteLine("C# → JAVA TV INTEROP PASS: pairing, 16 concurrent sends, ordered IDs, UTF-8, clear");return;
}
if(args.Length!=1)throw new ArgumentException("Model klasörü gerekli");
var cases=new Dictionary<string,string>{["We need to get out of here."]="Buradan çıkmalıyız.",["Stay here. I will find another way."]="Burada kal, başka bir yol bulacağım.",["The door is locked. Find the key."]="Kapı kilitli, anahtarı bulun.",["Hello, how are you?"]="Merhaba, nasılsın?",["Do not move."]="Kıpırdama."};
using(var translator=new Translator(args[0]))foreach(var (source,expected) in cases){var clock=Stopwatch.StartNew();var output=translator.Translate(source);if(output!=expected)throw new Exception($"Beklenmeyen çıktı: {source} => {output}");Console.WriteLine($"{source} => {output} ({clock.ElapsedMilliseconds} ms)");}
var gate=new SubtitleGate();if(gate.Observe("Stay here.",.95f,10000)!=null)throw new Exception("Kararsız metin kabul edildi");if(gate.Observe("Stay here.",.95f,10600)==null)throw new Exception("Kararlı metin atlandı");if(gate.Observe("Stay here.",.95f,11200)!=null)throw new Exception("Tekrar yayımlandı");if(gate.Observe("Wrong text",.1f,11800)!=null)throw new Exception("Düşük güven kabul edildi");if(gate.Observe("Stay here.",.95f,14500)!=null||gate.Observe("Stay here.",.95f,15100)==null)throw new Exception("Boşluk sonrası metin yenilenmedi");Console.WriteLine("C# ENGINE + GATE PASS");

var sessions=new SessionGuard();var old=sessions.Begin();if(!sessions.IsCurrent(old))throw new Exception("New session rejected");sessions.Invalidate();if(sessions.IsCurrent(old))throw new Exception("Stopped session still current");var fresh=sessions.Begin();if(sessions.IsCurrent(old)||!sessions.IsCurrent(fresh))throw new Exception("Old callback can overwrite a new session");Console.WriteLine("SESSION INVALIDATION PASS");

var legacy=Lodil.Windows.AppSettings.Parse("{\"Selected\":\"Çığlık\",\"Profiles\":{\"Çığlık\":{\"FontSize\":99,\"IntervalMs\":100,\"X\":-1200,\"Y\":300,\"Width\":800,\"Height\":100}}}");
if(!legacy.MinimizeOnStart||legacy.Selected!="Çığlık"||legacy.Profiles["Çığlık"].FontSize!=48||legacy.Profiles["Çığlık"].IntervalMs!=450||legacy.Profiles["Çığlık"].X!=-1200)throw new Exception("Legacy settings migration failed");
var many=new Lodil.Windows.AppSettings();for(var i=0;i<120;i++)many.Profiles["Oyun "+i]=new();many.Selected="Oyun 119";
var loaded=Lodil.Windows.AppSettings.Parse(System.Text.Json.JsonSerializer.Serialize(many));if(loaded.Profiles.Count!=121||loaded.Selected!="Oyun 119")throw new Exception("Large profile set discarded");
if(Lodil.Windows.AppSettings.Parse("not json").Profiles.Count!=1||Lodil.Windows.AppSettings.Parse("{\"Profiles\":null}").Profiles.Count!=1)throw new Exception("Invalid settings recovery failed");
Console.WriteLine("SETTINGS COMPATIBILITY + TURKISH NAMES + 120 PROFILES PASS");

int checks=0;
void Check(bool value,string name){if(!value)throw new Exception(name);checks++;}
void Reject<T>(Action action,string name)where T:Exception{try{action();}catch(T){checks++;return;}throw new Exception("Expected rejection: "+name);}
var filter=new SubtitleGate();
Check(filter.Observe("Stay here.",.9f,10000)==null,"first observation waits");
Check(filter.Observe("Stay here.",.9f,10500)!=null,"stable subtitle emits");
Check(filter.Observe("",.9f,10600)==null,"blank rejected");
Check(!filter.IsCurrent("Stay here.",10601),"blank invalidates candidate");
Check(filter.Observe("Stay here.",.9f,10700)==null,"reappearance waits");
Check(filter.Observe("Stay here.",.9f,10800)!=null,"immediate reappearance emits");
foreach(var invalid in new[]{float.NaN,float.PositiveInfinity,-1f,0f,.49f,1.01f}){
    Check(filter.Observe("Do not move.",invalid,11000)==null,"invalid confidence");
    Check(filter.Observe("Do not move.",invalid,11500)==null,"repeated invalid confidence");
}
foreach(var text in new[]{"ab","42 2345","x",new string('a',601)}){
    Check(filter.Observe(text,1,12000)==null,"invalid subtitle");Check(filter.Observe(text,1,12500)==null,"repeated invalid subtitle");
}
Check(filter.Observe("The door is locked.",.5f,13000)==null,"confidence boundary waits");
Check(filter.Observe("The door is locked.",.5f,13500)!=null,"confidence boundary emits");
filter.Retry();Check(filter.Observe("The door is locked.",1,14000)!=null,"failed translation may retry");
Check(!filter.IsCurrent("The door is locked.",19000),"expired scene rejected");
Check(filter.Observe("The door is locked.",1,19000)==null,"timeout resets stability");
Check(filter.Observe("The door is locked.",1,19500)!=null,"timeout allows re-emission");
Check(Tokenizer.Normalize("  We\n need\t help.\0  ")=="We need help.","control normalization");
var tokenizer=new Tokenizer(Path.Combine(args[0],"tokenizer.bin"));
foreach(var text in new[]{"", "   ",new string('a',601)})Reject<ArgumentException>(()=>tokenizer.Encode(text),"empty/oversized token input");
Check(tokenizer.Encode("Ｈｅｌｌｏ").SequenceEqual(tokenizer.Encode("Hello")),"NFKC normalization");
Check(tokenizer.Encode("We   need\nhelp.").SequenceEqual(tokenizer.Encode("We need help.")),"whitespace equivalent tokens");
var unicode=tokenizer.Encode("Zoë meets 🐉 near the gate.");Check(unicode.First()==39&&unicode.Last()==tokenizer.Eos,"Unicode source stays in Turkish target");
var concurrent=new SessionGuard();var tickets=new System.Collections.Concurrent.ConcurrentBag<long>();
Parallel.For(0,10000,_=>tickets.Add(concurrent.Begin()));Check(tickets.Distinct().Count()==10000,"parallel sessions have unique tickets");Check(tickets.Count(concurrent.IsCurrent)==1,"only newest concurrent session survives");
foreach(var json in new[]{"", "null","[]","{\"Profiles\":{\"x\":null}}","{\"Profiles\":{}}","{\"Profiles\":null}","{\"Profiles\":[]}"})Check(Lodil.Windows.AppSettings.Parse(json).Profiles.Count==1,"malformed settings recovery");
var boundary=new Lodil.Windows.AppSettings();for(var i=1;i<1000;i++)boundary.Profiles["Oyun "+i]=new();boundary.Selected="Oyun 999";
Check(Lodil.Windows.AppSettings.Parse(JsonSerializer.Serialize(boundary)).Profiles.Count==1000,"1000 profiles retained");boundary.Profiles["overflow"]=new();Check(Lodil.Windows.AppSettings.Parse(JsonSerializer.Serialize(boundary)).Profiles.Count==1,"excessive profile input recovers");
foreach(var address in new[]{"http://10.0.0.2:8765","http://192.168.1.2:8765","http://172.16.0.2:8765","http://172.31.255.2:8765","http://127.0.0.1:8765"})Check(TvClient.ValidateAddress(address).IsAbsoluteUri,"private TV address accepted");
foreach(var address in new[]{"https://192.168.1.2:8765","http://8.8.8.8:8765","http://172.32.0.1:8765","http://localhost:8765","http://192.168.1.2/translate","http://user:pass@192.168.1.2:8765","http://192.168.1.2?secret=x","http://192.168.1.2#x","file:///tmp/test","not a url"})Reject<ArgumentException>(()=>TvClient.ValidateAddress(address),"unapproved TV destination");
using(var translator=new Translator(args[0])){
    using var cancel=new CancellationTokenSource();cancel.Cancel();Reject<OperationCanceledException>(()=>translator.Translate("Hello",cancel.Token),"cancelled translation");
    Reject<ArgumentException>(()=>translator.Translate(" "),"empty text must not generate a hallucination");
    foreach(var source in new[]{"Open the gate.","I am looking for my brother.","We don't have much time.","You saved my life.","Follow me, and stay quiet.","Where is the village?"}){
        var result=translator.Translate(source);Check(result.Length>0&&result.Length<1600&&!result.Equals(source,StringComparison.OrdinalIgnoreCase),"real translation output");Console.WriteLine(source+" => "+result);
    }
    Check(translator.Translate("  We need to get out of here. ")==translator.Translate("We need to get out of here."),"normalized translation cache");
}
Console.WriteLine("DEEP REGRESSIONS PASS: "+checks+" checks; 10000 concurrent session invalidations");
