நாதலை FM — Native Android Starter Project

இது Kotlin + AndroidX Media3 பயன்படுத்தும் ஆரம்ப native Android project. இது இன்னும் build செய்து test செய்யப்படாத starter source; APK ஏற்கெனவே தயாராகிவிட்டது என்று பொருள் இல்லை.

PHONE-ல் இலவசமாக APK build செய்யும் வழி (GitHub Actions):
1. ZIP-ஐ download செய்து extract செய்யவும்.
2. GitHub account-ல் புதிய repository உருவாக்கவும்: NaadhalaiFM (Public அல்லது Private).
3. Extract செய்த project-இன் உள்ளடக்கங்களை repository-க்கு upload செய்யவும். Root-ல் settings.gradle.kts, build.gradle.kts, app/, .github/ இருக்க வேண்டும்.
4. GitHub repository-ல் Actions tab திறக்கவும்.
5. “Build Naadhalai FM APK” workflow தேர்வு செய்து “Run workflow” அழுத்தவும்.
6. Build முடிந்ததும் run page-இல் Artifacts பகுதியில் “NaadhalaiFM-debug-apk” download செய்யவும்.
7. ZIP artifact-ஐ extract செய்து app-debug.apk-ஐ install செய்யவும். Android கேட்டால் browser/files app-க்கு “Install unknown apps” permission-ஐ நீங்களே அனுமதிக்கவும்.

குறிப்புகள்:
- GitHub account / internet தேவை. GitHub Actions usage limits மற்றும் policies பொருந்தும்; free quota மாறலாம்.
- APK build முதல் முறையில் dependencies download செய்வதால் நேரம் ஆகலாம்.
- இது ஆரம்ப prototype. Tamil FM stream URLs முன்கூட்டியே சேர்க்கப்படவில்லை; உங்களுக்கு உரிமையுள்ள/அனுமதிக்கப்பட்ட direct audio stream URL மட்டும் சேர்க்கவும்.
- Background playback service, notification controls, favourites persistence, signed release APK, Play Store preparation இன்னும் சேர்க்கப்படவில்லை.
- இந்த ZIP-ல் Android project source உள்ளது; build செய்யப்பட்ட APK அல்ல.
