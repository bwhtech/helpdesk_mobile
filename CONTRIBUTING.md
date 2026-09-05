# Contributing

## Branches

`develop` is the trunk. Every pull request targets it, and it always carries the next
major version of the app.

`version-N` branches are the release lines. Each one is cut from `develop` when that
major version ships, and release tags (`v1.4.0`, `v1.4.1`) are created on it. Nothing
lands on a release line except fixes for that line.

At the time of writing the app is pre v1, so `develop` and `main` are the only branches;
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

Every pull request runs two required workflows:

- `build`: gradle wrapper validation, `assembleDebug`, `lintDebug`, `testDebugUnitTest`,
  and `assembleInternal`. The last one is the minified variant, so R8 and resource
  shrinking run against `proguard-rules.pro` on every change rather than for the first
  time at the release tag.
- `secret-scan`: gitleaks over the full history, pinned by version and SHA256.

Lint is gated on errors only; warnings do not fail the build. Fix a new lint error
rather than adding it to a baseline.

## Local setup

Copy `local.properties.sample` to `local.properties` and fill in the SDK path. The
Firebase config at `app/google-services.json` is not in the repository; CI builds use
`app/google-services.json.sample`, which carries the package name and nothing else.
