import app.ludil.SubtitleGate;
import app.ludil.CaptureGeometry;

public final class GateRegression {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        SubtitleGate gate=new SubtitleGate();
        check(gate.observe("Stay here.",95,10000)==null,"first frame waits");
        check("Stay here.".equals(gate.observe("Stay here.",95,10600)),"stable frame emits");
        check(gate.observe("Stay here.",95,11200)==null,"duplicates suppressed");
        check(gate.isCurrent("STAY HERE.",11300),"case-insensitive current scene");
        check(gate.observe("",95,11400)==null,"blank rejected");
        check(!gate.isCurrent("Stay here.",11401),"blank invalidates scene");
        check(gate.observe("Stay here.",95,11500)==null,"reappearance waits");
        check(gate.observe("Stay here.",95,11600)!=null,"same sentence can reappear immediately");
        for(String invalid:new String[]{"ab","12 1234","x","a".repeat(601)}){
            check(gate.observe(invalid,95,11700)==null,"invalid text rejected");
            check(gate.observe(invalid,95,11800)==null,"repeated invalid text rejected");
        }
        for(int confidence:new int[]{-1,0,49,101,1000}){
            check(gate.observe("Do not move.",confidence,12000)==null,"invalid confidence rejected");
            check(gate.observe("Do not move.",confidence,12100)==null,"repeated invalid confidence rejected");
        }
        check(gate.observe("Do not move.",50,12200)==null,"boundary confidence waits");
        check(gate.observe("Do not move.",50,12300)!=null,"boundary confidence emits");
        gate.retry();check(gate.observe("Do not move.",100,12400)!=null,"failed translation may retry");
        check(!gate.isCurrent("Do not move.",17000),"stale scene rejected");
        check(gate.observe("Do not move.",100,17000)==null,"timeout resets stability");
        check(gate.observe("Do not move.",100,17500)!=null,"timeout allows new caption");
        check(SubtitleGate.normalize(null).isEmpty(),"null normalization");
        check(SubtitleGate.normalize("  We\n need\t help.  ").equals("We need help."),"whitespace normalization");
        int[] band=CaptureGeometry.band(720,58,84,1f);check(band[0]==417&&band[1]==604,"hidden overlay preserves full selected subtitle band");
        band=CaptureGeometry.band(720,58,84,.9f);check(band[1]==604,"nonoverlapping overlay leaves source intact");
        band=CaptureGeometry.band(720,58,84,.8f);check(band[1]==570,"visible overlay excluded with margin");
        boolean rejected=false;try{CaptureGeometry.band(720,58,84,.5f);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"overlap cannot create invalid crop");
        System.out.println("GATE REGRESSIONS PASS: "+checks+" checks");
    }
}
