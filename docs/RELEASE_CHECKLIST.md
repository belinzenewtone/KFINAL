# Release Checklist — Go/No-Go

Run top-to-bottom before tagging a release. Every item is a gate; a single
failure is a No-Go.

## 1. Automated gates (CI must be green on the release commit)

- [ ] `./gradlew :app:detekt :sms:detekt` — zero findings
- [ ] `./gradlew :app:ktlintCheck :sms:ktlintCheck` — zero findings
- [ ] `./gradlew :sms:testDebugUnitTest :app:testDebugUnitTest` — all green
      (fixture-corpus accuracy suite included)
- [ ] `./gradlew :app:assembleRelease` — minified build succeeds

## 2. Database integrity

- [ ] Room schema JSON exported and committed (`app/schemas/`)
- [ ] No manual SQL migrations outside `LifeOsDatabase.MIGRATION_*`
- [ ] Install-over-upgrade smoke test: previous release → new build
      (verifies MIGRATION_2_3 on real data)

## 3. SMS pipeline smoke test (physical device)

- [ ] Realtime capture: send a test M-Pesa SMS → appears in ledger within seconds
- [ ] Duplicate broadcast of the same SMS → no second ledger row
- [ ] Non-financial SMS (OTP/promo) → ignored, visible in Import Health audit
- [ ] Quarantine flow: low-confidence message held → review → resolve
- [ ] Boot device → boot receiver re-arms receivers and sweep worker
- [ ] Airplane-mode historical import: bulk scan completes without OOM

## 4. Release mechanics

- [ ] `versionCode` incremented, `versionName` updated
- [ ] Signing secrets present in repo settings (RELEASE_KEYSTORE_BASE64,
      RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD)
- [ ] Tag pushed → Release workflow green → APK + `.sha256` attached
- [ ] Downloaded APK installs and launches on a clean device

## Go / No-Go

All boxes above checked → **Go**. Any failure → **No-Go**, fix, re-run from §1.
