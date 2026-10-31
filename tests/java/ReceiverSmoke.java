import app.lodil.*;
import org.json.JSONObject;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
public final class ReceiverSmoke {
    private static String body;
    private static int send(String path,String data,String token,boolean origin)throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:8765"+path).openConnection(Proxy.NO_PROXY);
        c.setConnectTimeout(2000);c.setReadTimeout(2000);c.setRequestMethod(data==null?"GET":"POST");
        if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
        if(origin)c.setRequestProperty("Origin","http://untrusted.invalid");
        if(data!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(var out=c.getOutputStream()){out.write(data.getBytes(StandardCharsets.UTF_8));}}
        int code=c.getResponseCode();try(var in=code<400?c.getInputStream():c.getErrorStream()){body=in==null?"":new String(in.readAllBytes(),StandardCharsets.UTF_8);}finally{c.disconnect();}return code;
    }
    private static void expect(int expected,int code){if(code!=expected)throw new AssertionError("Expected "+expected+", got "+code);}
    public static void main(String[] args)throws Exception {
        AtomicReference<String> caption=new AtomicReference<>("");AtomicReference<Integer> ttl=new AtomicReference<>(0);
        try(LanReceiver r=new LanReceiver(new LanReceiver.Listener(){public void subtitle(String en,String tr,int t){caption.set(tr);ttl.set(t);}public void clear(){caption.set("");}})){
            expect(200,send("/health",null,null,false));
            expect(403,send("/pair","{\"code\":\"wrong\"}",null,false));
            expect(200,send("/pair",new JSONObject().put("code",RuntimeState.pairingCode).toString(),null,false));
            String token=new JSONObject(body).getString("token");
            expect(403,send("/pair",new JSONObject().put("code",RuntimeState.pairingCode).toString(),null,false));
            String message=new JSONObject().put("id",1).put("sourceText","Stay here.").put("translatedText","Burada kal.").put("ttlMs",999999).toString();
            expect(401,send("/subtitle",message,"wrong",false));
            expect(403,send("/subtitle",message,token,true));
            expect(200,send("/subtitle",message,token,false));
            if(!caption.get().equals("Burada kal.")||ttl.get()!=10000)throw new AssertionError("Caption/TTL mismatch");
            expect(409,send("/subtitle",message,token,false));
            expect(422,send("/subtitle","{\"id\":2,\"translatedText\":\"\"}",token,false));
            expect(200,send("/clear","{\"id\":2}",token,false));if(!caption.get().isEmpty())throw new AssertionError("Clear failed");
            expect(413,send("/subtitle","x".repeat(8300),token,false));
            expect(404,send("/unknown","{\"id\":3}",token,false));
            expect(405,send("/subtitle",null,token,false));
            String low=new JSONObject().put("id",3).put("translatedText","Türkçe altyazı — Çığlık, ıİşŞğĞ.").put("ttlMs",-1).toString();
            expect(200,send("/subtitle",low,token,false));if(ttl.get()!=500)throw new AssertionError("Minimum TTL not enforced");
            expect(422,send("/subtitle",new JSONObject().put("id",4).put("translatedText","x".repeat(1601)).toString(),token,false));
            System.out.println("RECEIVER PASS: pairing, auth, code reuse, Origin, UTF-8, TTL bound, replay, empty text, clear, size limit");
        }
        for(int i=0;i<12;i++)try(LanReceiver r=new LanReceiver(new LanReceiver.Listener(){public void subtitle(String a,String b,int c){}public void clear(){}})){
            expect(200,send("/health",null,null,false));
        }
        System.out.println("RECEIVER LIFECYCLE PASS: 12 immediate close/restart cycles");
    }
}
