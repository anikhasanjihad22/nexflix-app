# Nexflix Android App (WebView + Push Notifications + Auto Update)

Turns your Blogger site into a real Android app: no URL bar, downloads
saved to a `Nexflix` folder, pull-to-refresh, push notification with
thumbnail on every new post, and forced auto-update. Built entirely with
GitHub Actions — no computer needed.

## What's already done for you
- `MainActivity.kt` — the WebView engine (no URL bar, downloads, pull-to-refresh, offline screen, back button behavior)
- `NexflixMessagingService.kt` — shows the notification with image + caption, opens the exact post on tap
- `.github/workflows/build.yml` — builds a signed release APK automatically on every push
- `.github/workflows/notify.yml` — checks your Blogger RSS every 10 minutes and sends the notification
- `version.json` — controls the force-update popup

## Step 1 — Change these 3 things first
1. `app/src/main/res/values/strings.xml` → set `base_url` to your Blogger URL, and `version_check_url` to your future GitHub raw file link
2. `scripts/check_new_post.py` → set `BLOGGER_RSS_URL` to `https://YOURSITE.blogspot.com/feeds/posts/default?alt=rss`
3. `version.json` → replace `YOUR_USERNAME/YOUR_REPO` with your actual GitHub repo path (twice — in this file and in strings.xml's version_check_url)

## Step 2 — Create a Firebase project (free)
1. Go to https://console.firebase.google.com → Add project
2. Add an Android app → package name: `com.nexflix.app`
3. Download `google-services.json` (do NOT commit this to GitHub — see Step 4)
4. In Firebase Console → Project Settings → Service Accounts → "Generate new private key" → this downloads a second JSON file (used for sending notifications from the script)

## Step 3 — Create a signing keystore (one-time, needed to publish real releases)
Since you don't have Android Studio, you can generate this using GitHub Codespaces
(free, browser-based) or Termux on your phone:
```
keytool -genkey -v -keystore nexflix.jks -keyalg RSA -keysize 2048 -validity 10000 -alias nexflix
```
Keep this file safe — you'll need the SAME keystore for every future update, or
Android/Play Store will reject it as a different app.

## Step 4 — Add GitHub Secrets
In your GitHub repo → Settings → Secrets and variables → Actions → New repository secret. Add:

| Secret name | Value |
|---|---|
| `GOOGLE_SERVICES_JSON` | base64 of `google-services.json` (run `base64 -w0 google-services.json`) |
| `KEYSTORE_BASE64` | base64 of `nexflix.jks` (run `base64 -w0 nexflix.jks`) |
| `KEYSTORE_PASSWORD` | the password you set in Step 3 |
| `KEY_ALIAS` | `nexflix` (or whatever alias you used) |
| `KEY_PASSWORD` | the key password you set in Step 3 |
| `FIREBASE_SERVICE_ACCOUNT` | the FULL contents of the service-account JSON from Step 2.4 (paste as-is, not base64) |

## Step 5 — Push to GitHub
Upload this whole folder to a new GitHub repo (you can do this from the GitHub
mobile app or the website's "Add file → Upload files").
Once pushed to `main`, the **Build Nexflix APK** workflow runs automatically →
check the "Actions" tab → once green, your APK is attached under "Releases"
on the right side of your repo, and also downloadable from the workflow run
as an artifact.

## Step 6 — Replace the placeholder icon
`ic_launcher_foreground.xml` / `ic_launcher_background.xml` are simple
placeholders (red background, white "N"). Replace them with your real logo:
- Easiest free way: use https://icon.kitchen or https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html on your phone, generate the adaptive icon files, and upload them into `app/src/main/res/mipmap-*` folders (replacing the vector-based ones), or just replace the two XML files with your own vector logo.

## Step 7 — Every time you update the app
1. Bump `versionCode` (and `versionName`) in `app/build.gradle`
2. Push to `main` → new signed APK builds automatically
3. Update `version.json`: set `latestVersionCode` to the new number, `forceUpdate: true`, and `apkUrl` to the new release link
4. Old installs will now show the blocking "Update Now" dialog and won't be usable until updated

## Sending an "update available" push notification
Every time you release a new version, you can also push an actual notification
(not just the in-app blocking dialog):
1. Go to your repo → **Actions** tab → **Notify App Update** (left sidebar) → **Run workflow**
2. Fill in the message and paste the new APK's download link (from Releases)
3. Run it → every user with the app installed gets a notification; tapping it opens the download link directly

## Notes
- Notifications go out to everyone via the FCM topic `new_posts` — free, no per-user setup needed.
- The RSS check runs every 10 minutes via `notify.yml`. You can lower this, but GitHub free-tier scheduled jobs aren't always exact-to-the-minute.
- To publish on the Play Store later, this same signed APK (as an `.aab`, add `bundleRelease` task) can be uploaded — that only needs the one-time $25 Play Console fee, nothing else changes.
