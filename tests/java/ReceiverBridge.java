import app.ludil.*;
import org.json.JSONObject;

/** Local interop test server for the production C# TV sender. */
public final class ReceiverBridge {
    public static void main(String[] args)throws Exception{
        try(LanReceiver receiver=new LanReceiver(new LanReceiver.Listener(){
            public void subtitle(String source,String text,int ttl){if(!text.contains("Türkçe"))throw new AssertionError("UTF-8 text lost");}
            public void clear(){}
        })){
            System.out.println(new JSONObject().put("address","http://127.0.0.1:8765").put("code",RuntimeState.pairingCode));System.out.flush();
            System.in.read();
        }
    }
}
