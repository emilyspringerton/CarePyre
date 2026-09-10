# NORTHSTAR — Community Tools: Resume/CV Builder + Verifier

**Status:** Shipped — master resume (now including a "Links" section for multiple GitHub/
LinkedIn/portfolio links), real bespoke Target variants (show/hide + text overrides) covering
all five entry types (Work/Education/Skill/Award/Profile), client-side preview templates, a
real, downloadable, ATS-safe PDF export file (Layer 3), and a real agent-ergonomic API surface
(PATCH/POST/DELETE primitives + a published OpenAPI spec). No DOCX export path yet.
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

**Layer 3 — export-format safety (real, shipped).** ATS-readability is also a property of
the RENDERED document (tables, multi-column layouts, image-only text are all real, documented ATS
parsing pitfalls at the rendering layer, not the data layer). A real, deliberately plain,
single-column, real-selectable-text PDF export (`internal/resume/pdf.go`'s own `RenderPDF`, built
on `github.com/go-pdf/fpdf`, no cgo) is the real way "verify" closes this loop — see §4d.

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

## 4b. Real Target resumes — bespoke, tailored variants per opportunity, shipped same day

Founder real-time arc, followed in full: "like we have a base set of things that we tell the
system in terms of history and skills etc we need some way to start building more bespoke
resumes for specific opportunities" → "the pattern probably applies to education awards
experience skills even different little summary texts" → "this is probably most useful in the
skills section" → "we are going to want to keep all of the exported tweaked versions so we can
clone from them for new opportunities."

Real, deliberate design: the master resume (§2-§4) stays the single, full source of truth. A
Target never duplicates or edits that data — it real, named SELECTS a subset of it (by stable
ID) to show for one specific opportunity, plus a small, named set of text overrides for the
professional summary/headline (the two blurbs that actually get rewritten per application in
practice — "even different little summary texts"), not a general per-entry text-override
mechanism, named honestly as out of scope.

New `Work.ID`/`Education.ID`/`Skill.ID`/`Award.ID` — a real, deliberate CarePyre extension
beyond strict JSON Resume (which has no `id` field), server-assigned (`assignResumeIDs`)
whenever a saved entry arrives with none, so a Target's own selection lists reference a
specific entry stably, surviving reorders/edits to other entries.

New `internal/resume/target.go`: `Target{IncludedWorkIDs, IncludedEducationIDs,
IncludedSkillIDs, IncludedAwardIDs, SummaryOverride, LabelOverride}` + `Resolve(master, target)
*Resume`, a real generic `filterByID[T]` shared across all four sections. A nil/empty
selection resolves to EMPTY, not a fallback to "show everything" — honest, literal show/hide.

New `CommunityToolsTargetsHandler`: `GET`/`PUT /api/v1/community-tools/resume/targets`
(whole-list replace, matching the master resume's own established `PUT` convention — an entry
with no `id` is a new target, one missing from the new list is a real delete, satisfying "clone
from them" with zero new API surface: a clone is just a new, unsaved target row pre-filled from
an existing one's current values), plus `GET .../targets/{id}/resolved` and
`POST .../targets/{id}/verify` — verification runs against the real RESOLVED view, not the
master, catching e.g. "this target hides every work entry, it now fails
has-work-or-education" even though the master itself passes.

New migration `202609090002_resume_targets.sql`: a sibling `targets` JSON column on the same
`resumes` row (targets are views over one user's one master resume, not independent documents).

New "Bespoke resumes" card in `console.html`: a real checkbox per master Work/Education/Skill/
Award entry (human-labeled, e.g. "Line Cook — Acme Corp"), real optional summary/headline
override fields (a real checkbox gates whether an override is sent at all — `null` vs. an
empty string are genuinely different on the wire, matching `SummaryOverride`'s own
`*string`-pointer contract), and real Clone/Verify/Preview/Remove actions per bespoke resume.

**Real, live-found bug fixed before shipping, not caught by syntax checking alone**: the
"Remove" button on a bespoke-resume card originally removed only the DOM element, never the
matching entry in `currentTargets` (the real, separate source-of-truth array "New"/"Clone"
re-render FROM) — clicking either of those after a Remove would silently resurrect the
"removed" target. Fixed by splicing the array too, then re-rendering, the same DOM/array-sync
discipline every other mutation already follows. Found by re-reading the actual control flow,
not by any automated check.

17 new Go tests (`internal/resume/target_test.go` + `community_tools_test.go`), all passing —
see `IDUNA_PRO` commit `8bc4889` for the full backend writeup. `renderResumeTemplate`
(§4c below) was also run for real against sample data (not just `node --check`), confirmed to
produce correctly-escaped, complete output for both templates and to not throw on an empty
resume.

**Real default-state fix, 2026-09-09 (same day, later)**: founder real-time "i removed all my
bespoke resumes, created a new bespoke resume, and i am expecting it to default to the exact
same as the master resume." "+ New bespoke resume" originally started every checkbox unchecked
— technically consistent with `Resolve`'s own literal "nothing selected means nothing shown"
contract, but the wrong default for the real, common tailoring workflow, which is subtractive
(start from everything, hide what's not relevant to THIS opportunity) rather than additive.
Fixed in console.html only (a new `allIDs()` helper) — a fresh target's checkboxes now start
with every current master Work/Education/Skill/Award/Link entry selected. `Resolve`'s own
backend contract is completely unchanged: an explicitly emptied selection still resolves to an
empty resume.

## 4c. Real preview templates, shipped same day

Founder real-time: "you should be able to change your resume from a classy looking output to a
more tech clean tech looking with a click" — followed directly. One real, semantic render
(`renderResumeTemplate`) of whichever source is selected (the master resume, cached already
from the page's own load — no extra fetch — or a saved Target's own real, resolved view via
`GET .../targets/{id}/resolved`), wrapped in one of two real, distinct CSS classes
(`.resume-tpl-classic` — serif, centered, traditional; `.resume-tpl-tech` — sans-serif,
left-aligned, a colored accent) switchable instantly by clicking either template button, no new
request needed to switch since the underlying data is already cached client-side.

Every real, user-controlled piece of text goes through `esc()` into a plain text-content
position — verified for real, not just asserted: `renderResumeTemplate` was extracted and run
directly against sample data containing real HTML-special characters (`<`, `>`, `&`, `"`) in
the name/label/summary fields, confirming the raw characters never appear unescaped in the
output.

Real, honest, not done: no PDF/print-specific stylesheet tuning beyond the two screen templates
themselves (a browser's own "Print" / "Save as PDF" works today, untested for real layout
quality); no way to save a chosen template preference per Target yet (the template choice is
session-only, reset on reload).

## 4d. Real PDF export (Layer 3), shipped 2026-09-09

Closes the real gap §3/§4c/§5 all named honestly since this feature first shipped: a real,
downloadable, ATS-safe PDF FILE, not just a screen preview. `internal/resume/pdf.go`'s
`RenderPDF(r *Resume) ([]byte, error)` — one deliberately plain, single-column,
real-selectable-text (never an image) layout, built on `github.com/go-pdf/fpdf` (the maintained
`jung-kurt/gofpdf` fork), a real, pure-Go, no-cgo library matching this whole repo's own
established no-cgo discipline. Real, named limitation: the built-in Arial core font only covers
Windows-1252 — non-Latin scripts (CJK, Cyrillic) degrade gracefully (drop/render as `?`), not a
silent corruption of the rest of the page; a real embedded-TTF-font pass would be needed to close
that gap fully, not attempted here.

Two real routes: `GET /api/v1/community-tools/resume/export.pdf` (the master resume) and
`GET /api/v1/community-tools/resume/targets/{id}/export.pdf` (a resolved Target's own real,
filtered view) — both `community-tools.access`-gated like every other route in this feature,
both served through one shared `writeResumePDF` helper. Real, deliberate, found-before-shipping
caution: a Target's own real, user-controlled `Name` field going straight into an HTTP response
header (`Content-Disposition`'s filename) is a real header-injection/malformed-response risk —
the same class of bug this feature's own console.html work already found and fixed once
(`esc()` spliced into an HTML attribute, §4a). Closed here by `pdfFilename`, a narrow allowlist
sanitizer (letters/digits/hyphen/underscore only, everything else collapsed to a hyphen, fixed
`.pdf` extension always appended) — live-verified against a target literally named
`Kitchen Jobs "Special"`, confirming the downloaded `Content-Disposition` header stays
well-formed (one real, correctly quoted filename) rather than breaking.

Real, downloadable buttons in `console.html`: "Download PDF" in the Preview & templates card
(downloads whatever's currently selected — master or Target), and a real, per-card "Export PDF"
button alongside each Target's own Clone/Preview/Verify/Remove actions. Both fetch the PDF as a
blob through the caller's own real bearer token (a plain `<a href>` can't carry it), then trigger
a real browser download via a throwaway object URL — the filename used is whatever the server's
own `Content-Disposition` header actually said, not reconstructed client-side.

Live-verified end to end against a real, fresh-SQLite-booted `idunapro` binary (not just
`go test`): register → flip `is_community_tools_enabled` → log in for a token carrying the real
`community-tools.access` permission → save a real resume → `GET .../export.pdf` returns a real,
OS-recognized (`file(1)`-confirmed) single-page PDF with the correct `Content-Type`/
`Content-Disposition` headers; the quoted-target-name case above confirmed live too, not just in
the unit test suite.

## 4e. Real agent-ergonomic API primitives + OpenAPI spec, shipped 2026-09-09

Founder real-time: "ensure that all of the features we have have good api because i am going to
ask agents to work with those primitives to start intelligently managing the resume using
agentic ai." Every route from §4-4d used whole-document PUT/whole-list PUT only -- an agent
making one small edit had to fetch, mutate, and resend everything.

Real, new, additive primitives (the original whole-document routes are unchanged, still there):
`PATCH /resume/basics` (partial Basics merge); `POST /resume/{work,education,skills,awards}`
(create one entry); `PATCH`/`DELETE /resume/{section}/{id}` (update/remove one entry); the
identical shape for Targets (`POST /resume/targets`, `PATCH`/`DELETE /resume/targets/{id}`).
One real, shared generic implementation (`CommunityToolsEntryHandler[T]` + `entryOps[T]`) serves
Work/Education/Skill/Award/Profile instead of five hand-duplicated handlers.

PATCH relies on `encoding/json`'s own real "unmarshal onto an already-populated value" merge
semantics: a JSON key absent from the request body leaves that field untouched; a key present
(including an explicit `""` or `null`) overwrites it. For Target's `summary_override`/
`label_override` specifically this gives a genuine three-way distinction -- absent (unchanged),
`null` (explicitly cleared), or a real value (set) -- verified directly in tests, not just
asserted.

Real, found-and-fixed-before-shipping bug: the first draft of `main.go`'s wiring built the four
entry handlers with only `DB`/`Prefix` set, leaving `Ops` a zero-value struct of nil functions --
compiles fine, panics the moment any entry is actually touched. Fixed with real, exported
per-type constructors (`NewCommunityToolsWorkHandler` etc.) that fully wire everything together,
so main.go can never again construct one of these half-built.

New `GET /api/v1/community-tools/openapi.json` -- a real, complete OpenAPI 3.0 document (`go:embed`,
genuine JSON, not YAML served under a misleading `.json` URL) describing every route, so an
agent can bootstrap against this API without reading Go source. Deliberately public (no auth) --
it describes the shape of the API, not any caller's own data, the same reasoning `JWKSHandler`
already applies to its own public key set.

## 4f. Real multi-GitHub-links support ("Links" section), shipped 2026-09-09

Founder real-time: "we need to be able to add and configure the output of multiple github
links." Real, checked-first finding: `basics.profiles` existed in the JSON Resume data model
since this feature's very first pass, but had zero UI, zero PDF rendering, and zero screen-
preview rendering -- and no stable per-entry `id`, so it couldn't be selected into a bespoke
Target either. A real, silent, three-way gap, closed the same way Skills' own gap was closed
earlier the same day.

- `Profile.ID` (new, mirrors `Work.ID`/etc.) -- multiple entries sharing the identical
  `network` value (e.g. several distinct GitHub repo links) is real and expected; the id, not
  the network label, is what makes them independently addressable.
- `Target.IncludedProfileIDs` + `Resolve` now filters Profiles the same way it filters
  Work/Education/Skill/Award.
- `RenderPDF` and console.html's `renderResumeTemplate` both now render a real "Links" line
  under the contact info (`Network: address`, address-less entries skipped) -- previously
  rendered nowhere in either output.
- A new "Links" section in console.html's Resume editor (network label + URL, add/remove rows,
  matching the existing Work/Education/Skills row-editor convention exactly) and a "Show links"
  checklist in the Bespoke Resumes card, so a Target can show a different subset of links per
  opportunity.
- `NewCommunityToolsProfilesHandler` reuses the exact same generic `CommunityToolsEntryHandler[T]`
  machinery from §4e -- `POST`/`PATCH`/`DELETE /resume/profiles[/{id}]` -- and is documented in
  the OpenAPI spec alongside the other four entry types.

Real, honest verification beyond `go test`: `renderResumeTemplate`'s new links line was
extracted and run directly in real Node (not just `node --check`) against sample data
containing an XSS-shaped payload in a profile URL and two same-network GitHub links, confirming
the payload renders escaped (not raw), both links render independently, and an address-less
profile is skipped entirely -- the same discipline this file's own `esc()`-into-an-attribute
finding established earlier in this feature's history.

## 4g. Real quick-add for skills, shipped 2026-09-09

Founder real-time: "ok we need a way to quick add skills via a comma separated list." A new
"Quick add" field above the Skills list — paste or type a comma-separated list, hit Add (or
press Enter), and each word becomes its own real skill row through the same
`addResumeSkillRow`/`makeResumeEntryRow` machinery "+ Add skill" already uses one at a time. No
new save path or backend change — reuses the existing whole-document PUT exactly as before.
Skips anything that case-insensitively matches a skill already on the page, including
duplicates within the same paste. Verified directly: the parsing/dedup algorithm was extracted
and run in real Node against comma/whitespace edge cases, all pass.

## 4h. Real "Compact" template + candidate name/timestamp in exports, shipped 2026-09-09

Founder real-time: "add a new output template compact that manages to get the experience and
education like into 2 columns or something so we can get more skills on the page and keep it 1
page" / "the downloaded resume should include the candidate name and the export timestamp."

`resume.RenderPDF` now takes a `template` argument. "classic" (unchanged) stays the one plain,
single-column, ATS-safe layout. "compact" is a new, real, deliberately OPT-IN layout: Experience
and Education render in two manually-positioned columns (a real `colState` helper tracks each
column's own x/width/y cursor, since fpdf's own newline handling always resets X to the page
margin, never an arbitrary column start), freeing vertical space for a fuller Skills section.
Named honestly, not hidden: this is a real trade-off against the same ATS multi-column parsing
risk "classic" exists specifically to avoid — choose "classic" for an ATS submission, "compact"
for a human reviewer or a printed copy where density matters more.

Every generated PDF (both templates) now carries a real footer via fpdf's own `SetFooterFunc` —
the candidate's name and the exact export timestamp, repeating on every page. The downloaded
filename also now includes the candidate's real name plus today's date, not just a generic
"resume.pdf"/target-name-only filename.

Real, direct verification beyond `go test`: decompressed the actual public `RenderPDF("compact")`
output's own FlateDecode content stream and confirmed "Experience" and "Education" text render
at genuinely different, non-overlapping x-coordinates with a real gutter between them — not just
that it returns without error.

New "Compact" button in console.html's Preview & templates card (a real CSS grid screen
approximation of the PDF's own two-column layout, not pixel-matched — see §5's own updated note
below). Both PDF download paths (the Preview card's button and each Target's own inline "Export
PDF" button) now pass the currently-selected preview template through as a real query param.

**Header layout refined, 2026-09-10**: founder real-time, "can we shift the contact info and
links to the right (right align) and the name and headline to the left so they can free up just
a bit more vertical space on the compact template?" The Compact PDF header now puts name+label
left-aligned and contact+links right-aligned on two shared rows, instead of four separate
centered lines — a real, direct vertical-space saving, applied only to Compact (Classic's own
header is completely unchanged). The Compact screen preview gets the matching real flex-row
treatment. Verified directly: a new test parses the actual PDF text-position operators to
confirm the real left/right placement, not just that the strings appear somewhere on the page.

## 4i. Real auto-sort for Work/Education/Awards, fixed 2026-09-09

Kanban card CVB-12434 (the real IDUNA priority queue, position 0), founder real-time: "the work
history needs to auto sort i put a new one 2006-present and it went to the bottom of the resume
instead of the top." `resume.SortByRecency` reorders Work/Education/Awards to the real, standard
most-recent-first convention every time the resume is saved — a currently-ongoing entry (no end
date) always sorts first, ahead of anything already ended. Wired into `saveResume`, the one real,
shared choke point every write path (whole-document PUT and every single-entry
POST/PATCH/DELETE) already reduces to, so this is guaranteed regardless of which API surface made
the edit. Live-verified: the real, existing production data for a real account contained an
entry closely matching the exact reported scenario, previously buried near the bottom — re-sorted
using the same shipped function, confirmed it now ranks near the top.

## 4j. Real auto-linking + markdown links in the summary, shipped 2026-09-10

Founder real-time: "can we add auto linking to the email and the links on the exports also can
we allow for markdown in the summary so that we can have hyperlinks there too?"

**Auto-linking**: the email address becomes a real `mailto:` link; each profile entry with a
real URL becomes a real, clickable link — a bare `github.com/x/y`-style entry (no scheme typed)
gets a real `https://` prefix added so it's actually clickable. A username-only profile entry
has no real URL to link to, so it stays honest plain text rather than a guessed-at link. Applied
consistently in both the PDF (classic and compact headers) and the screen preview (all three
templates).

**Markdown links in the summary**: a real, narrow `[text](url)` subset — no bold/italic/headers,
a deliberate v0 boundary. In the PDF this uses fpdf's own `Write`/`WriteLinkString` (the real
primitives for mixing plain and linked text within one flowing paragraph); deliberately scoped
to `basics.summary` only, not Work-entry summaries, since those primitives aren't column-width-
aware and would overflow the compact template's own two-column layout.

**Real, named security measure**: a shared `safeHref` (mirrored by hand between the Go and JS
implementations) rejects `javascript:`/`data:`/`vbscript:`/`file:` schemes outright — neither a
profile link nor a markdown-summary link can ever become a real, clickable dangerous-scheme
link; it renders as its own literal, un-linked text instead. Verified with real XSS-shaped test
payloads on both sides (a literal quote in a URL, HTML in a link's display text, a `javascript:`
scheme) — confirmed no attribute breakout and no dangerous scheme ever becomes a real link.
Live-verified against real production data: the real, existing account's own real email and
GitHub profile URL both correctly became real, clickable links in the actual generated PDF.

## 5. Real, honest, not done

- No real DOCX export — PDF (Layer 3) is the only real exported file format; a separate,
  un-attempted follow-up if ever needed.
- No pixel-level visual parity between the exported PDF and any of the three web preview
  templates (Classic/Clean Tech/Compact) — each of the PDF's two real layouts (classic/compact)
  and each of the three screen styles is its own real, independent rendering, not a shared
  template engine; "Compact" in both places pursues the same real two-column idea, not identical
  output.
- Per-entry (not just per-target) text overrides — "even different little summary texts" is
  satisfied at the Basics (summary/headline) level only, not per-work-entry, named honestly as
  a real, separate, smaller possible extension if ever needed.
- Markdown links are supported in `basics.summary` only, not Work-entry summaries (a real,
  deliberate scope boundary — see §4j) — a real, separate follow-up if ever needed, blocked on
  a column-width-aware version of fpdf's own `Write`/`WriteLinkString` primitives for the
  compact template specifically.
- Section-label vocabulary advisory checks (real ATS advice about non-standard headers) named in
  the original scoping pass but not implemented — a real, separate, smaller follow-up to Layer 2.
- No live browser verification of the new console.html UI (see §4a) — syntax/structure checked
  directly, a real click-through was not.
- The Links editor UI (§4f) exposes only `network`/`url` fields, not `username` — the data
  model and API support it (a link can still be saved with just a username via the API), but
  the console.html form has no separate field for it; a real, small, later addition if needed.
