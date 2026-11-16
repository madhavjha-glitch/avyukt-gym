# Amit Fitness Gym (Android)

Native Android application built with Kotlin, Jetpack Compose, Material 3, Navigation Compose, Jetpack Credential Manager (Google Sign-In), and Cloud Firestore.

## Features

- **Public Gym Showcase**:
  - Hero branding with live open/closed gym status (`5:00 AM – 10:00 AM` & `5:00 PM – 10:00 PM`, Monday–Saturday; closed Sundays) and live second-by-second clock.
  - Morning & evening batch timing cards and weekly schedule chips.
  - Monthly membership fees card (₹500/month) with included perks.
  - One-tap WhatsApp join and phone dialer actions.
- **Owner Admin Panel**:
  - Google Sign-In authentication via Credential Manager + 4-digit owner PIN lock screen (`1234`).
  - **Dashboard**: Real-time summary of total members, today's attendance, collected fees, and pending fees.
  - **Members**: Add members (name, 10-digit phone, joining date), search, call, and delete with confirmation.
  - **Attendance**: Date picker, individual Present/Absent toggles, bulk Mark All Present/Absent, and monthly attendance counts.
  - **Fees**: Month switcher, Pending-first sorting, one-tap Paid/Pending toggle with paid date, and WhatsApp fees report export.
