# Financial Health Auditor

A native Android app that reviews a user's transaction history for fraud, duplicate subscriptions, billing errors, spending anomalies, and cash-flow risk — then drafts the resolution (a cancellation email, a dispute letter, or a corrective budget plan) for the user to review and send.

## How it works

The user adds transactions by pasting message text or photographing a bill (OCR via ML Kit) — no background SMS access or bank login required. Every transaction is stored locally and folded into a running behavior profile per merchant/provider. Detection modules compare new activity against that profile; anything unusual gets flagged, explained in plain language, and paired with a ready-to-edit resolution draft. The user approves or dismisses each flag with a swipe.

## Tech stack

- Kotlin + Jetpack Compose (native Android, min SDK 26)
- Room (local SQLite database — all data stays on-device)
- ML Kit Text Recognition (bill photo OCR)
- LLM API (explanation generation + resolution drafts)
- *(Phase 2)* PDF export library, Firebase Auth if real accounts are added later

## Detection modules

| # | Module | What it checks | Output fields |
|---|--------|-----------------|----------------|
| 1 | Fraud Detection | Transactions that break the user's normal pattern (amount, time, merchant) | Merchant, amount, date, AI explanation, confidence score |
| 2 | Duplicate Subscriptions | Overlapping or forgotten recurring charges | Service name, overlap detected, cancellation email draft |
| 3 | Bill Error Detection | Meter-reading continuity + amount vs. historical average | Previous bill, current bill, expected amount, difference |
| 4 | Spending Anomalies | Shifts in everyday spending behavior | Category comparison chart, plain-language explanation (e.g. "you spent 42% more on food this month") |
| 5 | Cash Flow Prediction | Predicted shortfalls before they happen | Income, expenses, expected balance, risk level, 30-day forecast graph |

Users choose which modules are active from the Choose Modules screen; disabled modules don't run at all — no computation, no flags, no false positives from a check the user didn't ask for.

## Full app flow (target vision)

1. **Splash Screen** — logo, "Financial Health Auditor," tagline
2. **Onboarding** (3 screens) — hidden risks / AI analysis / ready-to-send solutions
3. **Login / Sign-up** — Email, Google Sign-in, or Continue as Guest
4. **Home Dashboard** — 5 module cards (Fraud, Duplicates, Bill Errors, Spending, Cash Flow); bottom nav: Home / Upload / Reports / AI Assistant / Profile
5. **Upload Data** — paste message, photograph bill, upload bank statement (CSV/PDF), connect bank *(future)*, or use sample data → "Start Financial Audit"
6. **Processing Screen** — animated checklist (reading transactions → detecting fraud → checking subscriptions → comparing bills → forecasting cash flow → generating recommendations)
7. **Audit Result Dashboard** — summary cards per module + overall Financial Health Score (e.g. 87/100)
8. **Module Detail Screens** — one per module, each with its own fields and action button (Generate Dispute Letter / Cancel Subscription / Generate Complaint Email, etc.)
9. **AI Resolution Center** — tabs for Dispute Letters / Cancellation Emails / Budget Plan; edit, copy, download PDF, share
10. **Personalized Budget Screen** — income, necessary expenses, savings goal, recommended limits, charts
11. **Reports Screen** — monthly report (health score, risk score, money saved, issues, recommendations), export/share PDF
12. **AI Chat Assistant** — chatbot answering questions grounded in the user's own flagged data
13. **Profile & Settings** — personal info, connected accounts, notifications, privacy, theme, help & feedback

## Build phases

**Phase 1 — MVP (current focus, ~6 weeks)**
Splash → Choose Modules → Add Transaction (paste/photo, sample data) → Flagged Issues (swipe to resolve) → Review Issue (explanation + editable draft) → Monthly Summary. Guest entry only, no real accounts. One module built and proven end-to-end at a time, starting with Duplicate Subscriptions. See `Phase1_Walkthrough_Setup_to_Module2.md` for exact build steps and prompts.

**Phase 2 — Full vision (after MVP works)**
- Onboarding screens (cosmetic, cheap to add anytime)
- Real Login/Google Sign-in *(requires Firebase Auth + a reason to need accounts — reconsider if the app stays local-only)*
- Bank statement CSV/PDF upload, "Connect bank" *(needs its own scoping — open banking access is a significant addition, not a quick one)*
- Processing-screen checklist animation
- Financial Health Score formula (needs its own weighted-scoring design pass)
- AI Resolution Center tabs + PDF export/share
- Personalized Budget screen with charts
- Reports screen with PDF export
- AI Chat Assistant *(build after all 5 modules produce solid explanations — the chatbot's answers are only as good as the data it can pull from)*
- Profile & Settings (theme, notifications, privacy, help/feedback)

## Project structure

```
com.yourname.financialhealthauditor/
  data/          Room entities & database (Transaction, BillHistory, BehaviorProfile)
  detection/     One detector class per module
  ai/            Explanation and resolution-draft generation
  ui/
    screens/     One composable screen per app screen
    components/  Reusable UI pieces (cards, chips, etc.)
  utils/         Text parsing (paste + OCR input → Transaction)
```

## Setup

1. Clone the repo
2. Open in Android Studio (or via Antigravity)
3. Add your LLM API key to local secrets (never commit this — see `.gitignore`)
4. Build and run on an emulator or device (min SDK 26)

## Status

In progress — see commit history and `Phase1_Walkthrough_Setup_to_Module2.md` for the current build stage.

## Notes on distribution

This project is built for local sideloading (direct APK install), not Play Store distribution, to keep the input methods (manual paste + photo) simple and free of restricted permissions.