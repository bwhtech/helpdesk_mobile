# Helpdesk Analytics

Native Android client for Frappe Helpdesk: tickets, analytics, and reply/comment from the phone.

## Build setup

1. Copy `local.properties.sample` to `local.properties` and set `sdk.dir`.
2. Add your Firebase config at `app/google-services.json` (untracked). Use
   `app/google-services.json.sample` as the shape; download the real file from the
   Firebase console for package `com.example.helpdeskanalytics`.
3. Build: `./gradlew assembleDebug`.

Credentials (site URL + API key/secret) are entered at login and stored in
encrypted preferences; nothing is baked into the APK.
