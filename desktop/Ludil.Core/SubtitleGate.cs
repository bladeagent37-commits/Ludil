using System.Text.RegularExpressions;
namespace Ludil.Core;
public sealed class SubtitleGate {
    readonly int minimumLength;readonly long maximumSceneAge;
    public SubtitleGate(int minimumLength=3,long maximumSceneAge=4000){this.minimumLength=minimumLength;this.maximumSceneAge=maximumSceneAge;}
    string candidate="",emitted="";int count;long seen;
    public string? Observe(string raw,float confidence,long now){
        var text=Tokenizer.Normalize(raw);if(now-seen>2200){candidate="";emitted="";count=0;}
        if(text.Length<minimumLength||text.Length>600||!float.IsFinite(confidence)||confidence<.5f||confidence>1f||!Regex.IsMatch(text,"[A-Za-z]{2}")){candidate="";emitted="";count=0;return null;}
        var key=text.ToLowerInvariant();count=key==candidate?count+1:1;candidate=key;seen=now;
        if(count>=2&&key!=emitted){emitted=key;return text;}return null;
    }
    public bool IsCurrent(string text,long now)=>Tokenizer.Normalize(text).Equals(candidate,StringComparison.OrdinalIgnoreCase)&&now-seen<maximumSceneAge;
    public void Retry()=>emitted="";
}
