# Avyukt Gym (Android + Web Showcase)

Native Android application built with Kotlin, Jetpack Compose, Material 3, Navigation Compose, Jetpack Credential Manager (Google Sign-In), and Cloud Firestore — plus a static Vercel-ready public showcase website in `public/`.

## Android App Features

- **Public Gym Showcase**:
  - Hero branding with live open/closed gym status (`5:00 AM – 10:00 AM` & `5:00 PM – 10:00 PM`, Monday–Saturday; closed Sundays) and live second-by-second clock.
  - Morning & evening batch timing cards and weekly schedule chips.
  - Monthly membership fees card (`₹500/month`) with included perks.
  - One-tap WhatsApp join and phone dialer actions (`+91 99907 15771`).
- **Owner Admin Panel (Android APK)**:
  - Google Sign-In authentication via Credential Manager synced with Cloud Firestore (`Offline Saved / Auto-Sync` indicator).
  - **Dashboard**: Total members, today's attendance, collected/pending fees, Fees Due / Expiry Alert card, 6-month Revenue / 7-day Attendance / Batch Split analytics charts, and CSV export (`Fees`, `Attendance`, `Members Backup`).
  - **Members**: Add/Edit members (name, 10-digit phone, joining date, Morning/Evening/Both batch), batch filter chips, search, call, delete, and Member History sheet.
  - **Attendance**: Date picker, batch filter (`All`, `Morning`, `Evening`), individual toggles, batch-aware Mark All Present/Absent, and monthly counts.
  - **Fees**: Month switcher, `All / Fees Pending / Fees Paid` filters, Paid/Pending toggle, branded PDF Fee Receipt share, and WhatsApp fee reminders/reports.

## Deployment Notes

- **Android App (Full Admin Panel + Gym Showcase)**: Export the APK/AAB from Google AI Studio or build `:app` with Gradle / Android Studio.
- **Vercel (Public Web Showcase)**: `vercel.json` serves the static public showcase from `public/` (`https://<your-project>.vercel.app`), so pushing this repository to GitHub deploys cleanly on Vercel without failing on the Android Gradle files.
