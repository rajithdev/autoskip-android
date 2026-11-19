# AutoSkip

An Android app that presses YouTube's **Skip** button for you, the moment it appears.

It is not an ad blocker. It does exactly what your finger would do: when a skippable ad shows its Skip button, AutoSkip taps it. Ads without a Skip button play as usual, and nothing is skipped sooner than YouTube allows.

## What it can and can't see

AutoSkip uses Android's Accessibility permission, which is powerful, so here is exactly what it does with it:

- **It only looks at YouTube.** The service is registered for the YouTube app's package only, and the code only inspects YouTube's windows, to find the Skip button.
- **Nothing leaves your phone.** The app requests no permissions at all, including internet. Android itself prevents it from sending anything anywhere. You can check this under App info, Permissions.
- **It only presses Skip.** It taps a button whose label is exactly "Skip", "Skip ad", "Skip ads" or "Skip this ad" (or one of YouTube's known skip-button ids), and nothing else.

All of the logic is in three short files under [`app/src/main/java/com/autoskip/app`](app/src/main/java/com/autoskip/app): `SkipService.java` (the service), `SkipMatcher.java` (what counts as a Skip button) and `Prefs.java` (the counter and pause switch).

## Install

Download `AutoSkip.apk` from the [latest release](../../releases/latest).

### From the file

1. Open the APK on your phone and allow installs from that app when asked.
2. Open AutoSkip, tap **Turn on AutoSkip**, read the explanation, and continue to Accessibility settings.
3. Find AutoSkip under Installed apps (or Downloaded apps), switch it on, and tap **Allow** on Android's warning.

If the switch is greyed out, open the app's **App info**, tap the three dots at the top right, and choose **Allow restricted settings**. Android requires this for any app installed from a file.

### If Play Protect blocks the install

In some countries Google Play Protect refuses apps installed from a downloaded file when they use Accessibility, with no option to continue. In that case install over USB from a computer instead:

1. On the phone, enable Developer options and **USB debugging**.
2. Connect the phone and run:

   ```bash
   adb install AutoSkip.apk
   ```

3. Turn AutoSkip on as in step 2 above, then switch USB debugging back off.

## About Android's warning

When you switch AutoSkip on, Android warns that it can view and control your screen. Every app that uses Accessibility gets the same warning, whatever the app actually does, and no app can change or remove it. It is right to read it carefully and to only allow apps whose behaviour you can check, which is why this source code is public.

## Limitations

- **English only for now.** The button is recognised by YouTube's internal ids and by English labels. For another language, add the translated label to `LABELS` in `SkipMatcher.java`.
- **YouTube can change.** If a YouTube update renames the button, skipping stops until the ids or labels here are updated.
- **Force-stopping the app turns the service off.** That is Android's behaviour; switch it on again in Accessibility settings.

Tested on Android 16 with YouTube 21.45.82. Requires Android 8.0 or later.

## Build from source

The app is plain Java with no libraries, so it builds with just a JDK and the Android SDK's own tools. No Gradle or Android Studio is needed.

You need a JDK (17 or later) and an Android SDK with a `platforms` package and a `build-tools` package installed. Then:

```bash
./build.sh
```

This writes `AutoSkip.apk` to the project folder. The SDK is looked up in `$ANDROID_HOME`, then in `~/Library/Android/sdk`.

The first build creates a signing key, `debug.keystore`, which is deliberately not in this repository. An APK you build yourself is therefore signed with your own key and cannot be installed over an official release; uninstall the other one first.

## Disclaimer

AutoSkip is an independent project and is not affiliated with or endorsed by YouTube or Google. Automating taps in the YouTube app may be against YouTube's Terms of Service; use it at your own discretion.

## Licence

[MIT](LICENSE)
