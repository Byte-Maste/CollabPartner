# CollabPartner

Production-style Android collaboration app built for real-time communication, shared activity flows, and cloud-backed user experiences using Firebase.

## Overview

CollabPartner is a native Android project focused on fast, reliable collaboration features. The app is designed to keep users synchronized in near real time using Firebase Realtime Database and Cloud Firestore, while supporting media workflows and secure authentication.

This project demonstrates practical mobile engineering skills across architecture, backend integration, performance, and UX reliability.

## Key Capabilities

- Real-time data sync across users with Firebase Realtime Database.
- Structured cloud data and feature modules using Cloud Firestore.
- User authentication and session flow using Firebase Auth.
- Cloud media workflows with Firebase Storage.
- Embedded meeting capability using Jitsi Meet SDK.
- Image loading and caching with Glide.
- Email integration using JavaMail APIs.
- ViewBinding-based UI layer for safer view access.

## Technology Stack

- Language: Java (Android)
- SDK: Android SDK 36 (min SDK 24)
- Build: Gradle Kotlin DSL
- UI: Material Components, AppCompat, ConstraintLayout
- Cloud: Firebase Auth, Realtime Database, Firestore, Storage
- Realtime Communication: Jitsi Meet SDK
- Media: Glide
- Utilities: JavaMail

## Architecture

The app follows a feature-oriented Android structure with clear separation between:

- UI components and screen logic
- Firebase data operations
- Media and communication integrations
- Shared utilities and helpers

Primary goals of the architecture:

- Fast feature iteration
- Predictable data flow
- Reliable handling of asynchronous operations
- Maintainable code for team collaboration

## Project Structure

```
RealTimeDataFirebase/
|- app/
|  |- src/main/
|  |- build.gradle.kts
|  |- google-services.json
|- gradle/
|- build.gradle.kts
|- settings.gradle.kts
|- README.md
```

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- Firebase project configured

### Setup

1. Clone the repository.
2. Open the project in Android Studio.
3. Add your Firebase config file at `app/google-services.json`.
4. Sync Gradle.
5. Run on emulator or physical Android device.

## Build and Run

```bash
./gradlew assembleDebug
./gradlew installDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

## Why This Project Stands Out

- Solves real collaboration use cases on mobile.
- Integrates multiple production-grade services in one app.
- Demonstrates practical Android + Firebase engineering depth.
- Built with scalability and maintainability in mind.

## Author

Krishna Choudhary

## Repository

https://github.com/Byte-Maste/CollabPartner

## Shipped APK (Free Download)

https://github.com/Byte-Maste/CollabPartner/releases/tag/app-demo-v1.0.0
