# CarePyre — Style Guide

**Status:** v1, founder-directed pivot away from the dark EINHORN_INDUSTRIAL "stark" aesthetic.
Founder, real-time: *"very health care vibe a little more friendly than the einhorn stark site
light blue"* → *"and white"* → *"cyan and white"* → *"and orange."*

CarePyre is a nonprofit/community trust, not a game studio or a signal-intelligence product —
it needs to read as trustworthy, calm, and human on first look, the way a real clinic or
community health center's site does, not as a tech-company dark-mode dashboard. This is a
deliberate, explicit break from the rest of the monorepo's visual language.

---

## Color Palette

| Role | Color | Hex | Use |
|---|---|---|---|
| Background | White | `#FFFFFF` | Primary page background |
| Background, secondary | Pale Blue Tint | `#EAF6FB` | Section backgrounds, card fills, alternating stripes |
| Primary Blue | Sky Blue | `#3FA9DC` | Headings, primary buttons, link color |
| Primary Blue, dark | Deep Sky Blue | `#1D7EA6` | Hover states, body headline text, high-contrast pairing with white |
| Accent Cyan | Cyan | `#22D3EE` | Secondary accents, icons, dividers, highlight underlines |
| Accent Orange | Warm Orange | `#FF8A3D` | Calls to action, the one warm color in an otherwise cool palette — used sparingly, only for things the page wants a visitor to *do* (buttons, "Get Involved") |
| Text, primary | Near-Black | `#1A2733` | Body copy — not pure black, keeps the page feeling soft rather than harsh |
| Text, muted | Slate Gray | `#5B6B76` | Secondary copy, captions |
| Border | Pale Blue-Gray | `#D6E8F0` | Card borders, dividers |

**Rule of thumb**: the page should read as 80% white/pale-blue, ~15% sky blue, ~5% orange. Orange
is a call-to-action color, not a background or a decoration — if more than one or two things on a
given screen are orange, that's too many.

## Typography

- System font stack (no external font loading): `-apple-system, BlinkMacSystemFont, "Segoe UI",
  Helvetica, Arial, sans-serif` — matches how other static pages in this monorepo are built, no
  extra network requests, fast on low-end devices (the exact audience this site is for).
- Headlines: bold, deep sky blue (`#1D7EA6`) or near-black, generous line-height, never all-caps
  (all-caps reads cold/corporate — the opposite of the "friendly" direction).
- Body copy: near-black on white/pale-blue, comfortable line-height (1.6+), never smaller than
  16px for accessibility (real audience includes older and vision-impaired users).

## Tone / Voice

- Warm, direct, plain language. No jargon, no "leverage synergies" corporate-speak.
- First person plural ("we," "our") when describing CarePyre's own actions; second person
  ("you," "your") when describing what a visitor/participant gets.
- Confident but not salesy — this is a real nonprofit/community trust, not a startup pitch deck.
- **Traditional charities and nonprofits are partners, not a foil.** Founder, 2026-08-10, after
  reviewing a draft that read as dismissive of donation-based/legacy charity models: "traditional
  charity are our partners and it kind of shits on them" → "we are going to bring them forward
  into the future" → "pls ensure our partnerships are good." Never frame CarePyre's
  infrastructure-first approach as superior to or in opposition to existing charitable work
  (avoid language like "legacy systems extract," "not just a donation," "not extracted from
  them" — all real phrasing caught and removed from the first landing-page draft). The correct
  frame: CarePyre builds infrastructure *underneath and alongside* the nonprofits, congregations,
  and community organizations already doing this work — modernizing and supporting that work,
  not replacing or shaming it.

## Components

### Primary button (call to action — orange, use sparingly)
```html
<a class="btn btn-primary" href="#">Get Involved</a>
```
```css
.btn-primary {
  background: #FF8A3D;
  color: #FFFFFF;
  padding: 0.85rem 1.8rem;
  border-radius: 999px;
  font-weight: 600;
}
```

### Secondary button (blue outline)
```css
.btn-secondary {
  background: transparent;
  border: 2px solid #3FA9DC;
  color: #1D7EA6;
  padding: 0.8rem 1.7rem;
  border-radius: 999px;
  font-weight: 600;
}
```

### Card (pale blue fill, soft border)
```css
.card {
  background: #EAF6FB;
  border: 1px solid #D6E8F0;
  border-radius: 16px;
  padding: 1.5rem;
}
```

### Section divider accent
A thin cyan rule (`#22D3EE`, 3px) under section headings — the one place cyan gets used
structurally rather than decoratively.

---

## What changed from the first draft

The first pass (now discarded) used a dark charcoal background with an orange/gold "ember and
ash" fire theme, matching the "Pyre" half of the name literally. Founder redirected explicitly:
lighter, healthcare-friendly, blue/white primary with cyan and orange as accents — the "Care"
half of the name should read first, not the "Pyre" half. This file is the source of truth for the
new direction; `index.html` gets rebuilt against it next.
