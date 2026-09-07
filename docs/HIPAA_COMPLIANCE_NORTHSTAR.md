# CarePyre — HIPAA Operational Framework (2026-09-07)

## Where this comes from

Founder real-time, 2026-09-07: "so hipaa is mostly a set of operational guidelines for data
sharing can you build that into carepyre? for example we can allow providers to create email
accounts for participants what kind of infra or disclaimers do we need? i know we need a basic
carepyre terms and privacy policy we dont look at anyones emails but if the emails are
unencrypted and we get supeonad we are gonna have to give the data over so we forcefully
encourage the pgp integration on the email side we will figure out equivalent protections on the
telecom side that isnt officially live yet but email is ready to go live at least to receive
emails."

Same honest-scoping discipline `docs/EMAIL_NORTHSTAR.md`/`docs/MESH_NETWORK_RESEARCH.md` already
established: this document names what's real, what's built, and what's explicitly deferred.
**This is not a legal compliance determination.** Nobody on this team is a lawyer, and HIPAA
compliance for any specific provider relationship depends on facts (is the provider a covered
entity, is a BAA in place, what data actually flows) that only that provider and real counsel can
settle. What this document and this pass's real, shipped work DO provide: the technical
safeguards, access controls, and disclosures a HIPAA-aware operator would want in place before
onboarding a provider who might send protected health information through CarePyre — described
honestly, not oversold as "HIPAA compliant."

## Real, shipped this pass

1. **Terms of Service and Privacy Policy** — `terms.html` / `privacy.html`, real static pages
   matching the site's own `STYLE_GUIDE.md`, linked from `index.html`'s footer and from the
   console's sign-in card. Both are explicitly marked as drafts pending real legal review (same
   "not reviewed by a lawyer yet" honesty `EMILY_FOR_BUSINESS/LICENSE.md` already models) — they
   describe our real, current practices and intent, not a finished legal instrument. The Privacy
   Policy names the exact tension the founder raised directly: we don't read your mail, but an
   unencrypted mailbox is one we can be legally compelled to produce readable under a valid
   subpoena, which is why encryption is "forcefully encouraged."

2. **A real, least-privilege "provider" role in IDUNA_PRO** (`mail-accounts.provision`,
   `IDUNA_PRO/internal/userlog`'s own `IsProvider` field + `EventUserProviderChanged`,
   migrations `202609070002_local_users_is_provider.sql`) — distinct from full `users.admin`.
   Real, checked HIPAA-relevant design decision (see `EMILY/BACKLOG.md`'s own record of this
   pass): granting every case worker/clinician full console admin (kanban, mailing list, Twilio,
   every OTHER participant's data) to let them provision one mailbox would violate the same
   "minimum necessary" principle HIPAA's own Privacy Rule already names. A provider gets exactly
   one capability: create and manage mailboxes for the participants they themselves provisioned.

3. **Minimum-necessary scoping enforced server-side, not just in the UI.**
   `MailAccountsHandler` (IDUNA_PRO) now tracks `created_by` on every linked mailbox
   (`202609070003_mail_account_credentials_created_by.sql`) and a provider-only caller's
   list/reveal-password views are filtered to mailboxes they created — never every participant
   in the system. A provider must always link a new mailbox to a participant's `local_uid`
   (create rejects an unlinked request from a provider-only caller with 400), since an unlinked
   mailbox would otherwise be invisible to them forever.

4. **Providers can create the participant identity itself, not just the mailbox.**
   `POST /api/v1/users` (IDUNA_PRO) is now reachable by `mail-accounts.provision`, not only
   `users.admin` — the real, necessary other half of "providers can create email accounts for
   participants," since a mailbox needs a `local_uid` to link to and a provider had no other way
   to get one before this pass. `createUser` itself accepts no `is_admin`/`is_provider`/`status`
   input, so this can't be used to self-escalate; `listUsers`/`updateUser`/`deleteUser` stay
   `users.admin`-only, so a provider still can't browse or manage any other participant.
   `CarePyre/console.html` gained a real "Participants" panel (gated on `mail-accounts.provision`)
   that creates both in one flow and reveals a provider's own participants' mailbox passwords.

5. **A CarePyre admin can grant/revoke the provider role** via a real checkbox in the existing
   Users admin table (`console.html`), calling the same `PATCH /api/v1/users/{uid}
   {"is_provider": true}` API surface `is_admin` already established (CP-SIP-ADMIN-124323) — no
   new grant UI pattern invented.

6. **PGP encouragement made forceful, not just present.** The underlying self-service
   encryption-at-rest capability (paste a PGP/S-MIME key, `POST /api/v1/mail/encryption`) already
   shipped 2026-09-05 — this pass changed how hard the console pushes it: a persistent,
   non-dismissible banner now shows on the Mail page itself whenever the connected mailbox is
   unencrypted (previously the only signal was inside a collapsed `<details>` on a different
   page), explicitly naming the subpoena-disclosure risk in plain language, with a one-click
   "Secure my mailbox" button that jumps to and expands the key-upload form (now open by default
   on the Account page, not collapsed). This is real encouragement, not a hard requirement —
   nothing currently blocks sending or receiving mail unencrypted, matching the founder's own
   explicit "forcefully encourage" (not "require") framing.

## Explicitly not done — real, honest gaps

- **No Business Associate Agreement template or process.** If a provider using CarePyre is a
  HIPAA covered entity, a real BAA may be legally required before they send protected health
  information through the service. This document does not draft one — that's a real legal
  document needing real counsel, not something to improvise. The Privacy Policy names this
  possibility honestly and asks providers with questions to reach out before treating an
  unencrypted account as safe for PHI.
- **No hard technical enforcement of encryption.** The founder's own explicit instruction was to
  encourage, not block — so an unencrypted mailbox remains fully functional. If CarePyre later
  decides to REQUIRE encryption for provider-managed participant accounts specifically (a
  plausible, stricter future policy), that's a real, separate, not-yet-scoped change.
- **Telecom-side (SIP/voice) equivalent protections — explicitly deferred by the founder**
  ("we will figure out equivalent protections on the telecom side that isnt officially live
  yet"). Nothing in this pass touches SIP/Asterisk call recording, voicemail, or call metadata
  handling. Voice service is not described as HIPAA-relevant-ready by this document.
- **No audit-log UI for "who accessed which participant's mailbox."** The real data exists
  (`mail_account_credentials.created_by`, the unified event log's own `iduna-auth`/`idunapro/api`
  event sources), but there is no admin-facing report surfacing it yet — a real, useful follow-up
  for demonstrating minimum-necessary access in practice, not built this pass.
- **No formal risk assessment, breach notification procedure, or workforce training program** —
  the real administrative-safeguards side of HIPAA's Security Rule, entirely outside what code
  can build. Named here as a real gap, not silently skipped.
- **Data export/deletion self-service is IDUNA_PRO's own GDPR pipeline** (see
  `IDUNA_PRO/internal/gdpr`, shipped 2026-09-07, same day as this pass) — real and working for
  IDUNA_PRO identities generally, but CarePyre's own console does not yet expose a
  participant-facing "export/delete my data" button; today that flow is contact-form-mediated
  (see Privacy Policy's own "Your rights" section).

## Related

- `docs/EMAIL_NORTHSTAR.md` — the underlying Stalwart mail server research and deployment
  decisions this document builds on top of.
- `docs/CAREPYRE_CONSOLE_NORTHSTAR.md` — the console's own architecture (IDUNA_PRO as backend).
- `IDUNA_PRO/CHANGELOG.md` (2026-09-05 entry) — the self-service PGP/S-MIME encryption-at-rest
  capability this pass's "forceful encouragement" UI sits on top of, unchanged.
- `IDUNA_PRO/internal/gdpr` — the real data export/deletion pipeline (Article 15/17/20), built
  the same day as this pass, on the general IDUNA_PRO multi-tenant platform rather than CarePyre
  specifically.
- `EMILY/BACKLOG.md` — the section logging this pass's real, completed work with commit
  references.
