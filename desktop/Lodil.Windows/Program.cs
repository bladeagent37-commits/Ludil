namespace Lodil.Windows;
internal static class Program {
    [STAThread]static void Main(string[] args){
        ApplicationConfiguration.Initialize();
        Application.ThreadException+=(_,e)=>MessageBox.Show("İşlem tamamlanamadı. Uygulamayı durdurup yeniden aç.\n\n"+e.Exception.GetType().Name,"Lodil",MessageBoxButtons.OK,MessageBoxIcon.Error);
        try{var smoke=args.Length==2&&args[0]=="--ui-smoke";var form=new MainForm(smoke);if(smoke)form.Shown+=async(_,_)=>await form.RunUiSmoke(args[1]);Application.Run(form);}catch(Exception e){MessageBox.Show("Lodil açılamadı. ZIP dosyasının tamamını bir klasöre çıkartıp yeniden dene.\n\n"+e.GetType().Name,"Lodil",MessageBoxButtons.OK,MessageBoxIcon.Error);}
    }
}
