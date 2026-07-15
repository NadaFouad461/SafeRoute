# Implementation Plan - Enhanced Fake Call with Profile Contacts

The user wants the "Fake Call" feature to use contacts from their profile (emergency contacts) and to improve the overall service logic.

## Proposed Changes

### UI & UX Improvements
#### [MODIFY] [fragment_fake_call.xml](file:///home/abdulrhmanmustafa/AndroidStudioProjects/SafeRoute/app/src/main/res/layout/fragment_fake_call.xml)
- Keep the current structure but prepare it for dynamic chip loading.
- Add a loading state (ProgressBar) while fetching contacts from Firestore.

#### [NEW] `activity_fake_incoming_call.xml`
- A new layout that simulates a real incoming call screen.
- Features:
    - Caller Name (from intent)
    - Animated "Accept" and "Decline" buttons.
    - Full-screen UI with dark background.

### Logic Improvements
#### [MODIFY] [FakeCallFragment.kt](file:///home/abdulrhmanmustafa/AndroidStudioProjects/SafeRoute/app/src/main/java/com/example/saferoute/ui/fakeCall/FakeCallFragment.kt)
- **Fetch Contacts**: Fetch emergency contacts from Firestore (`users/{uid}/contacts`).
- **Dynamic Chips**: Dynamically create chips for the emergency contacts so the user can quickly select one.
- **Improved Preview**: Update the live preview card when a contact is selected or manual name is typed.

#### [MODIFY] [FakeCallService.kt](file:///home/abdulrhmanmustafa/AndroidStudioProjects/SafeRoute/app/src/main/java/com/example/saferoute/services/FakeCallService.kt)
- **Trigger Actual Call Screen**: Instead of starting `FallAlertActivity` (which is for SOS), it will now start the new `FakeIncomingCallActivity`.

#### [NEW] `FakeIncomingCallActivity.kt`
- An Activity that plays a ringtone and shows the fake call UI.
- Handling "Accept": Stops ringtone, simulates a call timer, and allows hanging up.
- Handling "Decline": Stops ringtone and finishes the activity.

### Manifest
#### [MODIFY] [AndroidManifest.xml](file:///home/abdulrhmanmustafa/AndroidStudioProjects/SafeRoute/app/src/main/AndroidManifest.xml)
- Register `FakeIncomingCallActivity` with appropriate flags (`showOnLockScreen`, `turnScreenOn`).

## Verification Plan

### Manual Verification
1.  Open "Fake Call" screen.
2.  Verify that emergency contacts from the profile appear as selectable chips.
3.  Select a contact and verify the "Caller Identity" field and "Live Preview" update.
4.  Schedule a call (e.g., 5 seconds).
5.  Wait for the call and verify that the NEW `FakeIncomingCallActivity` appears.
6.  Verify that a ringtone plays.
7.  Test "Accept" and "Decline" actions.
