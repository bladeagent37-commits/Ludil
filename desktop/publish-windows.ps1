param([string]$Output=(Join-Path $PSScriptRoot "dist"))
$ErrorActionPreference="Stop"
$Project=Join-Path $PSScriptRoot "Ludil.Windows/Ludil.Windows.csproj"
dotnet publish $Project -c Release -r win-x64 --self-contained true -p:RestoreLockedMode=true -o $Output
if($LASTEXITCODE -ne 0){throw "Windows derlemesi tamamlanamadi."}
$AssetRoot=Join-Path $PSScriptRoot "../app/src/main/assets"
New-Item -ItemType Directory -Force -Path (Join-Path $Output "models/translation"),(Join-Path $Output "models/ocr"),(Join-Path $Output "LISANSLAR") | Out-Null
foreach($Name in @("encoder.onnx","decoder.onnx","tokenizer.bin")){Copy-Item (Join-Path $AssetRoot "translation/$Name") (Join-Path $Output "models/translation/$Name") -Force}
Copy-Item (Join-Path $AssetRoot "eng.traineddata") (Join-Path $Output "models/ocr/eng.traineddata") -Force
Copy-Item (Join-Path $PSScriptRoot "KULLANIM.md") $Output -Force
Copy-Item (Join-Path $PSScriptRoot "licenses/*") (Join-Path $Output "LISANSLAR") -Force
Copy-Item (Join-Path $PSScriptRoot "../LICENSE") (Join-Path $Output "LICENSE") -Force
$TestCommand="@echo off`r`npushd `"%~dp0`"`r`nLudil.exe --ui-smoke `"%TEMP%\Ludil-QA`"`r`npopd`r`n"
Set-Content -Path (Join-Path $Output "Ludil-Test.cmd") -Value $TestCommand -Encoding ascii -NoNewline
$OutputRoot=(Resolve-Path $Output).Path
$ManifestPath=Join-Path $OutputRoot "DOSYA-OZETLERI.sha256"
$Lines=Get-ChildItem $OutputRoot -File -Recurse | Where-Object { $_.FullName -ne $ManifestPath } | Sort-Object FullName | ForEach-Object {
    $Relative=$_.FullName.Substring($OutputRoot.Length+1).Replace('\','/')
    (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()+"  "+$Relative
}
Set-Content -Path $ManifestPath -Value $Lines -Encoding ascii
Write-Output "Windows PC paketi hazir: $Output"
