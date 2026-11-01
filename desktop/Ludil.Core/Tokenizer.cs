using System.Buffers.Binary;
using System.Text;
using System.Text.RegularExpressions;
namespace Ludil.Core;

public sealed class Tokenizer {
    readonly Dictionary<string,(float Score,int Id)> pieces=new(StringComparer.Ordinal);
    readonly string[] vocabulary;
    readonly int longest;
    readonly float unknown;
    public int Pad {get;}
    public int Eos {get;}
    public int Language {get;}
    public Tokenizer(string file) {
        using var input=File.OpenRead(file);using var reader=new BinaryReader(input);
        int Integer(){var b=reader.ReadBytes(4);if(b.Length!=4)throw new InvalidDataException();return BinaryPrimitives.ReadInt32BigEndian(b);}
        string Text(){var n=Integer();if(n<0||n>20000)throw new InvalidDataException();var b=reader.ReadBytes(n);if(b.Length!=n)throw new InvalidDataException();return Encoding.UTF8.GetString(b);}
        if(Integer()!=0x4D414731)throw new InvalidDataException("Sözcük modeli geçersiz.");
        Pad=Integer();Eos=Integer();Language=Integer();var size=Integer();
        if(size<1000||size>100000||Language!=39)throw new InvalidDataException("Türkçe modeli geçersiz.");
        vocabulary=new string[size];for(var i=0;i<size;i++)vocabulary[i]=Text();
        var count=Integer();var minimum=0f;for(var i=0;i<count;i++){
            var word=Text();var score=BitConverter.Int32BitsToSingle(Integer());var id=Integer();pieces[word]=(score,id);
            longest=Math.Max(longest,word.Length);minimum=Math.Min(minimum,score);
        }
        unknown=minimum-10;
    }
    public static string Normalize(string text)=>Regex.Replace(Regex.Replace(text??"",@"[\p{Cc}\p{Cf}]"," "),@"\s+"," ").Trim();
    public long[] Encode(string input){
        if(string.IsNullOrWhiteSpace(input)||input.Length>600)throw new ArgumentException("1–600 karakter arasında bir İngilizce altyazı gir.");
        var text="▁"+Normalize(input.Normalize(NormalizationForm.FormKC)).Replace(' ','▁');
        var best=new float[text.Length+1];Array.Fill(best,float.NegativeInfinity);best[0]=0;
        var previous=new int[text.Length+1];var tokens=new int[text.Length+1];
        for(var i=0;i<text.Length;i++){
            if(float.IsNegativeInfinity(best[i]))continue;
            for(var j=i+1;j<=Math.Min(text.Length,i+longest);j++)if(pieces.TryGetValue(text[i..j],out var piece)&&best[i]+piece.Score>best[j]){best[j]=best[i]+piece.Score;previous[j]=i;tokens[j]=piece.Id;}
            var end=i+(char.IsHighSurrogate(text[i])&&i+1<text.Length&&char.IsLowSurrogate(text[i+1])?2:1);
            if(best[i]+unknown>best[end]){best[end]=best[i]+unknown;previous[end]=i;tokens[end]=1;}
        }
        var ids=new List<long>();for(var i=text.Length;i>0;i=previous[i])ids.Add(tokens[i]);ids.Reverse();
        if(ids.Count>110)throw new InvalidOperationException("Altyazı çok uzun. Daha dar bir okuma bölgesi seç.");
        ids.Insert(0,Language);ids.Add(Eos);return ids.ToArray();
    }
    public string Decode(IEnumerable<int> ids)=>string.Concat(ids.Where(i=>i>1&&i<vocabulary.Length&&i!=Pad&&i!=Language).Select(i=>vocabulary[i])).Replace('▁',' ').Trim();
}
