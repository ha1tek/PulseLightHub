$userShort = "C:\Users\0935~1"
$buildTools = "$userShort\AppData\Local\Android\Sdk\build-tools\34.0.0"
$androidJar = "$userShort\AppData\Local\Android\Sdk\platforms\android-35\android.jar"
$projectDir = "C:\ai_projects\root_android_free\PulseLightApp"
$keystore = "$userShort\.android\debug.keystore"
$jdk = "$userShort\AppData\Local\Programs\jdk-17\jdk-17.0.12+7"
$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"
$javac = "$jdk\bin\javac.exe"
$jar = "$jdk\bin\jar.exe"
$adb = "C:\platform-tools\adb.exe"
$device = "192.168.1.3:5555"

Write-Host "=== Building PulseLightHub (Offline Architecture) ==="
New-Item -ItemType Directory -Force -Path "$projectDir\build\gen", "$projectDir\build\classes", "$projectDir\build\dex" | Out-Null

# Clean previous build artifacts
Remove-Item -Recurse -Force "$projectDir\build\classes\*", "$projectDir\build\dex\*" -ErrorAction SilentlyContinue

Write-Host "--- 1. Compiling resources with aapt2 ---"
& "$buildTools\aapt2.exe" compile --dir "$projectDir\res" -o "$projectDir\build\compiled_res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

Write-Host "--- 2. Linking resources ---"
& "$buildTools\aapt2.exe" link -o "$projectDir\build\unaligned_unpacked.apk" -I "$androidJar" --manifest "$projectDir\AndroidManifest.xml" --java "$projectDir\build\gen" "$projectDir\build\compiled_res.zip" --auto-add-overlay
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "--- 3. Compiling Java ---"
$classpath = "$androidJar"
$javaFiles = Get-ChildItem -Recurse "$projectDir\src\main\java\*.java", "$projectDir\build\gen\*.java" | Select-Object -ExpandProperty FullName
& $javac -encoding UTF-8 -d "$projectDir\build\classes" -cp "$classpath" --release 8 $javaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "--- 4. Running D8 dexer ---"
$classFiles = Get-ChildItem -Recurse "$projectDir\build\classes\*.class" | Select-Object -ExpandProperty FullName
& "$buildTools\d8.bat" --min-api 26 --lib "$androidJar" --output "$projectDir\build\dex" $classFiles
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host "--- 5. Adding dex to APK ---"
& $jar -uf "$projectDir\build\unaligned_unpacked.apk" -C "$projectDir\build\dex" classes.dex
if ($LASTEXITCODE -ne 0) { throw "jar add failed" }

Write-Host "--- 6. Zipalign ---"
Remove-Item -Force "$projectDir\build\PulseLightHub.apk" -ErrorAction SilentlyContinue
& "$buildTools\zipalign.exe" -p -f 4 "$projectDir\build\unaligned_unpacked.apk" "$projectDir\build\PulseLightHub.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host "--- 7. Signing APK ---"
& "$buildTools\apksigner.bat" sign --ks "$keystore" --ks-pass pass:android --key-pass pass:android "$projectDir\build\PulseLightHub.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host "--- 8. Installing APK on device ($device) ---"
& $adb -s $device install -r "$projectDir\build\PulseLightHub.apk"
if ($LASTEXITCODE -ne 0) { throw "adb install failed" }

Write-Host "--- 9. Granting WRITE_SECURE_SETTINGS ---"
& $adb -s $device shell pm grant com.antigravity.pulselight android.permission.WRITE_SECURE_SETTINGS
& $adb -s $device shell settings put global customize_breath_light_time 00002359
& $adb -s $device shell settings put global customize_breath_light_master_switch 1
& $adb -s $device shell settings put global oplus_breath_light_master_switch 1
& $adb -s $device shell settings put global customize_breath_light_flip_switch 0

Write-Host "--- 10. Native Driver: Direct Binder IPC (No Daemon required) ---"

Write-Host "--- 11. Launching MainActivity ---"
& $adb -s $device shell am force-stop com.antigravity.pulselight
& $adb -s $device shell am start -n com.antigravity.pulselight/.MainActivity --windowingMode 1

Write-Host "=== Build & Installation Finished Successfully! ==="
