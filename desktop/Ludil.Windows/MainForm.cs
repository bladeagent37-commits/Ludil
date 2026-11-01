using Ludil.Core;
using System.Diagnostics;
namespace Ludil.Windows;

public sealed partial class MainForm:Form {
    readonly AppSettings settings;readonly CaptionOverlay overlay=new();readonly NotifyIcon tray=new();readonly SessionGuard sessions=new();readonly object engineSync=new();readonly MenuTextTranslator menuTranslator=new();
    Rectangle region;CancellationTokenSource? cancellation,testCancellation;Task? running;Task<PipelineCheck>? testTask;Translator? translator;TvClient? tv;bool pairing;
    bool loading=true,selecting,stopping,testing,closeRequested,closed;int intervalMs=700;readonly bool uiTestMode;
    string ModelDirectory=>Path.Combine(AppContext.BaseDirectory,"models","translation");
    public MainForm(bool testMode=false){uiTestMode=testMode;settings=testMode?new AppSettings():AppSettings.Load();BuildLayout();InitializeTray();ReloadProfiles();LoadCurrentProfile();loading=false;RefreshState();
        FormClosing+=OnClosing;Shown+=(_,_)=>{if(!uiTestMode)RegisterShortcuts();};
    }
    protected override void OnHandleCreated(EventArgs e){base.OnHandleCreated(e);if(OperatingSystem.IsWindows()){var dark=1;Native.DwmSetWindowAttribute(Handle,20,ref dark,sizeof(int));}}
    void InitializeTray(){var menu=new ContextMenuStrip();menu.Items.Add("Kontrol penceresini aç",null,(_,_)=>RestoreWindow());menu.Items.Add("Çeviriyi durdur",null,(_,_)=>_=Stop());menu.Items.Add("Çıkış",null,(_,_)=>Close());tray.Icon=Icon??SystemIcons.Application;tray.Text="Ludil • Kapalı";tray.ContextMenuStrip=menu;tray.DoubleClick+=(_,_)=>RestoreWindow();tray.Visible=!uiTestMode;}
    Translator Engine(){lock(engineSync)return translator??=new Translator(ModelDirectory);}
    void Ui(Action action){if(closed||IsDisposed||!IsHandleCreated)return;try{BeginInvoke(()=>{if(!closed&&!IsDisposed)action();});}catch(InvalidOperationException){}}
    void UiSession(long ticket,Action action)=>Ui(()=>{if(sessions.IsCurrent(ticket)&&!closeRequested)action();});
    void SetStatus(string value,bool error=false){status.Text=value;status.ForeColor=error?UiTheme.Warning:UiTheme.Accent;}
    GameProfile CurrentProfile(){var p=new GameProfile{FontSize=fontSize.Value,Yellow=color.SelectedIndex==1,Original=original.Checked,Above=position.SelectedIndex==1,IntervalMs=intervalMs,MenuMode=textMode.SelectedIndex==1};p.SetRegion(region);return p;}
    void ReloadProfiles(){var before=loading;loading=true;profiles.Items.Clear();foreach(var name in settings.Profiles.Keys)profiles.Items.Add(name);profiles.SelectedItem=settings.Selected;loading=before;}
    void LoadCurrentProfile(){loading=true;var p=settings.Profiles[settings.Selected];p.Validate();region=p.Region;textMode.SelectedIndex=p.MenuMode?1:0;fontSize.Value=p.FontSize;color.SelectedIndex=p.Yellow?1:0;position.SelectedIndex=p.Above?1:0;original.Checked=p.Original;minimize.Checked=settings.MinimizeOnStart;intervalMs=p.IntervalMs;speed.SelectedIndex=intervalMs<=500?1:intervalMs>=1000?2:0;profileBadge.Text=settings.Selected;loading=false;UpdateAppearancePreview();RefreshState();}
    void ProfileSelected(){if(loading||Busy||profiles.SelectedItem is not string key)return;settings.Selected=key;LoadCurrentProfile();TrySaveSettings();SetStatus(ValidRegion()?"Profil hazır • Başlatabilirsin":"Bu oyun için altyazı alanını seç");}
    void SpeedChanged(){if(!loading&&!Busy)intervalMs=speed.SelectedIndex switch{1=>450,2=>1200,_=>700};AppearanceChanged();}
    void AppearanceChanged(){if(loading)return;if(Busy)return;settings.MinimizeOnStart=minimize.Checked;UpdateAppearancePreview();SaveCurrentProfile();}
    void UpdateAppearancePreview(){fontValue.Text=fontSize.Value.ToString();appearancePreview.Font=new Font("Segoe UI",fontSize.Value,FontStyle.Bold,GraphicsUnit.Pixel);appearancePreview.ForeColor=color.SelectedIndex==1?Color.FromArgb(255,224,138):UiTheme.Ink;}
    void SaveCurrentProfile(){settings.Profiles[settings.Selected]=CurrentProfile();TrySaveSettings();}
    bool TrySaveSettings(){if(uiTestMode)return true;try{settings.Save();return true;}catch(Exception e)when(e is IOException or UnauthorizedAccessException){SetStatus("Ayar kaydedilemedi • Yardım’a bak",true);diagnostic.Text="Tercihlerin bu oturumda geçerli. Kalıcı kayıt için yerel kullanıcı klasörüne yazma iznini kontrol et.";return false;}}
    void AddProfile(){if(Busy)return;using var dialog=new Form{Text="Yeni oyun profili",Size=new Size(440,235),FormBorderStyle=FormBorderStyle.FixedDialog,StartPosition=FormStartPosition.CenterParent,MinimizeBox=false,MaximizeBox=false,BackColor=UiTheme.Background,ForeColor=UiTheme.Ink,Font=UiTheme.Font()};
        var prompt=UiTheme.Text("Oyunun adı",12,null,true);prompt.SetBounds(22,15,370,29);var input=new TextBox{MaxLength=80,BackColor=UiTheme.SurfaceRaised,ForeColor=UiTheme.Ink,Font=UiTheme.Font(12),BorderStyle=BorderStyle.FixedSingle};input.SetBounds(22,50,375,32);
        var message=UiTheme.Text("Örneğin: Cyberpunk 2077",9.5f,UiTheme.Muted);message.SetBounds(22,87,375,24);var ok=new SoftButton{Text="Oluştur",Primary=true,DialogResult=DialogResult.None};ok.SetBounds(241,127,156,43);var cancel=new SoftButton{Text="Vazgeç",DialogResult=DialogResult.Cancel};cancel.SetBounds(22,127,145,43);
        ok.Click+=(_,_)=>{var key=input.Text.Trim();if(key.Length==0){message.Text="Bir oyun adı yaz.";return;}if(settings.Profiles.Count>=1000&&!settings.Profiles.ContainsKey(key)){message.Text="En fazla 1000 oyun profili kaydedilebilir.";return;}if(!settings.Profiles.ContainsKey(key)){var p=CurrentProfile();p.SetRegion(Rectangle.Empty);settings.Profiles[key]=p;}settings.Selected=key;dialog.DialogResult=DialogResult.OK;dialog.Close();};
        dialog.Controls.AddRange([prompt,input,message,ok,cancel]);dialog.AcceptButton=ok;dialog.CancelButton=cancel;
        if(dialog.ShowDialog(this)==DialogResult.OK){ReloadProfiles();LoadCurrentProfile();TrySaveSettings();ShowPage(0);SetStatus("Oyun eklendi • Altyazı alanını seç");}
    }
    void ModeChanged(){if(loading||Busy)return;SaveCurrentProfile();RefreshState();SetStatus(textMode.SelectedIndex==1?"Menünün yazı alanını seç • Çeviri yanında açılır":"Altyazı alanını seç");}
    bool Busy=>running!=null||testing||selecting||stopping||closeRequested||pairing;
    bool ValidRegion(){if(region.Width<80||region.Height<24||region.Width>5000)return false;var monitor=Screen.FromRectangle(region).Bounds;return monitor.Contains(region)&&(textMode.SelectedIndex==1?MenuPanelLayout.TryPlace(monitor,region,out _):region.Height<monitor.Height*.45);}
    void RefreshState(){var valid=ValidRegion();regionTitle.Text=valid?"Altyazı alanın hazır":"Henüz alan seçilmedi";regionDetail.Text=valid?$"{settings.Selected} • Alanı değiştirmedikçe hatırlanır.":"Oyundaki İngilizce yazıları fareyle işaretle.";choose.Text=valid?"Alanı değiştir":"Alanı seç";choose.Primary=!valid;choose.Invalidate();choose.Enabled=!Busy;start.Enabled=!testing&&!selecting&&!stopping&&!pairing&&!closeRequested&&(running!=null||valid);start.Primary=running==null;start.Danger=running!=null;start.Text=stopping?"Durduruluyor…":running!=null?"Çeviriyi durdur":valid?"Çeviriyi başlat":"Başlamak için önce alan seç";
        foreach(var c in new Control[]{profiles,fontSize,color,position,speed,original,minimize,test,newProfile,tvConnect,textMode})c.Enabled=!Busy;profileBadge.Text=settings.Selected;start.Invalidate();tray.Text=running!=null?"Ludil • Çeviri açık":"Ludil • Kapalı";
        tvConnect.Text=tv==null?"TV'ye bağlan":"TV bağlantısını kes";footer.Text=tv==null?"Çevrimdışı çalışır • Görüntüler kaydedilmez":"TV bağlı • Yalnızca altyazı metni ev ağına gönderilir";
    }
    async Task PickRegion(){if(testing||selecting||pairing||closeRequested)return;selecting=true;RefreshState();try{
        if(running!=null)await Stop();if(closeRequested)return;overlay.ClearCaption();WindowState=FormWindowState.Minimized;await Task.Delay(200);if(closeRequested)return;
        using var picker=new RegionPicker();var result=picker.ShowDialog(this);if(closeRequested)return;if(result==DialogResult.OK){region=picker.Selected;SaveCurrentProfile();ShowPage(0);SetStatus(ValidRegion()?"Alan seçildi • Başlatabilirsin":"Alan çok geniş • Sadece yazıları seç",!ValidRegion());}WindowState=FormWindowState.Normal;Activate();
    }finally{selecting=false;if(!closed)RefreshState();}}
    async Task Toggle(){if(stopping||testing||selecting||pairing||closeRequested)return;if(running!=null){await Stop();return;}if(!ValidRegion()){await PickRegion();return;}
        var missing=PipelineCheck.MissingFile(AppContext.BaseDirectory);if(missing!=null){ShowProblem("Uygulama dosyaları eksik. ZIP’in tamamını bir klasöre çıkartıp EXE’yi oradan aç.");return;}
        SaveCurrentProfile();var profile=CurrentProfile();cancellation=new CancellationTokenSource();var token=cancellation.Token;var ticket=sessions.Begin();SetStatus("Çeviri hazırlanıyor…");running=Task.Run(()=>ReadLoop(profile,ticket,token));RefreshState();if(minimize.Checked&&!uiTestMode){WindowState=FormWindowState.Minimized;tray.ShowBalloonTip(3500,"Ludil açık","Ctrl+Shift+F8 ile durdurabilirsin. Kontrol penceresi için simgeye çift tıkla.",ToolTipIcon.Info);}
    }
    void ReadLoop(GameProfile profile,long ticket,CancellationToken token){
        try{if(!profile.MenuMode)Engine();token.ThrowIfCancellationRequested();using var ocr=new ScreenOcr(Path.Combine(AppContext.BaseDirectory,"models","ocr"));var gate=new SubtitleGate(profile.MenuMode?2:3,profile.MenuMode?9000:4000);string lastSource="",lastTranslation="";var blanks=0;long tvSent=0;
            UiSession(ticket,()=>SetStatus("Çeviri açık • Ctrl+Shift+F8 ile durdur"));
            while(!token.IsCancellationRequested){var clock=Stopwatch.StartNew();var reading=ocr.Read(profile.Region,profile.MenuMode);token.ThrowIfCancellationRequested();var source=gate.Observe(reading.Text,reading.Confidence,Environment.TickCount64);
                if(source!=null){var started=Stopwatch.StartNew();if(profile.MenuMode)source=reading.Text;var tr=profile.MenuMode?menuTranslator.Translate(source,line=>Engine().Translate(line,token)):Engine().Translate(source,token);token.ThrowIfCancellationRequested();var latest=ocr.Read(profile.Region,profile.MenuMode);token.ThrowIfCancellationRequested();
                    if(started.ElapsedMilliseconds>(profile.MenuMode?8500:4000)||latest.Confidence<.5f||!Tokenizer.Normalize(latest.Text).Equals(Tokenizer.Normalize(source),StringComparison.OrdinalIgnoreCase)){ClearTv(token);gate.Observe(latest.Text,latest.Confidence,Environment.TickCount64);gate.Retry();lastSource=lastTranslation="";UiSession(ticket,overlay.ClearCaption);}
                    else{lastSource=source;lastTranslation=tr;blanks=0;SendTv(source,tr,token,profile.MenuMode);tvSent=Environment.TickCount64;UiSession(ticket,()=>{sourceLabel.Text=source;translationLabel.Text=tr;translationLabel.ForeColor=UiTheme.Ink;overlay.ShowCaption(source,tr,profile);});}
                }else if(reading.Confidence>=.5f&&Tokenizer.Normalize(reading.Text).Equals(Tokenizer.Normalize(lastSource),StringComparison.OrdinalIgnoreCase)&&lastTranslation.Length>0){blanks=0;UiSession(ticket,overlay.RefreshCaption);if(Environment.TickCount64-tvSent>1800){SendTv(lastSource,lastTranslation,token,profile.MenuMode);tvSent=Environment.TickCount64;}}
                else if(string.IsNullOrEmpty(reading.Text)||reading.Confidence<.5f){if(++blanks>=2){if(lastSource.Length>0)ClearTv(token);gate.Retry();lastSource=lastTranslation="";UiSession(ticket,overlay.ClearCaption);}}
                else if(lastSource.Length>0&&!Tokenizer.Normalize(reading.Text).Equals(Tokenizer.Normalize(lastSource),StringComparison.OrdinalIgnoreCase)){ClearTv(token);gate.Retry();lastSource=lastTranslation="";UiSession(ticket,overlay.ClearCaption);}
                token.WaitHandle.WaitOne(Math.Max(0,profile.IntervalMs-(int)clock.ElapsedMilliseconds));
            }
        }catch(OperationCanceledException){}catch(Exception e){UiSession(ticket,()=>ShowProblem(FriendlyError(e)));}
        finally{Ui(()=>{if(sessions.IsCurrent(ticket)&&!stopping){overlay.ClearCaption();running=null;cancellation?.Dispose();cancellation=null;RefreshState();}});}
    }
    async Task Stop(){if(stopping)return;stopping=true;sessions.Invalidate();cancellation?.Cancel();overlay.ClearCaption();RefreshState();var task=running;if(task!=null){SetStatus("Çeviri durduruluyor…");try{await task;}catch(OperationCanceledException){}}if(tv!=null)try{await tv.ClearAsync();}catch(Exception e)when(e is HttpRequestException or TaskCanceledException){}running=null;cancellation?.Dispose();cancellation=null;stopping=false;RefreshState();SetStatus("Durduruldu • Ekran okunmuyor");}
    void SendTv(string source,string text,CancellationToken token,bool menu=false){if(tv==null)return;try{(menu?tv.MenuAsync(source,text,token):tv.CaptionAsync(source,text,token)).GetAwaiter().GetResult();}catch(HttpRequestException){Ui(()=>SetStatus("PC çevirisi açık • TV bağlantısını kontrol et",true));}catch(TaskCanceledException)when(!token.IsCancellationRequested){Ui(()=>SetStatus("TV yanıt vermiyor • PC çevirisi açık",true));}}
    void ClearTv(CancellationToken token){if(tv==null)return;try{tv.ClearAsync(token).GetAwaiter().GetResult();}catch(HttpRequestException){}catch(TaskCanceledException)when(!token.IsCancellationRequested){}}
    async Task ConnectTv(){
        if(Busy)return;
        if(tv!=null){var connected=tv;tv=null;pairing=true;RefreshState();try{await connected.ClearAsync();}catch(Exception e)when(e is HttpRequestException or TaskCanceledException){}finally{connected.Dispose();pairing=false;if(!closed)RefreshState();}return;}
        using var dialog=new Form{Text="Ludil • TV'ye bağlan",ClientSize=new Size(460,310),FormBorderStyle=FormBorderStyle.FixedDialog,StartPosition=FormStartPosition.CenterParent,MaximizeBox=false,MinimizeBox=false,BackColor=UiTheme.Background,ForeColor=UiTheme.Ink,Font=UiTheme.Font()};
        var hint=UiTheme.Text("TV'de Ludil → Bağlantı → Altyazı almaya başla.\nİki cihaz aynı ev ağına bağlı olmalı.",10,UiTheme.Muted);hint.SetBounds(22,16,418,52);
        var addressLabel=UiTheme.Text("TV adresi",10);addressLabel.SetBounds(22,76,410,24);var address=new TextBox{Font=UiTheme.Font(11),PlaceholderText="http://192.168.1.50:8765"};address.SetBounds(22,104,416,32);
        var codeLabel=UiTheme.Text("6 haneli eşleştirme kodu",10);codeLabel.SetBounds(22,151,410,24);var code=new TextBox{Font=UiTheme.Font(12),MaxLength=6};code.SetBounds(22,181,416,32);
        var ok=new SoftButton{Text="Eşleştir",Primary=true};ok.SetBounds(246,242,192,46);var cancel=new SoftButton{Text="Vazgeç",DialogResult=DialogResult.Cancel};cancel.SetBounds(22,242,168,46);
        TvClient? candidate=null;ok.Click+=(_,_)=>{try{candidate=new TvClient(address.Text.Trim());if(code.Text.Length!=6||code.Text.Any(c=>c<'0'||c>'9'))throw new ArgumentException("6 haneli kodu gir.");dialog.DialogResult=DialogResult.OK;dialog.Close();}catch(ArgumentException e){candidate?.Dispose();candidate=null;hint.Text=e.Message;hint.ForeColor=UiTheme.Warning;}};
        dialog.Controls.AddRange([hint,addressLabel,address,codeLabel,code,ok,cancel]);dialog.AcceptButton=ok;dialog.CancelButton=cancel;
        if(dialog.ShowDialog(this)!=DialogResult.OK){candidate?.Dispose();return;}
        pairing=true;RefreshState();try{await candidate!.PairAsync(code.Text);if(closeRequested){candidate.Dispose();return;}tv=candidate;SetStatus("TV eşleştirildi • Çeviriyi başlatabilirsin");}
        catch(Exception e)when(e is HttpRequestException or TaskCanceledException or System.Text.Json.JsonException){candidate?.Dispose();if(!closeRequested){diagnostic.Text="TV'ye bağlanılamadı. TV adresini, yeni eşleştirme kodunu ve aynı ağa bağlı olduğunuzu kontrol et.";ShowPage(3);SetStatus("TV eşleşmedi • Yeniden deneyebilirsin",true);}}
        finally{pairing=false;if(!closed)RefreshState();}
    }
    async Task TestPipeline(){if(Busy)return;testing=true;testCancellation=new CancellationTokenSource();RefreshState();diagnostic.Text="Örnek görüntü okunup Türkçeye çevriliyor. Ekranın taranmıyor…";SetStatus("Yerel çalışma testi sürüyor…");
        try{testTask=Task.Run(()=>PipelineCheck.Run(Engine(),AppContext.BaseDirectory,testCancellation.Token));var result=await testTask;if(closeRequested)return;diagnostic.Text=$"Test başarılı. Örnek görüntüden İngilizce metin okundu ve Türkçeye çevrildi.\n\n{result.Source}\n{result.Translation}";sourceLabel.Text=result.Source;translationLabel.Text=result.Translation;translationLabel.ForeColor=UiTheme.Ink;SetStatus("Örnek görüntü testi başarılı");}
        catch(OperationCanceledException){}catch(Exception e){if(!closeRequested)ShowProblem(FriendlyError(e));}
        finally{testing=false;testCancellation?.Dispose();testCancellation=null;if(!closed)RefreshState();}
    }
    void ShowProblem(string explanation){SetStatus("Bir sorun var • Yardım’a bak",true);diagnostic.Text=explanation;ShowPage(3);if(WindowState==FormWindowState.Minimized)WindowState=FormWindowState.Normal;}
    static string FriendlyError(Exception error){var e=error.GetBaseException();return e switch{DllNotFoundException or BadImageFormatException=>"Yerel okuma bileşeni açılamadı. ZIP’in tamamını çıkart. Sorun sürerse kullanım notundaki resmi Microsoft Visual C++ x64 çalışma zamanını kur.",FileNotFoundException or DirectoryNotFoundException=>"Bir model dosyası eksik. ZIP’in tamamını bir klasöre çıkartıp uygulamayı oradan aç.",TimeoutException=>"Çeviri çok uzun sürdü. Altyazı alanını daraltıp yeniden başlat.",_=>"Görüntü veya çeviri okunamadı. Oyunu kenarlıksız moda al, yalnızca İngilizce altyazı alanını yeniden seç ve başlat."};}
    void RegisterShortcuts(){var ok=true;for(var i=1;i<=3;i++)ok&=Native.RegisterHotKey(Handle,100+i,0x0002|0x0004|0x4000,(uint)(Keys.F7+i));if(!ok)diagnostic.Text="Bazı kısayollar başka uygulamada kullanılıyor. Ana ekrandaki düğmeleri kullanabilirsin.";}
    protected override void WndProc(ref Message m){if(m.Msg==0x0312){switch(m.WParam.ToInt32()){case 101:_=Toggle();break;case 102:overlay.ToggleHidden();break;case 103:_=PickRegion();break;}return;}base.WndProc(ref m);}
    void RestoreWindow(){if(closeRequested)return;Show();WindowState=FormWindowState.Normal;Activate();}
    void OpenLicenses(){try{Process.Start(new ProcessStartInfo(Path.Combine(AppContext.BaseDirectory,"LISANSLAR")){UseShellExecute=true});}catch(Exception){diagnostic.Text="Lisans dosyaları uygulama klasöründeki LISANSLAR dizinindedir.";}}
    async void OnClosing(object? sender,FormClosingEventArgs e){if(closed)return;if(closeRequested){e.Cancel=true;return;}e.Cancel=true;closeRequested=true;Enabled=false;testCancellation?.Cancel();await Stop();if(testTask!=null){try{await testTask;}catch(Exception){}}closed=true;for(var i=1;i<=3;i++)Native.UnregisterHotKey(Handle,100+i);tray.Visible=false;tray.Dispose();overlay.Dispose();translator?.Dispose();tv?.Dispose();Close();}
}
