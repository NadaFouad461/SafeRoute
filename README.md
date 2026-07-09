# SafeRoute

## Overview

This repository contains the source code for **SafeRoute**, an Android safety application designed to protect users during emergency or high-risk situations. The application addresses common personal safety scenarios such as walking alone at night, experiencing a sudden accident, or needing to alert trusted contacts immediately.

SafeRoute integrates real-time location sharing, an emergency alert (SOS) system, motion-based fall detection, and a community-informed danger zone map into a single mobile application.

## Problem Statement

Personal safety incidents often occur in situations where a user is unable to actively seek help — due to being alone, incapacitated, or under threat. Existing solutions are typically fragmented across separate apps (location sharing, emergency contacts, incident reporting) with no unified, automated response system. SafeRoute addresses this gap by combining automatic risk detection (fall detection, Safe Walk timeout) with instant alerting and live location visibility for trusted contacts.

## Features

|Feature|Description|
|-|-|
|SOS Alert|A single-tap emergency alert that transmits the user's live location and a notification to saved emergency contacts.|
|Live Location Sharing|Continuous synchronization of the user's location to Firebase Realtime Database, enabling trusted contacts to track them in real time.|
|Safe Walk Timer|A user-defined countdown for a walking trip. If the timer expires without confirmation that the user is safe, an SOS alert is triggered automatically.|
|Fake Call (Escape Call)|A simulated incoming call feature that allows a user to safely exit an uncomfortable or unsafe situation.|
|Fall and Motion Detection|Utilizes the device accelerometer and gyroscope to detect falls or abnormal movement and prompts the user for confirmation of their status.|
|Danger Zone Map|Displays previously reported incident or SOS locations on the map as markers, allowing users to identify and avoid high-risk areas.|
|Emergency Log|Maintains a historical record of past SOS events and alerts for later review.|
|Push Notifications|Firebase Cloud Messaging is used to deliver real-time emergency notifications to designated contacts.|

## Technology Stack

* **Language:** Kotlin
* **Architecture:** MVVM (ViewModel and Repository pattern)
* **Dependency Injection:** Dagger Hilt
* **Local Persistence:** Room Database (emergency log, cached user data)
* **Backend and Real-Time Data:** Firebase

  * Firebase Authentication (login and registration)
  * Firebase Realtime Database (live location synchronization)
  * Cloud Firestore (emergency logs, contacts, structured data)
  * Firebase Cloud Messaging (SOS and emergency notifications)
  * Cloud Functions (server-side SOS processing, located in `sos\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_functions/`)
* **Maps and Location:**

  * OSMDroid (map rendering)
  * Google Play Services Location (GPS tracking)
* **Sensors:** Android Sensors API (accelerometer and gyroscope) for fall detection
* **Concurrency:** Kotlin Coroutines

## Project Structure

```
app/src/main/java/com/example/saferoute/
├── data/
│   ├── local/          Room database (DAOs, entities)
│   ├── remote/         Firebase and API clients
│   └── repository/     Auth, Location, Emergency, Log, and SOS repositories
├── di/                  Hilt dependency injection modules
├── services/            Background services (location tracking, fall detection,
│                        SOS, fake call, Safe Walk)
├── ui/
│   ├── auth/            Login, signup, onboarding
│   ├── map/             Map screen, live location, danger zones, Safe Walk, fake call
│   ├── sensors/         Fall detection UI and alerts
│   ├── sos/             SOS trigger and contact management
│   ├── history/         Emergency log and user profile
│   └── start/           Splash screen, onboarding, permission requests
└── utils/               Constants, extensions, permission manager
```

## Team Responsibilities

|Area|Responsibility|
|-|-|
|Authentication and Profile|Login/registration, user profile management, emergency contact management (Firebase Authentication and Firestore)|
|Maps and Location (`ui/map`, `LocationTrackingService`, `LocationRepository`)|Map rendering, live location tracking, danger zone markers|
|SOS and Notifications|SOS trigger logic, alert dispatch, Firebase Cloud Messaging, SMS|
|Sensors and Smart Features|Fall detection, Safe Walk timer, motion monitoring|
|Database and Emergency Log|Room and Firestore integration for storing and synchronizing emergency history|
|Cloud Functions|Server-side SOS processing (`sos\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_functions/`)|

Each team member develops on an individual feature branch and merges into the shared project repository following local testing and verification.

## Getting Started

### Prerequisites

* Android Studio (latest stable release)
* JDK 11 or higher
* A configured Firebase project with the following services enabled:

  * Realtime Database
  * Cloud Firestore
  * Authentication
  * Cloud Messaging

### Installation

1. Clone the repository:

```bash
   git clone <https://github.com/AbdulrhmanMustafa-dev/SafeRoute>
   ```

2. Open the project in Android Studio.
3. Add a valid `google-services.json` file to the `app/` directory.
4. Sync Gradle dependencies and run the application on a device or emulator (minimum SDK 24, target SDK 36).

### Database Security

Realtime Database rules are currently configured for open read/write access to support active development. These rules must be restricted prior to any production deployment.

## Current Status and Known Limitations

* Danger zone markers are retrieved from Firebase and rendered on the map; however, the visual presentation (a red circular marker with a warning icon implemented as a `Polygon` overlay) has not yet been finalized to match the intended design.
* A temporary, hardcoded user identifier (`"test\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_user\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_123"`) is currently used for Firebase location paths. This will be replaced with `auth.currentUser?.uid` once the authentication module is fully integrated.
* Firebase security rules remain open for development purposes and require restriction before release.

## Roadmap

* Replace the hardcoded user identifier with the authenticated user's ID
* Finalize the visual design of danger zone markers
* Restrict Firebase security rules for production readiness
* Implement automated testing for location and SOS workflows
* Refine the onboarding and permissions request flow

## License

This project was developed as part of a team academic project. A license should be added here if the project is intended for public or open-source distribution.
