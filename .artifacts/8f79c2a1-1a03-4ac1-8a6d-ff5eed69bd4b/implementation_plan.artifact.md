# Implementation Plan - Refine Back Button in Safe Walk

Improve the back button design in the Safe Walk screen and implement its functionality in the fragment.

## Proposed Changes

### 1. Layout Refactoring

#### [MODIFY] [fragment_safe_walk.xml](file:///home/abdulrhmanmustafa/AndroidStudioProjects/SafeRoute/app/src/main/res/layout/fragment_safe_walk.xml)
- Use the standard `app:navigationIcon` in the `MaterialToolbar` instead of the custom `ImageButton`. This ensures proper alignment, ripple effects, and consistency with other screens.
- Use `@android:drawable/ic_media_previous` (or a similar standard back icon) for the navigation icon.
- Remove the `ImageButton` with id `imageButton` from the `ConstraintLayout`.
- Keep the "Safe Walk Active" title and "End Trip" button for functionality.

### 2. Logic Update

#### [MODIFY] [SafeWalkFragment.kt](file:///home/abdulrhmanmustafa/AndroidStudioProjects/SafeRoute/app/src/main/java/com/example/saferoute/ui/sensors/SafeWalkFragment.kt)
- Add a click listener for the toolbar's navigation icon in `onViewCreated`:
  ```kotlin
  binding.toolbar.setNavigationOnClickListener {
      findNavController().navigateUp()
  }
  ```

## Verification Plan

### Manual Verification
- Open the Safe Walk screen.
- Verify the back button appears on the left side of the top bar.
- Click the back button and ensure it returns to the previous screen.
- Verify the "Safe Walk Active" text and "End Trip" button are still visible and correctly positioned.
