import app.ludil.*;
import java.io.File;
import java.util.Arrays;
public final class EngineSmoke {
    public static void main(String[] args)throws Exception {
        MarianTokenizer tokenizer=new MarianTokenizer(new File(args[0],"tokenizer.bin"));
        String[] samples={"We need to get out of here.","Stay here. I will find another way.","The door is locked. Find the key.","Hello, how are you?","Do not move.","We have to leave before it is too late."};
        try(MarianTranslator t=new MarianTranslator(new File(args[0]))){for(String s:samples){long start=System.nanoTime();System.out.println(s+" => "+t.translate(s)+" ["+(System.nanoTime()-start)/1000000+" ms]");System.out.println("TOKENS "+Arrays.toString(tokenizer.encode(s)));}}
        SubtitleGate gate=new SubtitleGate();if(gate.observe("Hello world.",90,10000)!=null)throw new AssertionError("Unstable text emitted");if(gate.observe("Hello world.",90,10600)==null)throw new AssertionError("Stable text missing");if(gate.observe("Hello world.",90,11200)!=null)throw new AssertionError("Duplicate emitted");if(gate.observe("Bad text",20,11800)!=null)throw new AssertionError("Low confidence accepted");
        System.out.println("GATE PASS");
    }
}
