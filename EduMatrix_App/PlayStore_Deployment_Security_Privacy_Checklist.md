# Security and Privacy Checklist for Google Play Store Deployment

To successfully deploy AuraFace App to the Google Play Store, you must comply with Google's strict privacy and security guidelines. We've updated your app's code to improve security. This document outlines the remaining steps.

## 1. What We Just Fixed in Code

*   **Exported Components Security:** Updated `NotificationActionReceiver` in `AndroidManifest.xml` to `android:exported="false"`. This prevents malicious apps on the device from exploiting it by sending fake notification intents.
*   **Disabled Cloud Backups:** Set `android:allowBackup="false"` in `AndroidManifest.xml`. This ensures any sensitive data stored locally (like encrypted preferences or login tokens) is not backed up to a user's Google Drive where it could potentially be extracted or restored onto an unauthorized device.
*   **ProGuard/R8 Obfuscation:** Your `build.gradle.kts` already has `isMinifyEnabled = true` and `isShrinkResources = true` under the `release` build type. This ensures your code is obfuscated, making it much harder for attackers to decompile and reverse-engineer your app. The `proguard-rules.pro` file is correctly configured to keep API models.
*   **Secure Storage:** The app correctly uses `androidx.security:security-crypto` for EncryptedSharedPreferences (which encrypts data before storing it locally).

## 2. Actions Required Before Deployment

### A. Network Security Config Optimization
Your `network_security_config.xml` allows cleartext (HTTP) traffic for `10.113.156.225` and `localhost`. 
*   **Production Requirement:** Google Play Store highly encourages **HTTPS**. When you migrate your backend server to production, ensure it supports HTTPS. You can then remove those cleartext exceptions from `network_security_config.xml` and rely purely on the secure `base-config`.
*   If your production server still uses HTTP, update the `<domain>` tag with your actual production domain/IP, but expect warnings from the Play Console.

### B. Generate a Privacy Policy (Required)
Google requires a prominent Privacy Policy URL uploaded on the Play Console because your app requests sensitive permissions like **CAMERA** and **LOCATION**.

Your Privacy Policy must publicly state:
1.  **What data is collected:** State that the app accesses the Camera (for face matching) and Location (for geofencing/attendance).
2.  **How it handles Face Data:** Deeply explain how the facial recognition data is processed—whether it's processed on-device (via MLKit) or sent to your server, and how long it is stored. Google is extremely strict about biometric/facial data.
3.  **Third-Party Services:** Disclose the use of Firebase (for push notifications) and how it handles device tokens.
4.  **Data Deletion Policy:** Explain how users can request the deletion of their data.

*(Host this privacy policy on a free site like GitHub Pages, Google Sites, or your own domain, and link it in the "App Content > Privacy Policy" section of the Google Play Console).*

### C. Complete the "Data Safety Form" on Play Console
When you upload your app bundle (`.aab`) to the Play Console, you must manually fill out the **Data Safety** section under **App Content**. Ensure your answers match your app's actual behavior:

*   **Location:** Accesses "Approximate Location" and "Precise Location". Is this data shared with third parties? (Probably "No"). Is it collected? ("Yes, for App Functionality" - Attendance).
*   **Photos and Videos / Face Data:** You use the Camera. State whether photos are stored or simply processed in real-time.
*   **Device or Other Identifiers:** Firebase Cloud Messaging uses device IDs for notifications. You must declare this under "Device or other identifiers" (Collected -> App Functionality -> Not Shared).

### D. App Signing Keystore
Make sure to build a **Signed App Bundle** (.aab). Do not lose the `.jks` or `.keystore` file, and keep the passwords safe. Without this keystore, you cannot upload future updates to your app.

### E. Location Permission Justification
Because you request `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`, if you ever decide to request **Background Location** (`ACCESS_BACKGROUND_LOCATION`), you will require a rigorous justification video to Google demonstrating why background location is the core feature of the app. Try to rely only on foreground location checks (when the app is open) to simplify the Play Store review process.

## Summary Checklist
- [x] Secured Android Manifest Components 
- [x] Disabled Cloud Backups for Sensitive Data Protection
- [x] Validated ProGuard Rules for Obfuscation
- [ ] Ensure backend connection uses HTTPS (or update `network_security_config` domains)
- [ ] Create and host a clear Privacy Policy
- [ ] Complete the Data safety form thoroughly on Play Console
- [ ] Build a Signed Release App Bundle (.aab)
