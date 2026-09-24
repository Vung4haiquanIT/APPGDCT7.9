# Implementation Plan - Add Preview for ThongBaoScreen

The goal is to add a `@Preview` for the `ThongBaoScreen` Composable in `ThongBaoScreen.kt`. Since the original Composable takes a `ViewModel` parameter, I will extract the UI logic into a stateless `ThongBaoScreenContent` Composable and then create a preview for it.

## User Review Required

> [!IMPORTANT]
> The `ThongBaoScreen` will be refactored to delegate its UI rendering to `ThongBaoScreenContent`. This is a standard practice for Compose to enable Previews and improve testability.

## Proposed Changes

### UI Components

#### [MODIFY] [ThongBaoScreen.kt](file:///C:/Users/Laptop K1/AndroidStudioProjects/APPGDCT7.9/app/src/main/java/com/example/ui/screens/ThongBaoScreen.kt)
- Import `ProgressDoc` and `MyApplicationTheme`.
- Extract `ThongBaoScreenContent` from `ThongBaoScreen`.
- Refactor `ThongBaoScreen` to collect state from `AppViewModel` and pass it to `ThongBaoScreenContent`.
- Add `ThongBaoScreenPreview` at the bottom of the file with sample data.

## Verification Plan

### Automated Tests
- Call `analyze_file` to ensure there are no syntax errors.
- Call `render_compose_preview` to verify the preview renders correctly.

### Manual Verification
- N/A
