# Implementation Plan - Add Preview for CaNhanScreen

Add a `@Preview` composable for `CaNhanScreen` in `CaNhanScreen.kt`. This involves extracting the UI logic into a stateless `CaNhanScreenContent` composable to allow for easy previewing with sample data.

## Proposed Changes

### UI Components

#### [MODIFY] [CaNhanScreen.kt](file:///C:/Users/Laptop K1/AndroidStudioProjects/APPGDCT7.9/app/src/main/java/com/example/ui/screens/CaNhanScreen.kt)
- Extract the core UI logic from `CaNhanScreen` into a new `CaNhanScreenContent` composable.
- `CaNhanScreenContent` will take state variables (`userDoc`, `userExamResults`, etc.) and event lambdas as parameters.
- Update `CaNhanScreen` to collect state from `AppViewModel` and pass it to `CaNhanScreenContent`.
- Add `CaNhanScreenPreview` at the bottom of the file using `MyApplicationTheme` and sample data.

## Verification Plan

### Manual Verification
- Render the `CaNhanScreenPreview` in Android Studio's Compose Preview panel to verify the UI looks correct for both guest and authenticated states.
