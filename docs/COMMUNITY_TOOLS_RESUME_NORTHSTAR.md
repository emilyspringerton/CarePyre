# NORTHSTAR — Community Tools: Resume/CV Builder + Verifier

**Status:** Shipped, v0 backend + frontend (console.html). Layer 3 export rendering not built.
**Date:** 2026-09-09

Founder, real-time, verbatim across the ask: "we want to build a tool to maintain and verify
resume data" → "build it as a separate app using a new instance of iduna pro" →
"cvb.okemily.com for cvbuilder" → on what "verify" means: "we want to ensure the output is
structured to cater to machines reading the resume theres always advice to run it through a
resume validator i dont want to put my data into some rando site lets build our own to build
towards known standards for machine readable cvs" → **real, final correction**: "build it into
carepyre actually it makes sense to have that as part of the community tools but we want it to be
gated so that accounts need a feature flag set to see those features."

**Real, honest history**: this was scoped and partially built as its own standalone app
(`CVBUILDER`, backed by a dedicated new IDUNA_PRO instance) before the founder's own real-time
correction redirected it here — into CarePyre's own already-live IDUNA_PRO instance
(`idunapro.service`, CarePyre Console, `:8081`), as the first real "Community Tools" feature,
gated by a real per-account feature flag rather than a separate app/deployment. The
`CVBUILDER` scaffold was deleted; the one piece of real, unchanged value carried over is the
JSON Resume data model itself (`internal/resume` in `IDUNA_PRO`), which never depended on which
app hosted it.

## 1. What this actually is, in one sentence

A real "Community Tools" feature inside CarePyre's own console (IDUNA_PRO-backed), gated by a
per-account feature flag, for maintaining structured resume/CV data and verifying — against our
own, real, transparent rules, not a third-party site — that it will actually parse correctly as
machine-readable input (ATS/applicant-tracking-system parsers, and structured-data consumers
generally).

## 2. Real, concrete grounding: JSON Resume is the known standard

Checked directly (fetched the real, current schema, not assumed from memory):
`jsonresume.org`'s own schema is a real, open, widely-adopted JSON Schema for resume data —
exactly the "known standard for machine readable CVs" the founder's own framing asks for.
`IDUNA_PRO/internal/resume/model.go` is a direct, field-for-field Go mirror of it:

```
basics       — name, label, image, email, phone, url, summary,
                location {address, postalCode, city, countryCode, region},
                profiles [{network, username, url}]
work         — name, location, description, position, url, startDate, endDate, summary, highlights
volunteer    — organization, position, url, startDate, endDate, summary, highlights
education    — institution, url, area, studyType, startDate, endDate, score, courses
awards       — title, date, awarder, summary
certificates — name, date, url, issuer
publications — name, publisher, releaseDate, url, summary
skills       — name, level, keywords
languages    — language, fluency
interests    — name, keywords
references   — name, reference
projects     — name, description, highlights, keywords, startDate, endDate, url, roles, entity, type
meta         — canonical, version, lastModified
```

A user's data stays trivially exportable as valid JSON Resume at any time; "does this conform to
the known standard" is a real, mechanical structural question, not a subjective one.

## 3. What "verify" really means here — three real, separate, honest layers

Not fact-checking claims, not a vague "looks good" grade — a real check that the data will
actually be read correctly by a machine, with the rules themselves visible (this tool's own, not
a third party's opaque algorithm — the founder's own "not some rando site" framing directly).

**Layer 1 — schema conformance.** `basics.name`/`basics.email` present, at least one real
`work`/`education` entry. Shipped: `internal/resume.Verify`.

**Layer 2 — ATS-readiness rules, this tool's own, named explicitly.** `basics.phone` present (the
second field almost every real ATS parser keys off); every `work`/`education` entry has a real,
parseable `startDate`/`endDate` in JSON Resume's own real date format (`YYYY`/`YYYY-MM`/
`YYYY-MM-DD`), not a free-text string a real ATS date-parser chokes on; a `work` entry with a
`position` but no employer `name` is flagged (a real, common ATS-breaking mistake). Shipped, same
function — a real, itemized `VerifyResult{Checks: []Check{Rule, Passed, Message}}`, never a
single opaque score.

**Layer 3 — export-format safety (real, not yet built).** ATS-readability is also a property of
the RENDERED document (tables, multi-column layouts, image-only text are all real, documented ATS
parsing pitfalls at the rendering layer, not the data layer). A real, deliberately plain,
single-column, real-selectable-text export template is the real way "verify" closes this loop —
**not built in this pass**, named honestly as Phase 2 below.

## 4. Real architecture, as shipped

Reuses CarePyre's own already-live IDUNA_PRO instance (`idunapro.service`, `:8081`) — no new app,
no new deployment. New real pieces, all inside `IDUNA_PRO`:

- **`internal/resume`** — the JSON Resume-shaped Go data model (`model.go`) + the real,
  three-layer-minus-export verify logic (`verify.go`, Layers 1-2).
- **`internal/userlog`** — `LocalUser.IsCommunityToolsEnabled`, a real, plain per-account
  feature flag (event-sourced: `EventUserCommunityToolsChanged` / `UserCommunityToolsChangedData`,
  same grant/revoke-in-one-event-type shape `IsAdmin`/`IsProvider` already established),
  deliberately SEPARATE from the 4-tier admin/provider RBAC — this is for ordinary
  community-participant accounts, not staff.
- **`internal/http/handlers/local_auth.go`** — `localUserPermissions` grants a real
  `"community-tools.access"` permission whenever the flag is set, checked independently of
  tier (an admin who wants personal access needs the same flag on their own account too — a real,
  literal reading of "accounts need a feature flag set," no tier gets it implicitly).
- **`internal/http/handlers/users.go`** — `PATCH /api/v1/users/{uid} {"is_community_tools_enabled":
  true}`, `users.admin`-gated (same tier as `org_id` reassignment — grants no elevated RBAC tier,
  only access to one participant-facing feature).
- **`internal/http/handlers/community_tools.go`** — `GET`/`PUT /api/v1/community-tools/resume`
  (one resume per user, scoped to the caller's own `local_uid` via the JWT's own `local_uid`
  claim, no cross-user access path at all) and `POST /api/v1/community-tools/resume/verify`.
  Every route `community-tools.access`-gated in `main.go`.
- **Migration** `202609090001_community_tools.sql` — `local_users.is_community_tools_enabled`
  column + the `resumes` table (one JSON blob per user, not one column per JSON Resume field —
  the same "don't over-normalize past what's actually needed" judgment this monorepo's own IDUNA
  inventory work already made for its own free-text columns).

Real, live-verified (not just `go build`/`go test`): booted the real binary against a fresh
SQLite file, the new migration applied cleanly, `resumes` table and the new
`is_community_tools_enabled` column both present, `/health` OK. 7 new Go tests (3
`internal/resume` + a handful more — see `internal/resume/verify_test.go` and
`internal/http/handlers/community_tools_test.go`), all passing, full existing suite (`go test
./...`) green, zero regressions.

## 4a. Real frontend, shipped same day

New "Resume" sidebar item in `console.html`, hidden by default (shown only when
`effective_permissions` includes `community-tools.access` — a UX convenience only, every real
call still requires the permission server-side, the same convention `nav-admin` already
establishes). Real, editable Basics fields (name/headline/email/phone/summary), real dynamic,
repeatable Work Experience and Education entry lists (add/remove rows, matching JSON Resume's
own real array shape, not a fixed-count form), Save (`PUT`) and Verify (`POST .../verify`)
buttons, and a real, itemized verify report (per-rule pass/fail + message, never a single
score).

**Real, live-found correctness issue fixed while building this, not shipped broken**: entry rows
were first drafted via string-concatenated `innerHTML` with `esc()`'d values spliced into
`value="..."` attributes — `esc()` (already defined earlier in this file) only escapes
`&`/`<`/`>` for safe TEXT CONTENT, not the double-quote a real field value (a quoted job title,
an apostrophe in a name) could contain, which would break out of the attribute and inject HTML.
Fixed by building entry-row inputs via `document.createElement` + setting `.value` as a real DOM
property (never HTML-parsed), not string concatenation — the only place this file still builds
HTML via `esc()` + string concat is the verify-results panel, which is a genuinely safe text-
content position, not an attribute value.

JS syntax verified directly (`node --check` against the extracted `<script>` block); HTML
`<div>`/`</div>` tag balance verified directly (119/119) — this repo has no build/test pipeline
for static HTML, so these are the real, honest limits of what got verified here; no live
browser/click-through test was run.

## 5. Real, honest, not done

- **Layer 3 (export-format safety) is not built.** No ATS-safe PDF/DOCX render path exists yet.
- **No multiple resume variants per user** — v0 is deliberately one resume per account.
- Section-label vocabulary advisory checks (real ATS advice about non-standard headers) named in
  the original scoping pass but not implemented — a real, separate, smaller follow-up to Layer 2.
- No live browser verification of the new console.html UI (see §4a) — syntax/structure checked
  directly, a real click-through was not.
