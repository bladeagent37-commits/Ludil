using System.Net;
using System.Net.Http.Headers;
using System.Text.Json;
namespace Lodil.Core;

/// <summary>Ephemeral, explicitly paired TV connection. Captures and credentials are never stored.</summary>
public sealed class TvClient:IDisposable {
    readonly HttpClient http;
    readonly SemaphoreSlim sendLock=new(1,1);
    string token="";long sequence;
    public Uri Address{get;}
    public static Uri ValidateAddress(string text){
        if(!Uri.TryCreate(text,UriKind.Absolute,out var uri)||uri.Scheme!="http"||uri.UserInfo.Length!=0||uri.AbsolutePath!="/"||uri.Query.Length!=0||uri.Fragment.Length!=0||!IPAddress.TryParse(uri.Host,out var ip)||ip.AddressFamily!=System.Net.Sockets.AddressFamily.InterNetwork)
            throw new ArgumentException("TV ekranındaki http://192.168.1.50:8765 adresini gir.");
        var b=ip.GetAddressBytes();
        if(!(b[0]==10||b[0]==127||b[0]==192&&b[1]==168||b[0]==172&&b[1]>=16&&b[1]<=31||b[0]==169&&b[1]==254))throw new ArgumentException("Yalnızca ev ağındaki TV adresi kullanılabilir.");
        return uri;
    }
    public TvClient(string address){Address=ValidateAddress(address);http=new(new HttpClientHandler{UseProxy=false,AllowAutoRedirect=false}){BaseAddress=Address,Timeout=TimeSpan.FromSeconds(3),MaxResponseContentBufferSize=8192};}
    public async Task PairAsync(string code,CancellationToken cancellation=default){
        token="";
        if(code.Length!=6||code.Any(c=>c<'0'||c>'9'))throw new ArgumentException("TV ekranındaki 6 haneli kodu gir.");
        using var content=Content(new{code});
        using var response=await http.PostAsync("/pair",content,cancellation);
        if(!response.IsSuccessStatusCode)throw new HttpRequestException("Eşleştirme olmadı. TV'de yeni kod oluşturup yeniden dene.",null,response.StatusCode);
        await using var stream=await response.Content.ReadAsStreamAsync(cancellation);
        using var data=await JsonDocument.ParseAsync(stream,cancellationToken:cancellation);
        if(data.RootElement.ValueKind!=JsonValueKind.Object||!data.RootElement.TryGetProperty("token",out var value)||value.ValueKind!=JsonValueKind.String)throw new HttpRequestException("TV geçerli bir oturum açmadı.");
        token=value.GetString()??"";
        if(token.Length!=64||token.Any(c=>!Uri.IsHexDigit(c))){token="";throw new HttpRequestException("TV geçerli bir oturum açmadı.");}
    }
    public Task CaptionAsync(string source,string translation,CancellationToken cancellation=default)=>SendAsync("/subtitle",source,translation,cancellation);
    public Task ClearAsync(CancellationToken cancellation=default)=>SendAsync("/clear","","",cancellation);
    async Task SendAsync(string path,string source,string translation,CancellationToken cancellation){
        if(token.Length==0)throw new InvalidOperationException("Önce TV ile eşleştir.");
        await sendLock.WaitAsync(cancellation);
        try{
            using var request=new HttpRequestMessage(HttpMethod.Post,path);
            request.Headers.Authorization=new AuthenticationHeaderValue("Bearer",token);
            request.Content=Content(new{id=++sequence,sourceText=source,translatedText=translation,ttlMs=5000});
            using var response=await http.SendAsync(request,HttpCompletionOption.ResponseHeadersRead,cancellation);
            response.EnsureSuccessStatusCode();
        }finally{sendLock.Release();}
    }
    static ByteArrayContent Content<T>(T value){var content=new ByteArrayContent(JsonSerializer.SerializeToUtf8Bytes(value));content.Headers.ContentType=new MediaTypeHeaderValue("application/json"){CharSet="utf-8"};return content;}
    public void Dispose(){token="";http.Dispose();sendLock.Dispose();}
}
