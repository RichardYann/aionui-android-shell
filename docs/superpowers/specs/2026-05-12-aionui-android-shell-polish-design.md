# AionUi Android Shell Polish (Design)

## Goal

Refine the current Android WebView shell so it feels more immersive and app-like without changing the existing server connection model.

This polish pass covers:

- Hidden top navigation that appears only from a dedicated top-edge pull gesture
- Navigation styling that visually blends with the loaded page
- Clear loading feedback for connection and refresh actions
- Replacing the default launcher icon with an AionUI logo from the upstream `resources` directory

Non-goals:

- No change to the saved server URL model
- No change to WebView authentication/session behavior
- No new native settings screen
- No attempt to perfectly mirror every website's dynamic styling

## Confirmed Product Decisions

- Navigation stays hidden after a page is successfully loaded
- Navigation can only be revealed by pulling down from a narrow top-edge trigger area
- Pulling from normal page content must not reveal navigation
- Revealed navigation auto-hides after a short idle period
- Auto-hide is paused while the user is interacting with navigation controls
- Navigation color should follow the current page's primary background as closely as is practical
- Loading feedback should use both:
  - a thin top progress indicator
  - a centered loading spinner for obvious wait states
- Launcher icon should use an AionUI logo sourced from `https://github.com/iOfficeAI/AionUi/tree/main/resources`

## User Experience

### Navigation Reveal

After the WebView loads successfully:

1. The full top navigation bar is hidden
2. A narrow invisible touch target remains at the very top of the app content
3. Only a downward gesture that starts inside this top trigger area reveals navigation
4. Scrolling or dragging anywhere else in the page does not reveal navigation

### Navigation Visible State

When revealed, the navigation bar provides the same existing actions:

- Back
- Refresh
- Change server
- More menu

The navigation bar then auto-hides after a short timeout when:

- the user stops interacting
- the page is not in an obvious loading state
- no menu is open

Auto-hide is delayed or paused while:

- the user is pressing a navigation control
- a menu is open
- a refresh or page load is visibly in progress

### Loading Feedback

Two loading indicators are used together for different strengths of feedback:

- Top progress bar:
  - shown for page refreshes and normal page navigation
  - lightweight and non-blocking
- Center loading indicator:
  - shown for first connection
  - shown for explicit refreshes or clearly waiting states
  - hidden once page load completes or fails

### Failure Behavior

If the page fails to load:

- the existing error overlay remains the primary recovery UI
- loading indicators are dismissed
- navigation does not auto-hide in a way that makes recovery harder

## Visual Design

### Navigation Container

The navigation bar should feel like part of the page rather than a separate Android chrome strip.

Visual rules:

- Background color follows the current page background when possible
- Foreground content adapts for contrast
- The bar should remain readable on both dark and light pages
- The bar should look intentionally immersive rather than floating or purely system-themed

### Color Strategy

Color selection uses graceful fallback rather than brittle perfect matching.

Priority order:

1. Read the page background color from the loaded document
2. If needed, inspect likely top-level containers for a usable background color
3. Fall back to a defined AionUI default surface color

Foreground text/icon color is selected from the computed background luminance:

- dark foreground on light backgrounds
- light foreground on dark backgrounds

### Launcher Icon

The launcher icon should no longer use the Android default placeholder.

Requirements:

- Source from the AionUI upstream `resources` directory
- Prefer a clean logo asset that can scale well to Android launcher sizes
- Generate Android-ready launcher resources from that source
- Keep icon appearance centered and legible on common Android launchers

If the upstream logo is not directly suitable as a launcher icon, adapt it into a square-safe launcher treatment while preserving AionUI branding.

## Technical Design

### Layout Changes

`activity_web.xml` is expanded to include:

- A narrow top-edge gesture trigger layer
- A navigation container that can animate in and out
- A thin top progress indicator
- A centered loading overlay that sits above the WebView but below the full error overlay

Layering order:

1. WebView content
2. Loading feedback overlay
3. Hidden/revealed navigation system
4. Error overlay

This preserves recovery UX when a page cannot load.

### WebActivity Responsibilities

`WebActivity` gains four focused responsibilities:

1. Gesture-gated navigation reveal
2. Navigation visibility state and auto-hide timing
3. Page load state handling
4. Page-aware color extraction and styling

To keep the activity maintainable, helper methods should separate:

- reveal/hide state logic
- loading indicator logic
- page style extraction logic
- timer reset/pause behavior

### Gesture Model

The reveal gesture is restricted to a dedicated top-edge target view rather than full-page gesture interception.

Reasoning:

- avoids conflict with normal WebView scrolling
- matches the approved product behavior
- is easier to reason about than trying to infer edge-origin gestures from all page motion events

Implementation expectations:

- the trigger zone is visually unobtrusive or invisible
- only downward drag from that trigger zone can reveal navigation
- the rest of the page continues to behave like a normal WebView

### Loading State Model

The activity should distinguish between:

- initial load
- user-initiated refresh
- ordinary page transitions
- terminal failure state

Expected mapping:

- initial load: top progress + centered loader
- explicit refresh: top progress + centered loader
- lightweight in-page navigation: at minimum top progress
- load failure: hide loaders, show error overlay
- load success: hide loaders, resume auto-hide eligibility

### Page Color Extraction

The app should not attempt screenshot-based color detection.

Recommended approach:

- evaluate document styles through WebView JavaScript after page load progress is meaningful
- inspect `document.body`, `document.documentElement`, and likely top containers
- normalize extracted values into Android color ints
- ignore transparent/invalid values and fall back safely

This keeps the implementation deterministic and lightweight.

### Icon Sourcing

Asset workflow:

1. Select a suitable AionUI logo file from upstream `resources`
2. Bring the source asset into this project in a traceable form
3. Generate Android launcher resources
4. Update manifest/app resources to use the new icon

If adaptive icon assets are feasible, prefer them. If not, a standard launcher icon set is acceptable for this pass.

## Error Handling

- Invalid or missing page colors fall back to the AionUI default surface color
- If page color extraction runs late, navigation may briefly use fallback color before updating
- If the top-edge trigger is unavailable for some layout reason, navigation must still remain operable in failure states
- If upstream logo assets are ambiguous, choose the most launcher-safe option and document the choice

## Testing Strategy

### Manual Acceptance Checks

- After successful load, the navigation bar is hidden by default
- Pulling from the top trigger area reveals navigation
- Pulling from normal page content does not reveal navigation
- Revealed navigation auto-hides after idle timeout
- Navigation remains visible while the user is interacting with it
- Refresh shows the top progress indicator
- Initial connection and explicit refresh show a centered loader
- Loading indicators disappear on success or failure
- Navigation color updates to match the page background closely enough to feel immersive
- Navigation foreground remains readable on both light and dark pages
- Launcher icon displays as AionUI branding instead of the default Android placeholder

### Regression Checks

- Existing back navigation still works
- Change server still returns to `ConnectActivity`
- More menu actions still work
- Error overlay still appears on unreachable pages
- File upload/download behavior remains unchanged

## Scope Notes

This design intentionally does not include:

- bottom navigation
- arbitrary gesture customization
- a native theme editor
- full per-site styling profiles
- server-specific branding rules beyond launcher icon and fallback theme color

## Acceptance Criteria

- Navigation is hidden by default after successful connection
- Only the dedicated top-edge pull gesture reveals navigation
- Normal WebView scrolling does not reveal navigation
- Navigation visually blends with the page via best-effort background color matching
- Loading states are clearer through combined top progress and centered spinner feedback
- The app launcher icon is updated to use AionUI branding
