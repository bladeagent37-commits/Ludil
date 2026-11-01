using System.Drawing;
namespace Ludil.Core;

/// <summary>Places menu translations beside the OCR region, avoiding capture feedback.</summary>
public static class MenuPanelLayout {
    public static bool TryPlace(Rectangle screen,Rectangle source,out Rectangle panel){
        panel=Rectangle.Empty;if(source.Width<80||source.Height<24||!screen.Contains(source))return false;
        var left=source.Left-screen.Left-24;var right=screen.Right-source.Right-24;
        var available=Math.Max(left,right);if(available<180)return false;
        var width=Math.Min(440,available);var x=right>=left?source.Right+12:source.Left-width-12;
        panel=new Rectangle(x,screen.Top+24,width,Math.Max(120,screen.Height-48));
        return screen.Contains(panel)&&!panel.IntersectsWith(source);
    }
}
