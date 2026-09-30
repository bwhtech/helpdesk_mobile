# Contributing

## Branches

`develop` is the trunk. Every pull request targets it, and it always carries the next
major version of the app.

`version-N` branches are the release lines. Each one is cut from `develop` when that
major version ships, and release tags (`v1.4.0`, `v1.4.1`) are created on it. Nothing
lands on a release line except fixes for that line.

At the time of writing the app is pre v1, so `develop` is the only long lived branch;
`version-1` gets cut once the auth work and the surrounding polish are done.

When the Frappe UI native rework ships as v2, `version-2` is cut from `develop`, and
`develop` moves on to v3 material. The two lines diverge in UI stack from that point, so
a v1 fix cannot be cherry picked from `develop`; branch it off `version-1` directly and
forward port by hand only where the bug exists in both.

Branch names follow the commit type: `feat/oauth-login`, `fix/bottom-nav-label-wrap`,
`ci/build-hardening`, `docs/backend-schema`.

## Releases

Releases are tag driven. Pushing a `v*` tag runs `.github/workflows/release.yml`, which
builds a signed APK, verifies the signature, names it after the tag and publishes a
GitHub release. The tag is the only trigger, so the workflow works the same on any
release line.

The app reads its own version from the nearest reachable tag, so a build made on
`develop` after the lines diverge reports `0.0.0`. That is expected for debug and
internal builds.

## Checks

Every pull request must pass four checks before it can merge, and must be up to date
with `develop`:

- `build`: gradle wrapper validation, `assembleDebug`, a check that the exported Room
  schema under `app/schemas` is committed, `lintDebug`, `testDebugUnitTest`, and
  `assembleInternal`. The last one is the minified variant, so R8 and resource
  shrinking run against `proguard-rules.pro` on every change rather than for the first
  time at the release tag.
- `detekt`: static analysis against `config/detekt/detekt.yml`, in its own job of the
  `build` workflow.
- `emulator-tests`: `connectedDebugAndroidTest` on an API 35 emulator, in its own job of
  the `build` workflow. The tests under `app/src/androidTest` sign in to a fake Helpdesk
  site that runs on the device and answers from the JSON under
  `app/src/androidTest/assets/fake_site`, one file per request path, so they need no
  secrets. A failed run uploads a screenshot of each failing test, the report and
  logcat.
- `gitleaks`: the `secret-scan` workflow, gitleaks over the full history, pinned by
  version and SHA256.

Lint is gated on errors only; warnings do not fail the build. Fix a new lint error or
detekt finding rather than adding it to a baseline.

The `screenshots` workflow is advisory and never blocks a merge. It renders the screens in
`ScreenshotTest` with Roborazzi and compares them with the images recorded on the last
push to `develop`. The job summary lists any screen that looks different, and the
`screenshots` artifact holds develop's image, the pull request's image and a diff of each.
References are only recorded on the Linux runner, since macOS renders differently, so
nothing is committed. A screen whose pixels depend on the clock stays out of
`ScreenshotTest`.

## Local setup

Copy `local.properties.sample` to `local.properties` and fill in the SDK path. The
Firebase config at `app/google-services.json` is not in the repository; CI builds use
`app/google-services.json.sample`, which carries the package name and nothing else.

`./gradlew connectedDebugAndroidTest` installs a debug build on every connected device
and clears its data before each test. With a phone attached, set `ANDROID_SERIAL` to the
emulator's serial so the run stays off it.
