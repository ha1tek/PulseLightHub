$buildTools = "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0"
$androidJar = "$env:LOCALAPPDATA\Android\Sdk\platforms\android-35\android.jar"
$projectDir = "C:\ai_projects\root_android_free\PulseLightApp"
$keystore = "$env:USERPROFILE\.android\debug.keystore"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

New-Item -ItemType Directory -Force -Path "$projectDir\build\gen", "$projectDir\build\classes", "$projectDir\build\dex" | Out-Null

Write-Host "--- 1. Compiling resources with aapt2 ---"
& "$buildTools\aapt2.exe" compile --dir "$projectDir\res" -o "$projectDir\build\compiled_res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

Write-Host "--- 2. Linking resources ---"
& "$buildTools\aapt2.exe" link -o "$projectDir\build\unaligned_unpacked.apk" -I "$androidJar" --manifest "$projectDir\AndroidManifest.xml" --java "$projectDir\build\gen" "$projectDir\build\compiled_res.zip" --auto-add-overlay
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "--- 3. Compiling Java ---"
$javaFiles = Get-ChildItem -Recurse "$projectDir\src\main\java\*.java", "$projectDir\build\gen\*.java" | Select-Object -ExpandProperty FullName
& "javac.exe" -d "$projectDir\build\classes" -cp "$androidJar" --release 8 $javaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "--- 4. Running D8 dexer ---"
$classFiles = Get-ChildItem -Recurse "$projectDir\build\classes\*.class" | Select-Object -ExpandProperty FullName
& "$buildTools\d8.bat" --min-api 26 --lib "$androidJar" --output "$projectDir\build\dex" $classFiles
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host "--- 5. Adding dex to APK ---"
& "jar.exe" -uf "$projectDir\build\unaligned_unpacked.apk" -C "$projectDir\build\dex" classes.dex
if ($LASTEXITCODE -ne 0) { throw "jar add failed" }

Write-Host "--- 6. Zipalign ---"
Remove-Item -Force "$projectDir\build\PulseLightHub.apk" -ErrorAction SilentlyContinue
& "$buildTools\zipalign.exe" -p -f 4 "$projectDir\build\unaligned_unpacked.apk" "$projectDir\build\PulseLightHub.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host "--- 7. Signing APK ---"
& "$buildTools\apksigner.bat" sign --ks "$keystore" --ks-pass pass:android --key-pass pass:android "$projectDir\build\PulseLightHub.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host "--- 8. Installing APK on device ---"
& $adb install -r "$projectDir\build\PulseLightHub.apk"

Write-Host "--- 9. Granting WRITE_SECURE_SETTINGS ---"
& $adb shell pm grant com.antigravity.pulselight android.permission.WRITE_SECURE_SETTINGS

Write-Host "--- 10. Launching MainActivity ---"
& $adb shell am start -n com.antigravity.pulselight/.MainActivity

Write-Host "Build & Deployment Finished Successfully!"
