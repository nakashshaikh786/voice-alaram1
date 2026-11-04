# Voice Alarm (Android)

Record your voice, pick a date and time, and it rings as a full-screen alarm (works with the phone locked
or the app closed). Built for Android 8+ (tested target: Android 13 / One UI 5.1).

## Get the APK - option A: no Android Studio (GitHub)
1. Create a free account at github.com and a new empty repository.
2. Upload everything in this folder (keep the hidden .github folder) with "Add file > Upload files".
3. Open the Actions tab > "Build APK" > Run workflow (it also runs automatically after the upload).
4. When it turns green, open the run, download the "VoiceAlarm-apk" artifact, unzip it to get app-debug.apk.
5. Copy app-debug.apk to your phone and open it. Allow "Install unknown apps" for your file manager or browser.

## Get the APK - option B: Android Studio
Open this folder in Android Studio, wait for Gradle sync, then Build > Build APK(s)
(or press Run with the phone connected by USB and USB debugging on).

## Samsung phone setup (important for reliable alarms)
- Allow Microphone and Notifications when asked.
- In the app tap "Allow ringing in background (battery)" and choose Allow.
- Settings > Battery > Background usage limits > Never sleeping apps > add Voice Alarm.
- Alarm volume is controlled by the Alarm slider in Settings > Sounds and vibration > Volume.
