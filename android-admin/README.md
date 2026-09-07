# IDUNA Pro Admin (Android)

Real, minimal white-label Android wrapper (CP-WHITELABEL-1, founder real-time: "it needs to be
both white labeled first then made into carepyre"). One codebase, one `:app` module, a
WebView-based `MainActivity` pointed at a remote IDUNA_PRO console (`console.html` +
`BrandingHandler`'s own `/api/v1/branding` — see `IDUNA_PRO/internal/http/handlers/branding.go`
and `CarePyre/console.html`'s `applyBranding()`). Tenant identity is entirely a product-flavor
concern (`app/build.gradle.kts`):

| Flavor     | applicationId       | App name          | Console URL                        |
|------------|---------------------|-------------------|-------------------------------------|
| `generic`  | `pro.iduna.admin`   | IDUNA Pro Admin   | `https://example.invalid/console.html` (placeholder) |
| `carepyre` | `org.carepyre.admin`| CarePyre Admin    | `https://carepyre.org/console.html` |

Adding a third tenant means adding a third flavor block, never forking this module.

## Why a native wrapper at all, if the console is already a website

App-store presence and OS-level permission plumbing. The one real piece of native glue this
needs is `MainActivity`'s `WebChromeClient.onPermissionRequest` override, which is what lets the
console's own compliance-recording panel (CP-COMPLIANCE-REC-1, `getUserMedia`/`MediaRecorder`)
actually reach the device microphone from inside a WebView — that permission bridge doesn't exist
in a plain mobile browser tab the same way.

## Status — real, honest gap

Source-complete, not build-verified: this sandbox has no Android SDK at all (same gap already
named for `SPIDERBEETLE` and `MJOLNIR` in this monorepo), so `./gradlew assembleCarepyreDebug`
has not actually been run here. Build via CI or the founder's own machine, same as those two.

## Related

- `IDUNA_PRO/internal/http/handlers/branding.go` — the white-label API this app's console target
  reads at load.
- `IDUNA_PRO/internal/http/handlers/compliance_recording.go` — the consent-recording API the
  admin panel (reachable through this app) uses.
- `CarePyre/console.html` — the actual product; this app is packaging around it.
- `CarePyre/android` — the separate, unrelated CarePyre SIP softphone app (`org.carepyre.sip`);
  this is a new, second Android app, not a rewrite of that one.
